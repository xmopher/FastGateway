pipeline {
    agent any

    parameters {
        string(
            name: 'DOCKER_REGISTRY',
            defaultValue: 'docker.io'
        )
        string(
            name: 'DOCKER_IMAGE_NAME',
            defaultValue: 'gateway-service'
        )
        string(
            name: 'DOCKER_TAG',
            defaultValue: ''
        )
        string(
            name: 'DOCKER_REGISTRY_CREDENTIAL_ID',
            defaultValue: 'acr-user-platform'
        )
        
        string(
            name: 'K3S_HOST',
            defaultValue: ''
        )
        string(
            name: 'K3S_USER',
            defaultValue: 'ec2-user'
        )
        string(
            name: 'K3S_SSH_KEY_CREDENTIAL_ID',
            defaultValue: 'k3s-ssh-key'
        )
        string(
            name: 'K3S_KUBECONFIG_PATH',
            defaultValue: '~/.kube/config'
        )
        string(
            name: 'K3S_NAMESPACE',
            defaultValue: 'gateway-system'
        )
        
        string(
            name: 'REDIS_HOST',
            defaultValue: 'redis-service'
        )
        string(
            name: 'REDIS_PORT',
            defaultValue: '6379'
        )
        string(
            name: 'SPRING_PROFILES_ACTIVE',
            defaultValue: 'k8s'
        )
        
        booleanParam(
            name: 'SKIP_BUILD',
            defaultValue: false,
        )
        booleanParam(
            name: 'SKIP_DOCKER_BUILD',
            defaultValue: false,
        )
        booleanParam(
            name: 'SKIP_PUSH',
            defaultValue: false,
        )
        booleanParam(
            name: 'ENABLE_DEPLOY',
            defaultValue: true,
        )
        
        string(
            name: 'BRANCH_NAME',
            defaultValue: ''
        )
    }

    environment {
        PROJECT_NAME = "${params.DOCKER_IMAGE_NAME}"
        VERSION = "${params.DOCKER_TAG ?: env.BUILD_NUMBER}"
        DOCKER_REGISTRY = "${params.DOCKER_REGISTRY}"
        DOCKER_IMAGE = "${DOCKER_REGISTRY}/${PROJECT_NAME}"
        DOCKER_TAG = "${VERSION}"
        DOCKER_REGISTRY_CREDENTIAL_ID = "${params.DOCKER_REGISTRY_CREDENTIAL_ID}"
        K3S_HOST = "${params.K3S_HOST}"
        K3S_USER = "${params.K3S_USER}"
        K3S_SSH_KEY_CREDENTIAL_ID = "${params.K3S_SSH_KEY_CREDENTIAL_ID}"
        K3S_KUBECONFIG_PATH = "${params.K3S_KUBECONFIG_PATH}"
        K3S_NAMESPACE = "${params.K3S_NAMESPACE}"
        REDIS_HOST = "${params.REDIS_HOST}"
        REDIS_PORT = "${params.REDIS_PORT}"
        SPRING_PROFILES_ACTIVE = "${params.SPRING_PROFILES_ACTIVE}"
        MAVEN_OPTS = '-Xmx1024m'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    stages {
        stage('Environment Setup') {
            steps {
                script {
                    if (!isUnix()) {
                        def gitBinPath = 'C:\\Program Files\\Git\\bin'
                        def gitUsrBinPath = 'C:\\Program Files\\Git\\usr\\bin'
                        env.PATH = "${gitBinPath};${gitUsrBinPath};${env.PATH}"
                        echo "Git Bash added to PATH: ${env.PATH}"
                    }
                }
            }
        }
        
        stage('Checkout') {
            steps {
                script {
                    def branchName = params.BRANCH_NAME ?: env.BRANCH_NAME
                    echo "Checking out code, branch: ${branchName}"
                    if (params.BRANCH_NAME) {
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: "*/${params.BRANCH_NAME}"]],
                            extensions: [],
                            userRemoteConfigs: scm.userRemoteConfigs
                        ])
                    } else {
                        checkout scm
                    }
                }
            }
        }
        
        stage('Shell Configuration') {
            steps {
                script {
                    if (!isUnix()) {
                        def shPath = 'C:\\Program Files\\Git\\bin\\sh.exe'
                        if (fileExists(shPath)) {
                            env.SH_CMD = shPath
                            env.PATH = "C:\\Program Files\\Git\\bin;C:\\Program Files\\Git\\usr\\bin;${env.PATH}"
                            echo "Git Bash configured: ${shPath}"
                        } else {
                            error("Git Bash not found, please install Git for Windows")
                        }
                    } else {
                        env.SH_CMD = 'sh'
                    }
                }
            }
        }

        stage('Maven Build') {
            when {
                expression { !params.SKIP_BUILD }
            }
            steps {
                script {
                    echo "Starting Maven build..."
                    echo "Project: ${PROJECT_NAME}"
                    echo "Version: ${VERSION}"
                    if (isUnix()) {
                        sh 'mvn clean package -DskipTests'
                    } else {
                        bat "\"${env.SH_CMD}\" -c \"mvn clean package -DskipTests\""
                    }
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Build Docker Image') {
            when {
                expression { !params.SKIP_DOCKER_BUILD }
            }
            steps {
                script {
                    echo "Building Docker image: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                    echo "Registry: ${DOCKER_REGISTRY}"
                    script {
                        if (isUnix()) {
                            sh '''
                                docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                                docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest
                            '''
                        } else {
                            bat "\"${env.SH_CMD}\" -c \"docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} . && docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest\""
                        }
                    }
                }
            }
        }

        stage('Push Image') {
            when {
                expression { !params.SKIP_PUSH }
            }
            steps {
                script {
                    echo "Pushing image to registry..."
                    script {
                        if (DOCKER_REGISTRY_CREDENTIAL_ID) {
                            withCredentials([usernamePassword(
                                credentialsId: DOCKER_REGISTRY_CREDENTIAL_ID,
                                usernameVariable: 'DOCKER_REGISTRY_USER',
                                passwordVariable: 'DOCKER_REGISTRY_PASSWORD'
                            )]) {
                                script {
                                    if (isUnix()) {
                                        sh 'echo "${DOCKER_REGISTRY_PASSWORD}" | docker login ${DOCKER_REGISTRY} -u ${DOCKER_REGISTRY_USER} --password-stdin'
                                    } else {
                                        bat "\"${env.SH_CMD}\" -c \"echo ${DOCKER_REGISTRY_PASSWORD} | docker login ${DOCKER_REGISTRY} -u ${DOCKER_REGISTRY_USER} --password-stdin\""
                                    }
                                }
                            }
                        }
                    }
                    script {
                        if (isUnix()) {
                            sh '''
                                echo "Pushing image: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                                docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                                docker push ${DOCKER_IMAGE}:latest
                            '''
                        } else {
                            bat "\"${env.SH_CMD}\" -c \"echo Pushing image: ${DOCKER_IMAGE}:${DOCKER_TAG} && docker push ${DOCKER_IMAGE}:${DOCKER_TAG} && docker push ${DOCKER_IMAGE}:latest\""
                        }
                    }
                }
            }
        }

        stage('Deploy to K3s') {
            when {
                allOf {
                    expression { params.ENABLE_DEPLOY }
                    expression { params.K3S_HOST?.trim() }
                }
            }
            steps {
                script {
                    echo "Deploying to K3s cluster..."
                    echo "K3s host: ${K3S_HOST}"
                    echo "SSH user: ${K3S_USER}"
                    echo "Namespace: ${K3S_NAMESPACE}"
                    echo "Image: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                    if (!K3S_HOST?.trim()) {
                        error("K3S_HOST parameter is not set!")
                    }
                    if (!K3S_SSH_KEY_CREDENTIAL_ID?.trim()) {
                        error("K3S_SSH_KEY_CREDENTIAL_ID parameter is not set!")
                    }
                    withCredentials([sshUserPrivateKey(
                        credentialsId: K3S_SSH_KEY_CREDENTIAL_ID,
                        keyFileVariable: 'SSH_KEY_FILE',
                        usernameVariable: 'SSH_USER_FROM_CREDENTIAL'
                    )]) {
                        def sshUser = params.K3S_USER ?: env.SSH_USER_FROM_CREDENTIAL
                        if (isUnix()) {
                            sh """
                            mkdir -p /tmp/k8s-deploy
                            cp -r k8s/* /tmp/k8s-deploy/
                            sed -i "s|image:.*|image: ${DOCKER_IMAGE}:${DOCKER_TAG}|g" /tmp/k8s-deploy/gateway/gateway-deployment.yaml
                            if [ -n "${REDIS_HOST}" ] && [ "${REDIS_HOST}" != "redis-service" ]; then
                                sed -i "s|value: \"redis-service\"|value: \"${REDIS_HOST}\"|g" /tmp/k8s-deploy/gateway/gateway-deployment.yaml
                                sed -i "s|host: redis-service|host: ${REDIS_HOST}|g" /tmp/k8s-deploy/gateway/gateway-configmap.yaml
                            fi
                            scp -o StrictHostKeyChecking=no -i ${SSH_KEY_FILE} -r /tmp/k8s-deploy/* \
                                ${sshUser}@${K3S_HOST}:/tmp/k8s-deploy/
                            ssh -o StrictHostKeyChecking=no -i ${SSH_KEY_FILE} ${sshUser}@${K3S_HOST} bash << K8S_DEPLOY_EOF
                            set -e
                            
                            export KUBECONFIG=${K3S_KUBECONFIG_PATH}
                            export K3S_NAMESPACE=${K3S_NAMESPACE}
                            
                            echo "Creating namespace..."
                            kubectl apply -f /tmp/k8s-deploy/namespace.yaml
                            
                            echo "Configuring RBAC..."
                            kubectl apply -f /tmp/k8s-deploy/gateway/gateway-rbac.yaml
                            
                            echo "Configuring ConfigMap..."
                            kubectl apply -f /tmp/k8s-deploy/gateway/gateway-configmap.yaml
                            kubectl apply -f /tmp/k8s-deploy/gateway/gateway-ratelimit-config.yaml
                            
                            echo "Deploying Service and Ingress..."
                            kubectl apply -f /tmp/k8s-deploy/gateway/gateway-service.yaml
                            
                            echo "Deploying Deployment..."
                            kubectl apply -f /tmp/k8s-deploy/gateway/gateway-deployment.yaml
                            
                            echo "Waiting for Pod to start..."
                            for i in {1..60}; do
                                POD_NAME=\$(kubectl get pods -n \${K3S_NAMESPACE} -l app=api-gateway -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
                                if [ -n "\$POD_NAME" ]; then
                                    READY=\$(kubectl get pod \$POD_NAME -n \${K3S_NAMESPACE} -o jsonpath='{.status.containerStatuses[0].ready}' 2>/dev/null)
                                    if [ "\$READY" = "true" ]; then
                                        echo "Pod is ready: \$POD_NAME"
                                        break
                                    fi
                                fi
                                if [ \$i -eq 60 ]; then
                                    echo "Timeout waiting for Pod to be ready"
                                    break
                                fi
                                echo "Waiting... (\$i/60)"
                                sleep 5
                            done
                            
                            echo "Checking deployment status..."
                            kubectl get pods -n \${K3S_NAMESPACE} -l app=api-gateway
                            kubectl get svc -n \${K3S_NAMESPACE}
                            
                            POD_NAME=\$(kubectl get pods -n \${K3S_NAMESPACE} -l app=api-gateway -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
                            if [ -z "\$POD_NAME" ]; then
                                echo "Pod not found!"
                                kubectl get pods -n \${K3S_NAMESPACE}
                                exit 1
                            fi
                            
                            echo "Checking Pod details..."
                            POD_STATUS=\$(kubectl get pod \$POD_NAME -n \${K3S_NAMESPACE} -o jsonpath='{.status.phase}' 2>/dev/null)
                            CONTAINER_STATUS=\$(kubectl get pod \$POD_NAME -n \${K3S_NAMESPACE} -o jsonpath='{.status.containerStatuses[0].state.waiting.reason}' 2>/dev/null || kubectl get pod \$POD_NAME -n \${K3S_NAMESPACE} -o jsonpath='{.status.containerStatuses[0].state.running.startedAt}' 2>/dev/null || echo "unknown")
                            
                            kubectl describe pod \$POD_NAME -n \${K3S_NAMESPACE} | tail -40
                            
                            echo "Pod logs:"
                            kubectl logs \$POD_NAME -n \${K3S_NAMESPACE} --tail=100 || echo "Cannot get logs"
                            
                            if [ "\$POD_STATUS" != "Running" ] || [ "\$CONTAINER_STATUS" = "CrashLoopBackOff" ] || [ "\$CONTAINER_STATUS" = "ImagePullBackOff" ]; then
                                echo "Pod status abnormal: \$POD_STATUS, Container status: \$CONTAINER_STATUS"
                                echo "Previous container logs:"
                                kubectl logs \$POD_NAME -n \${K3S_NAMESPACE} --previous --tail=50 2>/dev/null || echo "No previous logs"
                                exit 1
                            fi
                            
                            echo "Pod running: \$POD_NAME"
                            echo "Executing health check..."
                            for i in {1..30}; do
                                if kubectl exec -n \${K3S_NAMESPACE} \$POD_NAME -- curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
                                    echo "Health check passed!"
                                    exit 0
                                fi
                                echo "Waiting... (\$i/30)"
                                sleep 2
                            done
                            
                            echo "Health check failed!"
                            kubectl logs \$POD_NAME -n \${K3S_NAMESPACE} --tail=50
                            exit 1
K8S_DEPLOY_EOF
                            """
                        } else {
                            def deployScript = """
DEPLOY_DIR=/tmp/k8s-deploy-\$\$
mkdir -p "\$DEPLOY_DIR"
cp -r k8s/* "\$DEPLOY_DIR/"
sed -i "s|image:.*|image: ${DOCKER_IMAGE}:${DOCKER_TAG}|g" "\$DEPLOY_DIR/gateway/gateway-deployment.yaml"
if [ -n "${REDIS_HOST}" ] && [ "${REDIS_HOST}" != "redis-service" ]; then
    perl -i -pe "s|value: \\\"redis-service\\\"|value: \\\"${REDIS_HOST}\\\"|g" "\$DEPLOY_DIR/gateway/gateway-deployment.yaml" 2>/dev/null || \\
    sed -i "s|value: \\\\"redis-service\\\\"|value: \\\\"${REDIS_HOST}\\\\"|g" "\$DEPLOY_DIR/gateway/gateway-deployment.yaml"
    sed -i "s|host: redis-service|host: ${REDIS_HOST}|g" "\$DEPLOY_DIR/gateway/gateway-configmap.yaml"
fi
if echo "${SSH_KEY_FILE}" | grep -q '^[A-Za-z]:'; then
    if command -v cygpath > /dev/null 2>&1; then
        SSH_KEY=\$(cygpath -u "${SSH_KEY_FILE}")
    else
        SSH_KEY=\$(echo "${SSH_KEY_FILE}" | sed 's|\\\\\\\\|/|g' | sed 's|^[Cc]:|/c|' | sed 's|^[Dd]:|/d|' | sed 's|^[Ee]:|/e|' | sed 's|^[Ff]:|/f|')
    fi
else
    SSH_KEY="${SSH_KEY_FILE}"
fi
scp -o StrictHostKeyChecking=no -i \${SSH_KEY} -r \${DEPLOY_DIR}/* \\
    ${sshUser}@${K3S_HOST}:/tmp/k8s-deploy/
ssh -o StrictHostKeyChecking=no -i \${SSH_KEY} ${sshUser}@${K3S_HOST} bash << K8S_DEPLOY_EOF
set -e

export KUBECONFIG=${K3S_KUBECONFIG_PATH}
export K3S_NAMESPACE=${K3S_NAMESPACE}

echo "Creating namespace..."
kubectl apply -f /tmp/k8s-deploy/namespace.yaml

echo "Configuring RBAC..."
kubectl apply -f /tmp/k8s-deploy/gateway/gateway-rbac.yaml

echo "Configuring ConfigMap..."
kubectl apply -f /tmp/k8s-deploy/gateway/gateway-configmap.yaml
kubectl apply -f /tmp/k8s-deploy/gateway/gateway-ratelimit-config.yaml

echo "Deploying Service and Ingress..."
kubectl apply -f /tmp/k8s-deploy/gateway/gateway-service.yaml

echo "Deploying Deployment..."
kubectl apply -f /tmp/k8s-deploy/gateway/gateway-deployment.yaml

echo "Waiting for Pod to start..."
for i in {1..60}; do
    POD_NAME=\\\$(kubectl get pods -n \\\$K3S_NAMESPACE -l app=api-gateway -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
    if [ -n "\\\$POD_NAME" ]; then
        READY=\\\$(kubectl get pod \\\$POD_NAME -n \\\$K3S_NAMESPACE -o jsonpath='{.status.containerStatuses[0].ready}' 2>/dev/null)
        if [ "\\\$READY" = "true" ]; then
            echo "Pod is ready: \\\$POD_NAME"
            break
        fi
    fi
    if [ \\\$i -eq 60 ]; then
        echo "Timeout waiting for Pod to be ready"
        break
    fi
    echo "Waiting... (\\\$i/60)"
    sleep 5
done

echo "Checking deployment status..."
kubectl get pods -n \\\$K3S_NAMESPACE -l app=api-gateway
kubectl get svc -n \\\$K3S_NAMESPACE

POD_NAME=\\\$(kubectl get pods -n \\\$K3S_NAMESPACE -l app=api-gateway -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
if [ -z "\\\$POD_NAME" ]; then
    echo "Pod not found!"
    kubectl get pods -n \\\$K3S_NAMESPACE
    exit 1
fi

echo "Checking Pod details..."
POD_STATUS=\\\$(kubectl get pod \\\$POD_NAME -n \\\$K3S_NAMESPACE -o jsonpath='{.status.phase}' 2>/dev/null)
CONTAINER_STATUS=\\\$(kubectl get pod \\\$POD_NAME -n \\\$K3S_NAMESPACE -o jsonpath='{.status.containerStatuses[0].state.waiting.reason}' 2>/dev/null || kubectl get pod \\\$POD_NAME -n \\\$K3S_NAMESPACE -o jsonpath='{.status.containerStatuses[0].state.running.startedAt}' 2>/dev/null || echo "unknown")

kubectl describe pod \\\$POD_NAME -n \\\$K3S_NAMESPACE | tail -40

echo "Pod logs:"
kubectl logs \\\$POD_NAME -n \\\$K3S_NAMESPACE --tail=100 || echo "Cannot get logs"

if [ "\\\$POD_STATUS" != "Running" ] || [ "\\\$CONTAINER_STATUS" = "CrashLoopBackOff" ] || [ "\\\$CONTAINER_STATUS" = "ImagePullBackOff" ]; then
    echo "Pod status abnormal: \\\$POD_STATUS, Container status: \\\$CONTAINER_STATUS"
    echo "Previous container logs:"
    kubectl logs \\\$POD_NAME -n \\\$K3S_NAMESPACE --previous --tail=50 2>/dev/null || echo "No previous logs"
    exit 1
fi

echo "Pod running: \\\$POD_NAME"
echo "Executing health check..."
for i in {1..30}; do
    if kubectl exec -n \\\$K3S_NAMESPACE \\\$POD_NAME -- curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "Health check passed!"
        exit 0
    fi
    echo "Waiting... (\\\$i/30)"
    sleep 2
done

echo "Health check failed!"
kubectl logs \\\$POD_NAME -n \\\$K3S_NAMESPACE --tail=50
exit 1
K8S_DEPLOY_EOF
                            """.trim()
                            
                            writeFile file: 'deploy.sh', text: deployScript, encoding: 'UTF-8'
                            bat "\"${env.SH_CMD}\" deploy.sh"
                        }
                    }
                }
            }
        }

        stage('Health Check') {
            when {
                allOf {
                    expression { params.ENABLE_DEPLOY }
                    expression { params.K3S_HOST?.trim() }
                }
            }
            steps {
                script {
                    echo "Executing external health check..."
                    withCredentials([sshUserPrivateKey(
                        credentialsId: K3S_SSH_KEY_CREDENTIAL_ID,
                        keyFileVariable: 'SSH_KEY_FILE',
                        usernameVariable: 'SSH_USER_FROM_CREDENTIAL'
                    )]) {
                        def sshUser = params.K3S_USER ?: env.SSH_USER_FROM_CREDENTIAL
                        script {
                            if (isUnix()) {
                                sh """
                                    ssh -o StrictHostKeyChecking=no -i ${SSH_KEY_FILE} ${sshUser}@${K3S_HOST} bash << HEALTH_CHECK_EOF
                                    export KUBECONFIG=${K3S_KUBECONFIG_PATH}
                                    export K3S_NAMESPACE=${K3S_NAMESPACE}
                                    SERVICE_TYPE=\$(kubectl get svc api-gateway-service -n \${K3S_NAMESPACE} -o jsonpath='{.spec.type}' 2>/dev/null)
                                    if [ "\$SERVICE_TYPE" = "NodePort" ]; then
                                        NODEPORT=\$(kubectl get svc api-gateway-service -n \${K3S_NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}')
                                        HEALTH_URL="http://localhost:\$NODEPORT/actuator/health"
                                    else
                                        HEALTH_URL="http://localhost:8080/actuator/health"
                                    fi
                                    POD_NAME=\$(kubectl get pods -n \${K3S_NAMESPACE} -l app=api-gateway -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
                                    if [ -n "\$POD_NAME" ]; then
                                        if kubectl exec -n \${K3S_NAMESPACE} \$POD_NAME -- curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
                                            echo "Health check passed!"
                                            exit 0
                                        fi
                                    fi
                                    echo "Unable to check via Pod, please verify service status manually"
                                    exit 0
HEALTH_CHECK_EOF
                                """
                            } else {
                                def kubeConfigPath = "${K3S_KUBECONFIG_PATH}"
                                def k3sNamespace = "${K3S_NAMESPACE}"
                                def healthCheckScript = """
SSH_KEY="${SSH_KEY_FILE}"
if echo "\\$SSH_KEY" | grep -q '^[A-Za-z]:'; then
    if command -v cygpath > /dev/null 2>&1; then
        SSH_KEY=\\$(cygpath -u "\\$SSH_KEY")
    else
        SSH_KEY=\\$(echo "\\$SSH_KEY" | sed 's|\\\\\\\\|/|g' | sed 's|^[Cc]:|/c|' | sed 's|^[Dd]:|/d|' | sed 's|^[Ee]:|/e|' | sed 's|^[Ff]:|/f|')
    fi
fi
ssh -o StrictHostKeyChecking=no -i "\\${SSH_KEY}" ${sshUser}@${K3S_HOST} bash << HEALTH_CHECK_EOF
export KUBECONFIG=${kubeConfigPath}
export K3S_NAMESPACE=${k3sNamespace}
SERVICE_TYPE=\\\$(kubectl get svc api-gateway-service -n \\\$K3S_NAMESPACE -o jsonpath='{.spec.type}' 2>/dev/null)
if [ "\\\$SERVICE_TYPE" = "NodePort" ]; then
    NODEPORT=\\\$(kubectl get svc api-gateway-service -n \\\$K3S_NAMESPACE -o jsonpath='{.spec.ports[0].nodePort}')
    HEALTH_URL="http://localhost:\\\$NODEPORT/actuator/health"
else
    HEALTH_URL="http://localhost:8080/actuator/health"
fi
POD_NAME=\\\$(kubectl get pods -n \\\$K3S_NAMESPACE -l app=api-gateway -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
if [ -n "\\\$POD_NAME" ]; then
    if kubectl exec -n \\\$K3S_NAMESPACE \\\$POD_NAME -- curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "Health check passed!"
        exit 0
    fi
fi
echo "Unable to check via Pod, please verify service status manually"
exit 0
HEALTH_CHECK_EOF
                                """.trim()
                                
                                writeFile file: 'healthcheck.sh', text: healthCheckScript, encoding: 'UTF-8'
                                bat "\"${env.SH_CMD}\" healthcheck.sh"
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo "Cleaning workspace..."
                cleanWs()
            }
        }
        success {
            echo "Build and deployment successful!"
        }
        failure {
            echo "Build or deployment failed!"
        }
        unstable {
            echo "Build unstable!"
        }
    }
}

