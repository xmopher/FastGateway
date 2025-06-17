#!/bin/bash

# Complete Kubernetes Deployment Script for Gateway Service
set -e

echo "🚀 Deploying Gateway Service to Kubernetes..."

VERSION="${2:-1.0.0}"

# Configuration
NAMESPACE="gateway-system"
SERVICES_NAMESPACE="services"
ECR_REGISTRY="815278552654.dkr.ecr.us-west-2.amazonaws.com"
GATEWAY_IMAGE="$ECR_REGISTRY/java/gateway:$VERSION"
REGION="us-west-2"
DOMAIN_NAME="${DOMAIN_NAME:-}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Functions
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# 🆕 显示版本信息
show_version_info() {
    echo ""
    log_info "📦 Deployment Configuration:"
    echo "   Version: $VERSION"
    echo "   Image: $GATEWAY_IMAGE"
    echo "   Namespace: $NAMESPACE"
    echo ""
}

check_prerequisites() {
    log_info "Checking prerequisites..."

    local missing_tools=()

    if ! command -v kubectl &> /dev/null; then
        missing_tools+=("kubectl")
    fi

    if ! command -v docker &> /dev/null; then
        missing_tools+=("docker")
    fi

    if ! command -v aws &> /dev/null; then
        missing_tools+=("aws")
    fi

    if [ ${#missing_tools[@]} -ne 0 ]; then
        log_error "Missing required tools: ${missing_tools[*]}"
        exit 1
    fi

    # Check if we can access the cluster
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot access Kubernetes cluster"
        exit 1
    fi

    log_success "All prerequisites check passed"
}

setup_ssl_certificate() {
    log_info "Setting up SSL certificate..."

    if [ ! -z "$DOMAIN_NAME" ]; then
        log_info "Setting up ACM certificate for domain: $DOMAIN_NAME"

        # Check if certificate exists
        CERT_ARN=$(aws acm list-certificates --region $REGION \
            --query "CertificateSummaryList[?DomainName=='$DOMAIN_NAME'].CertificateArn" \
            --output text 2>/dev/null || echo "")

        if [ ! -z "$CERT_ARN" ]; then
            log_success "Using existing certificate: $CERT_ARN"

            # Update ingress with correct certificate ARN
            sed -i "s|certificate/.*|certificate/${CERT_ARN##*/}\"|" k8s/gateway/gateway-service.yaml
        else
            log_warning "No certificate found for domain $DOMAIN_NAME"
            log_info "Please create ACM certificate first or use the default certificate"
        fi
    else
        log_info "Using default/existing certificate configuration"
        log_warning "For production, set DOMAIN_NAME environment variable"
    fi
}

build_and_push_image() {
    log_info "Building and pushing Docker image..."

    # Check if JAR file exists
    if [ ! -f target/gateway-service-*.jar ]; then
        log_error "JAR file not found in target/ directory"
        log_info "Please build the project first with: mvn clean package -DskipTests"
        exit 1
    fi

    # Build the Docker image
    log_info "Building Docker image with version: $VERSION"
    docker build -t gateway-service:$VERSION .

    # Tag for ECR
    docker tag gateway-service:$VERSION $GATEWAY_IMAGE

    # Login to ECR
    log_info "Logging into ECR..."
    aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin $ECR_REGISTRY

    # Push to ECR
    log_info "Pushing image to ECR: $GATEWAY_IMAGE"
    docker push $GATEWAY_IMAGE

    log_success "Image built and pushed successfully: $GATEWAY_IMAGE"
}

create_namespaces() {
    log_info "Creating namespaces..."
    kubectl apply -f k8s/namespace.yaml
    log_success "Namespaces created"
}

deploy_configurations() {
    log_info "Deploying configurations..."

    # Apply RBAC first
    kubectl apply -f k8s/gateway/gateway-rbac.yaml

    # Apply ConfigMaps
    kubectl apply -f k8s/gateway/gateway-configmap.yaml
    kubectl apply -f k8s/gateway/gateway-ratelimit-config.yaml

    log_success "Configurations deployed"
}

deploy_gateway() {
    log_info "Deploying Gateway application with version: $VERSION"

    # Update the deployment to use the correct image
    sed "s|image: .*|image: $GATEWAY_IMAGE|" k8s/gateway/gateway-deployment.yaml | kubectl apply -f -

    log_success "Gateway deployment created with image: $GATEWAY_IMAGE"
}

deploy_service() {
    log_info "Deploying Gateway service and ingress..."
    kubectl apply -f k8s/gateway/gateway-service.yaml
    log_success "Service and Ingress deployed"
}

wait_for_deployment() {
    log_info "Waiting for deployment to be ready..."

    # Wait for deployment to be available
    if kubectl wait --for=condition=available deployment/api-gateway -n $NAMESPACE --timeout=300s; then
        log_success "Deployment is ready"
    else
        log_error "Deployment failed to become ready in time"
        show_troubleshooting_info
        return 1
    fi

    # Wait a bit more for ingress
    log_info "Waiting for ALB to be provisioned (this may take 3-5 minutes)..."
    sleep 30
}

verify_service_discovery() {
    log_info "Verifying service discovery configuration..."

    # Check if services namespace exists and has discoverable services
    if kubectl get namespace $SERVICES_NAMESPACE &> /dev/null; then
        log_success "Services namespace exists"

        # List services with gateway.enabled=true label
        DISCOVERABLE_SERVICES=$(kubectl get services -n $SERVICES_NAMESPACE -l gateway.enabled=true --no-headers 2>/dev/null | wc -l)

        if [ "$DISCOVERABLE_SERVICES" -gt 0 ]; then
            log_success "Found $DISCOVERABLE_SERVICES discoverable services"
            kubectl get services -n $SERVICES_NAMESPACE -l gateway.enabled=true
        else
            log_warning "No services found with label 'gateway.enabled=true' in namespace '$SERVICES_NAMESPACE'"
            log_info "To make services discoverable by the gateway, add label: gateway.enabled=true"
            log_info "Example: kubectl label service your-service gateway.enabled=true -n $SERVICES_NAMESPACE"
        fi
    else
        log_warning "Services namespace '$SERVICES_NAMESPACE' does not exist"
        log_info "Services will be discovered automatically when deployed with label: gateway.enabled=true"
    fi
}

show_status() {
    echo ""
    log_info "📊 Deployment Status:"
    kubectl get pods -n $NAMESPACE -l app=api-gateway
    echo ""
    log_info "🌐 Gateway Service:"
    kubectl get service api-gateway-service -n $NAMESPACE
    echo ""
    log_info "🔗 Gateway Ingress:"
    kubectl get ingress gateway-ingress -n $NAMESPACE
    echo ""

    # 🆕 显示当前部署的镜像版本
    log_info "🏷️  Current deployed version:"
    kubectl get deployment api-gateway -n $NAMESPACE -o jsonpath='{.spec.template.spec.containers[0].image}'
    echo ""

    # Get ALB URL
    log_info "⏳ Getting ALB URL..."
    ALB_URL=$(kubectl get ingress gateway-ingress -n $NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null)
    if [ ! -z "$ALB_URL" ]; then
        log_success "🌐 ALB URL: https://$ALB_URL"
        log_info "🔗 Health Check: https://$ALB_URL/actuator/health"
        log_info "🔗 Metrics: https://$ALB_URL/actuator/prometheus"

        # Test connectivity
        if curl -s -f "https://$ALB_URL/actuator/health" > /dev/null 2>&1; then
            log_success "Gateway is responding to health checks"
        else
            log_warning "Gateway not responding yet (may still be starting)"
        fi
    else
        log_warning "ALB is still being created. Check again in a few minutes with:"
        echo "kubectl get ingress gateway-ingress -n $NAMESPACE"
    fi
}

show_troubleshooting_info() {
    echo ""
    log_warning "Troubleshooting information:"

    echo "=== Pod Status ==="
    kubectl get pods -n $NAMESPACE -l app=api-gateway

    echo "=== Pod Logs (last 50 lines) ==="
    kubectl logs -n $NAMESPACE -l app=api-gateway --tail=50

    echo "=== Pod Describe ==="
    kubectl describe pods -n $NAMESPACE -l app=api-gateway

    echo "=== Service Endpoints ==="
    kubectl get endpoints -n $NAMESPACE
}

# 🆕 列出已部署的版本
list_versions() {
    log_info "📋 Available versions in ECR:"
    aws ecr describe-images --repository-name java/gateway --region $REGION \
        --query 'imageDetails[*].imageTags[0]' --output table 2>/dev/null || {
        log_warning "Could not retrieve ECR versions"
    }

    echo ""
    log_info "🚀 Currently deployed version:"
    if kubectl get deployment api-gateway -n $NAMESPACE >/dev/null 2>&1; then
        kubectl get deployment api-gateway -n $NAMESPACE -o jsonpath='{.spec.template.spec.containers[0].image}'
        echo ""
    else
        log_warning "No deployment found"
    fi
}

# 🆕 回滚到指定版本
rollback_version() {
    local target_version="$1"
    if [ -z "$target_version" ]; then
        log_error "Please specify version to rollback to"
        log_info "Usage: $0 rollback <version>"
        log_info "Example: $0 rollback 1.0.1"
        exit 1
    fi

    local rollback_image="$ECR_REGISTRY/java/gateway:$target_version"
    log_info "🔄 Rolling back to version: $target_version"
    log_info "Image: $rollback_image"

    # Update deployment with rollback image
    kubectl patch deployment api-gateway -n $NAMESPACE -p "{\"spec\":{\"template\":{\"spec\":{\"containers\":[{\"name\":\"api-gateway\",\"image\":\"$rollback_image\"}]}}}}"

    log_success "Rollback initiated to version: $target_version"
    log_info "Monitoring rollback progress..."
    kubectl rollout status deployment/api-gateway -n $NAMESPACE
}

# Main execution
main() {
    show_version_info
    check_prerequisites
    setup_ssl_certificate
    build_and_push_image
    create_namespaces
    deploy_configurations
    deploy_gateway
    deploy_service

    if wait_for_deployment; then
        verify_service_discovery
        show_status

        echo ""
        log_success "🎉 Gateway Service deployed successfully!"
        log_success "📦 Version: $VERSION"
        log_info "📝 Next Steps:"
        echo "   1. Wait 3-5 minutes for ALB to be fully ready"
        echo "   2. Deploy backend services with label: gateway.enabled=true"
        echo "   3. Test: curl -k https://\$ALB_URL/actuator/health"
        echo "   4. Monitor: kubectl logs -n $NAMESPACE -l app=api-gateway -f"
        echo ""
        log_info "💡 Version Management:"
        echo "   - Deploy new version: $0 deploy <version>"
        echo "   - List versions: $0 versions"
        echo "   - Rollback: $0 rollback <version>"
    else
        log_error "Deployment failed"
        exit 1
    fi
}

# Handle script arguments
case "${1:-deploy}" in
    "deploy")
        main
        ;;
    "status")
        show_status
        ;;
    "logs")
        kubectl logs -n $NAMESPACE -l app=api-gateway -f
        ;;
    "delete")
        log_warning "Deleting Gateway deployment..."
        kubectl delete -f k8s/gateway/ 2>/dev/null || true
        kubectl delete namespace $NAMESPACE 2>/dev/null || true
        log_success "Gateway deleted"
        ;;
    "update-service")
        log_info "Updating service and ingress only..."
        kubectl apply -f k8s/gateway/gateway-service.yaml
        log_success "Service updated"
        ;;
    "get-url")
        ALB_URL=$(kubectl get ingress gateway-ingress -n $NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null)
        if [ ! -z "$ALB_URL" ]; then
            echo "ALB URL: https://$ALB_URL"
            echo "Health Check: https://$ALB_URL/actuator/health"
        else
            echo "ALB not ready yet. Please wait a few more minutes."
        fi
        ;;
    "test")
        ALB_URL=$(kubectl get ingress gateway-ingress -n $NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null)
        if [ ! -z "$ALB_URL" ]; then
            log_info "Testing ALB connectivity..."
            echo "Testing HTTPS health check:"
            curl -v -k "https://$ALB_URL/actuator/health" || echo "Health check failed"
            echo ""
            echo "Testing HTTP redirect:"
            curl -v "http://$ALB_URL" || echo "HTTP redirect test failed"
        else
            log_error "ALB URL not available yet"
        fi
        ;;
    "discover")
        verify_service_discovery
        ;;
    "versions")
        list_versions
        ;;
    "rollback")
        rollback_version "$2"
        ;;
    *)
        echo "Usage: $0 [command] [version]"
        echo ""
        echo "Commands:"
        echo "  deploy [version]   - Full deployment (build, push, deploy)"
        echo "                      Default version: 1.0.0"
        echo "                      Example: $0 deploy 1.0.1"
        echo "  status            - Show deployment status"
        echo "  logs              - Follow application logs"
        echo "  delete            - Delete all resources"
        echo "  update-service    - Update service and ingress only"
        echo "  get-url           - Get ALB URL"
        echo "  test              - Test ALB connectivity"
        echo "  discover          - Check service discovery configuration"
        echo "  versions          - List available versions"
        echo "  rollback <version>- Rollback to specific version"
        echo ""
        echo "Examples:"
        echo "  $0 deploy 1.0.1           # Deploy version 1.0.1"
        echo "  $0 deploy 1.0.2           # Deploy version 1.0.2"
        echo "  $0 rollback 1.0.1         # Rollback to version 1.0.1"
        echo "  $0 versions               # List all versions"
        echo ""
        echo "Environment variables:"
        echo "  DOMAIN_NAME       - Your domain for ACM certificate (optional)"
        echo "  GATEWAY_VERSION   - Override version (optional)"
        exit 1
        ;;
esac