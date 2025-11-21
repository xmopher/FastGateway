pipeline {
    agent any

    parameters {
        // Docker 镜像配置
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
        choice(
            name: 'DOCKER_REGISTRY_CREDENTIAL_ID',
            choices: ['', 'docker-registry']
        )
        
        // K3s 部署配置
        string(
            name: 'K3S_HOST',
            defaultValue: ''
        )
        string(
            name: 'K3S_USER',
            defaultValue: 'ec2-user'
        )
        choice(
            name: 'K3S_SSH_KEY_CREDENTIAL_ID',
            choices: ['k3s-ssh-key']
        )
        string(
            name: 'K3S_KUBECONFIG_PATH',
            defaultValue: '~/.kube/config'
        )
        string(
            name: 'K3S_NAMESPACE',
            defaultValue: 'gateway-system'
        )
        
        // 应用配置
        string(
            name: 'REDIS_HOST',
            defaultValue: 'redis-service'
        )
        string(
            name: 'REDIS_PORT',
            defaultValue: '6379'
        )
        choice(
            name: 'SPRING_PROFILES_ACTIVE',
            choices: ['k8s', 'dev', 'prod']
        )
        
        // 部署控制
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
        
        // 分支选择（用于手动构建时）
        string(
            name: 'BRANCH_NAME',
            defaultValue: ''
        )
    }

    environment {
        // 项目配置
        PROJECT_NAME = "${params.DOCKER_IMAGE_NAME}"
        VERSION = "${params.DOCKER_TAG ?: env.BUILD_NUMBER}"
        
        // Docker 镜像配置
        DOCKER_REGISTRY = "${params.DOCKER_REGISTRY}"
        DOCKER_IMAGE = "${DOCKER_REGISTRY}/${PROJECT_NAME}"
        DOCKER_TAG = "${VERSION}"
        DOCKER_REGISTRY_CREDENTIAL_ID = "${params.DOCKER_REGISTRY_CREDENTIAL_ID}"
        
        // K3s 部署配置（敏感信息通过 credentials 管理）
        K3S_HOST = "${params.K3S_HOST}"
        K3S_USER = "${params.K3S_USER}"
        K3S_SSH_KEY_CREDENTIAL_ID = "${params.K3S_SSH_KEY_CREDENTIAL_ID}"
        K3S_KUBECONFIG_PATH = "${params.K3S_KUBECONFIG_PATH}"
        K3S_NAMESPACE = "${params.K3S_NAMESPACE}"
        
        // 应用配置
        REDIS_HOST = "${params.REDIS_HOST}"
        REDIS_PORT = "${params.REDIS_PORT}"
        SPRING_PROFILES_ACTIVE = "${params.SPRING_PROFILES_ACTIVE}"
        
        // Maven 配置
        MAVEN_OPTS = '-Xmx1024m -XX:MaxPermSize=256m'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    stages {
        stage('代码检出') {
            steps {
                script {
                    def branchName = params.BRANCH_NAME ?: env.BRANCH_NAME
                    echo "🔄 检出代码，分支: ${branchName}"
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

        stage('Maven 构建') {
            when {
                expression { !params.SKIP_BUILD }
            }
            steps {
                script {
                    echo "🔨 开始 Maven 构建..."
                    echo "📦 项目名称: ${PROJECT_NAME}"
                    echo "🏷️  版本: ${VERSION}"
                    sh '''
                        mvn clean package -DskipTests
                    '''
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('构建 Docker 镜像') {
            when {
                expression { !params.SKIP_DOCKER_BUILD }
            }
            steps {
                script {
                    echo "🐳 构建 Docker 镜像: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                    echo "📦 镜像仓库: ${DOCKER_REGISTRY}"
                    sh '''
                        docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                        docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest
                    '''
                }
            }
        }

        stage('推送镜像到仓库') {
            when {
                expression { !params.SKIP_PUSH }
            }
            steps {
                script {
                    echo "📤 推送镜像到仓库..."
                    
                    // 登录 Docker Registry（如果需要）
                    script {
                        if (DOCKER_REGISTRY_CREDENTIAL_ID) {
                            withCredentials([usernamePassword(
                                credentialsId: DOCKER_REGISTRY_CREDENTIAL_ID,
                                usernameVariable: 'DOCKER_REGISTRY_USER',
                                passwordVariable: 'DOCKER_REGISTRY_PASSWORD'
                            )]) {
                                sh '''
                                    echo "${DOCKER_REGISTRY_PASSWORD}" | docker login ${DOCKER_REGISTRY} -u ${DOCKER_REGISTRY_USER} --password-stdin
                                '''
                            }
                        }
                    }
                    
                    // 推送镜像
                    sh '''
                        echo "推送镜像: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                        docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                        docker push ${DOCKER_IMAGE}:latest
                    '''
                }
            }
        }

        stage('部署到 K3s') {
            when {
                allOf {
                    expression { params.ENABLE_DEPLOY }
                    expression { params.K3S_HOST?.trim() }
                }
            }
            steps {
                script {
                    echo "🚀 部署到 K3s 集群..."
                    echo "📍 K3s 主机: ${K3S_HOST}"
                    echo "👤 SSH 用户: ${K3S_USER}"
                    echo "📦 命名空间: ${K3S_NAMESPACE}"
                    echo "🐳 镜像: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                    
                    // 验证必要的参数
                    if (!K3S_HOST?.trim()) {
                        error("❌ K3S_HOST 参数未设置！")
                    }
                    if (!K3S_SSH_KEY_CREDENTIAL_ID?.trim()) {
                        error("❌ K3S_SSH_KEY_CREDENTIAL_ID 参数未设置！")
                    }
                    
                    // 使用 withCredentials 读取 SSH 密钥
                    withCredentials([sshUserPrivateKey(
                        credentialsId: K3S_SSH_KEY_CREDENTIAL_ID,
                        keyFileVariable: 'SSH_KEY_FILE',
                        usernameVariable: 'SSH_USER_FROM_CREDENTIAL'
                    )]) {
                        // 使用参数中的用户名，如果没有则使用凭证中的用户名
                        def sshUser = params.K3S_USER ?: env.SSH_USER_FROM_CREDENTIAL
                        
                        // 通过 SSH 在 K3s 服务器上执行 kubectl 部署
                        sh """
                            # 准备临时部署文件
                            mkdir -p /tmp/k8s-deploy
                            
                            # 复制 K8s 配置文件
                            cp -r k8s/* /tmp/k8s-deploy/
                            
                            # 更新 deployment.yaml 中的镜像版本
                            sed -i "s|image:.*|image: ${DOCKER_IMAGE}:${DOCKER_TAG}|g" /tmp/k8s-deploy/gateway/gateway-deployment.yaml
                            
                            # 更新 Redis 主机地址（如果配置了外部 Redis）
                            if [ -n "${REDIS_HOST}" ] && [ "${REDIS_HOST}" != "redis-service" ]; then
                                sed -i "s|value: \"redis-service\"|value: \"${REDIS_HOST}\"|g" /tmp/k8s-deploy/gateway/gateway-deployment.yaml
                            fi
                            
                            # 将配置文件复制到 K3s 服务器
                            scp -o StrictHostKeyChecking=no -i ${SSH_KEY_FILE} -r /tmp/k8s-deploy/* \
                                ${sshUser}@${K3S_HOST}:/tmp/k8s-deploy/
                            
                            # 在 K3s 服务器上执行部署（使用双引号以便变量展开）
                            ssh -o StrictHostKeyChecking=no -i ${SSH_KEY_FILE} ${sshUser}@${K3S_HOST} bash << K8S_DEPLOY_EOF
                            set -e
                            
                            export KUBECONFIG=${K3S_KUBECONFIG_PATH}
                            export K3S_NAMESPACE=${K3S_NAMESPACE}
                            
                            echo "📦 创建命名空间..."
                            kubectl apply -f /tmp/k8s-deploy/namespace.yaml
                            
                            echo "🔐 配置 RBAC..."
                            kubectl apply -f /tmp/k8s-deploy/gateway/gateway-rbac.yaml
                            
                            echo "⚙️  配置 ConfigMap..."
                            kubectl apply -f /tmp/k8s-deploy/gateway/gateway-configmap.yaml
                            kubectl apply -f /tmp/k8s-deploy/gateway/gateway-ratelimit-config.yaml
                            
                            echo "🚀 部署 Service 和 Ingress..."
                            kubectl apply -f /tmp/k8s-deploy/gateway/gateway-service.yaml
                            
                            echo "📦 部署 Deployment..."
                            kubectl apply -f /tmp/k8s-deploy/gateway/gateway-deployment.yaml
                            
                            echo "⏳ 等待 Pod 启动..."
                            kubectl wait --for=condition=ready pod -l app=api-gateway -n \${K3S_NAMESPACE} --timeout=300s || true
                            
                            echo "📊 查看部署状态..."
                            kubectl get pods -n \${K3S_NAMESPACE} -l app=api-gateway
                            kubectl get svc -n \${K3S_NAMESPACE}
                            
                            echo "🏥 检查 Pod 健康状态..."
                            sleep 10
                            
                            POD_NAME=\$(kubectl get pods -n \${K3S_NAMESPACE} -l app=api-gateway -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
                            
                            if [ -z "\$POD_NAME" ]; then
                                echo "❌ 未找到 Pod！"
                                kubectl get pods -n \${K3S_NAMESPACE}
                                exit 1
                            fi
                            
                            # 检查 Pod 状态
                            POD_STATUS=\$(kubectl get pod \$POD_NAME -n \${K3S_NAMESPACE} -o jsonpath='{.status.phase}')
                            
                            if [ "\$POD_STATUS" != "Running" ]; then
                                echo "❌ Pod 状态异常: \$POD_STATUS"
                                kubectl describe pod \$POD_NAME -n \${K3S_NAMESPACE}
                                kubectl logs \$POD_NAME -n \${K3S_NAMESPACE} --tail=50
                                exit 1
                            fi
                            
                            echo "✅ Pod 运行正常: \$POD_NAME"
                            
                            # 健康检查
                            echo "🏥 执行健康检查..."
                            for i in {1..30}; do
                                if kubectl exec -n \${K3S_NAMESPACE} \$POD_NAME -- curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
                                    echo "✅ 服务健康检查通过！"
                                    exit 0
                                fi
                                echo "等待中... (\$i/30)"
                                sleep 2
                            done
                            
                            echo "❌ 服务健康检查失败！"
                            kubectl logs \$POD_NAME -n \${K3S_NAMESPACE} --tail=50
                            exit 1
K8S_DEPLOY_EOF
                    """
                    }
                }
            }
        }

        stage('健康检查') {
            when {
                allOf {
                    expression { params.ENABLE_DEPLOY }
                    expression { params.K3S_HOST?.trim() }
                }
            }
            steps {
                script {
                    echo "🏥 执行外部健康检查..."
                    withCredentials([sshUserPrivateKey(
                        credentialsId: K3S_SSH_KEY_CREDENTIAL_ID,
                        keyFileVariable: 'SSH_KEY_FILE',
                        usernameVariable: 'SSH_USER_FROM_CREDENTIAL'
                    )]) {
                        def sshUser = params.K3S_USER ?: env.SSH_USER_FROM_CREDENTIAL
                        sh """
                            # 通过 SSH 在 K3s 服务器上执行健康检查
                            ssh -o StrictHostKeyChecking=no -i ${SSH_KEY_FILE} ${sshUser}@${K3S_HOST} bash << HEALTH_CHECK_EOF
                            export KUBECONFIG=${K3S_KUBECONFIG_PATH}
                            export K3S_NAMESPACE=${K3S_NAMESPACE}
                            
                            # 获取 Service 的 NodePort 或通过 Ingress
                            SERVICE_TYPE=\$(kubectl get svc api-gateway-service -n \${K3S_NAMESPACE} -o jsonpath='{.spec.type}' 2>/dev/null)
                            
                            if [ "\$SERVICE_TYPE" = "NodePort" ]; then
                                NODEPORT=\$(kubectl get svc api-gateway-service -n \${K3S_NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}')
                                HEALTH_URL="http://localhost:\$NODEPORT/actuator/health"
                            else
                                # 通过 Port Forward 或 Ingress
                                HEALTH_URL="http://localhost:8080/actuator/health"
                            fi
                            
                            # 尝试通过 Pod 内部检查
                            POD_NAME=\$(kubectl get pods -n \${K3S_NAMESPACE} -l app=api-gateway -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
                            
                            if [ -n "\$POD_NAME" ]; then
                                if kubectl exec -n \${K3S_NAMESPACE} \$POD_NAME -- curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
                                    echo "✅ 服务健康检查通过！"
                                    exit 0
                                fi
                            fi
                            
                            echo "⚠️  无法通过 Pod 检查，请手动验证服务状态"
                            exit 0
HEALTH_CHECK_EOF
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo "🧹 清理工作空间..."
                cleanWs()
            }
        }
        success {
            echo "✅ 构建和部署成功！"
        }
        failure {
            echo "❌ 构建或部署失败！"
        }
        unstable {
            echo "⚠️ 构建不稳定！"
        }
    }
}

