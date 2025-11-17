#!/bin/bash

# K3s Deployment Script for Gateway Service
set -e

echo "🚀 Deploying Gateway Service to K3s..."

VERSION="${1:-latest}"

# Configuration
NAMESPACE="gateway-system"
SERVICES_NAMESPACE="services"
GATEWAY_IMAGE="${GATEWAY_IMAGE:-your-registry/gateway:${VERSION}}"

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

    if [ ${#missing_tools[@]} -ne 0 ]; then
        log_error "Missing required tools: ${missing_tools[*]}"
        log_info "Please install kubectl first"
        exit 1
    fi

    # Check if we can access the cluster
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot access K3s cluster"
        log_info "Please ensure K3s is running and kubectl is configured"
        log_info "Run: export KUBECONFIG=~/.kube/config"
        exit 1
    fi

    # Check if it's K3s
    if kubectl get nodes -o jsonpath='{.items[0].metadata.name}' | grep -q "k3s"; then
        log_success "Detected K3s cluster"
    else
        log_warning "This script is optimized for K3s, but other Kubernetes distributions should also work"
    fi

    log_success "All prerequisites check passed"
}

check_image() {
    log_info "Checking if image exists..."

    if [ "$GATEWAY_IMAGE" = "your-registry/gateway:${VERSION}" ]; then
        log_warning "Using default image placeholder: $GATEWAY_IMAGE"
        log_info "Please set GATEWAY_IMAGE environment variable or modify the script"
        read -p "Continue anyway? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
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

update_deployment_image() {
    log_info "Updating deployment with image: $GATEWAY_IMAGE"

    # Create a temporary file with updated image
    TEMP_DEPLOY=$(mktemp)
    sed "s|image: .*|image: $GATEWAY_IMAGE|" k8s/gateway/gateway-deployment.yaml > "$TEMP_DEPLOY"
    
    kubectl apply -f "$TEMP_DEPLOY"
    rm "$TEMP_DEPLOY"

    log_success "Deployment updated with image: $GATEWAY_IMAGE"
}

deploy_gateway() {
    log_info "Deploying Gateway application..."

    if [ -f k8s/gateway/gateway-deployment.yaml ]; then
        update_deployment_image
    else
        log_error "Deployment file not found: k8s/gateway/gateway-deployment.yaml"
        exit 1
    fi
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

    # Wait a bit more for service to stabilize
    log_info "Waiting for service to stabilize..."
    sleep 10
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

    # Show current deployed image version
    log_info "🏷️  Current deployed version:"
    kubectl get deployment api-gateway -n $NAMESPACE -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null || echo "Not deployed yet"
    echo ""

    # Get ingress URL
    log_info "🌐 Gateway Access:"
    INGRESS_HOST=$(kubectl get ingress gateway-ingress -n $NAMESPACE -o jsonpath='{.spec.rules[0].host}' 2>/dev/null || echo "")
    
    if [ ! -z "$INGRESS_HOST" ] && [ "$INGRESS_HOST" != "null" ]; then
        log_success "Access via domain: http://$INGRESS_HOST"
    fi

    # Get Node IP
    NODE_IP=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}' 2>/dev/null || \
              kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="ExternalIP")].address}' 2>/dev/null || \
              echo "unknown")
    
    if [ "$NODE_IP" != "unknown" ]; then
        log_info "Access via Node IP: http://$NODE_IP"
        log_info "Health Check: http://$NODE_IP/actuator/health"
        
        # Test connectivity
        if curl -s -f "http://$NODE_IP/actuator/health" > /dev/null 2>&1; then
            log_success "Gateway is responding to health checks"
        else
            log_warning "Gateway not responding yet (may still be starting)"
        fi
    else
        log_warning "Could not determine node IP"
    fi

    # Check Traefik service
    log_info "📦 Traefik Ingress Controller:"
    if kubectl get svc -n kube-system traefik 2>/dev/null | grep -q traefik; then
        TRAEFIK_IP=$(kubectl get svc -n kube-system traefik -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "")
        TRAEFIK_PORT=$(kubectl get svc -n kube-system traefik -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "")
        if [ ! -z "$TRAEFIK_IP" ]; then
            log_info "Traefik LoadBalancer IP: $TRAEFIK_IP"
        elif [ ! -z "$TRAEFIK_PORT" ]; then
            log_info "Traefik NodePort: $TRAEFIK_PORT"
        else
            log_info "Traefik is running (default ports: 80/443)"
        fi
    else
        log_warning "Traefik service not found (K3s should have Traefik by default)"
    fi
}

show_troubleshooting_info() {
    echo ""
    log_warning "Troubleshooting information:"

    echo "=== Pod Status ==="
    kubectl get pods -n $NAMESPACE -l app=api-gateway

    echo ""
    echo "=== Pod Logs (last 50 lines) ==="
    kubectl logs -n $NAMESPACE -l app=api-gateway --tail=50 2>/dev/null || log_warning "Could not fetch logs"

    echo ""
    echo "=== Pod Describe ==="
    kubectl describe pods -n $NAMESPACE -l app=api-gateway | head -100

    echo ""
    echo "=== Service Endpoints ==="
    kubectl get endpoints -n $NAMESPACE
}

# Main execution
main() {
    show_version_info
    check_prerequisites
    check_image
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
        echo "   1. Deploy backend services with label: gateway.enabled=true"
        echo "   2. Test: curl http://<node-ip>/actuator/health"
        echo "   3. Monitor: kubectl logs -n $NAMESPACE -l app=api-gateway -f"
        echo "   4. Check resources: kubectl top pods -n $NAMESPACE"
        echo ""
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
    "update-image")
        if [ -z "$2" ]; then
            log_error "Please specify image name"
            log_info "Usage: $0 update-image <image-name>"
            exit 1
        fi
        GATEWAY_IMAGE="$2"
        update_deployment_image
        log_success "Image updated"
        ;;
    "discover")
        verify_service_discovery
        ;;
    "troubleshoot")
        show_troubleshooting_info
        ;;
    *)
        echo "Usage: $0 [command] [options]"
        echo ""
        echo "Commands:"
        echo "  deploy [version]     - Full deployment (default: latest)"
        echo "                        Example: $0 deploy 1.0.0"
        echo "  status              - Show deployment status"
        echo "  logs                - Follow application logs"
        echo "  delete              - Delete all resources"
        echo "  update-service      - Update service and ingress only"
        echo "  update-image <img>  - Update deployment image"
        echo "  discover            - Check service discovery configuration"
        echo "  troubleshoot        - Show troubleshooting information"
        echo ""
        echo "Environment variables:"
        echo "  GATEWAY_IMAGE       - Override gateway image"
        echo "                        Example: export GATEWAY_IMAGE=my-registry/gateway:1.0.0"
        echo ""
        echo "Examples:"
        echo "  $0 deploy                    # Deploy with default image"
        echo "  GATEWAY_IMAGE=my/gateway:1.0.0 $0 deploy  # Deploy with custom image"
        echo "  $0 update-image my/gateway:1.0.1  # Update to new image"
        exit 1
        ;;
esac

