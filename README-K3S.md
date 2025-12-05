# FastGateway K3s 部署指南

本文档专门介绍如何在 K3s 集群中部署 FastGateway 网关服务。

## 📋 目录

- [K3s 简介](#k3s-简介)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [部署步骤](#部署步骤)
- [配置说明](#配置说明)
- [服务发现](#服务发现)
- [访问网关](#访问网关)
- [运维管理](#运维管理)
- [故障排查](#故障排查)
- [与标准 Kubernetes 的区别](#与标准-kubernetes-的区别)

## K3s 简介

K3s 是 Rancher 开发的一个轻量级 Kubernetes 发行版，专为边缘计算、IoT、CI/CD 等场景优化。它完全兼容 Kubernetes API，但具有以下特点：

- **轻量级**：二进制文件小于 60MB，资源占用极低
- **快速启动**：单节点部署只需几秒钟
- **内置组件**：默认集成 Traefik Ingress Controller、CoreDNS、CNI 等
- **简单配置**：无需复杂配置即可运行生产级 Kubernetes 集群
- **ARM64 支持**：完美支持 ARM 架构，适合边缘设备

### K3s 架构优势

```
┌─────────────────────────────────────────────────────────────┐
│                      K3s 集群架构                            │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  K3s Master Node                                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   API Server │  │  Traefik     │  │  CoreDNS     │     │
│  │              │  │  Ingress     │  │              │     │
│  │              │  │  Controller  │  │              │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
                           │
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
┌───────▼──────┐  ┌────────▼────────┐  ┌─────▼─────┐
│  Worker Node │  │  Worker Node    │  │  ...      │
│              │  │                 │  │           │
│  Gateway Pod │  │  Business Pods  │  │           │
└──────────────┘  └─────────────────┘  └───────────┘
```

## 环境要求

### 系统要求

- **操作系统**：Linux（推荐 Ubuntu 20.04+、CentOS 7+）
- **CPU**：2 核及以上
- **内存**：4GB 及以上（推荐 8GB+）
- **磁盘**：20GB 及以上可用空间
- **网络**：能够访问容器镜像仓库

### 软件依赖

- **K3s**：v1.24+ （安装方法见下文）
- **kubectl**：v1.24+ （用于集群管理）
- **Docker**（可选）：如果需要本地构建镜像

### K3s 安装

#### 方式一：一键安装脚本（推荐）

```bash
# 安装 K3s Server（Master 节点）
curl -sfL https://get.k3s.io | sh -

# 安装 K3s Agent（Worker 节点，可选）
# 在 Master 节点获取 Token：sudo cat /var/lib/rancher/k3s/server/node-token
# 在 Worker 节点执行：
curl -sfL https://get.k3s.io | K3S_URL=https://<master-ip>:6443 K3S_TOKEN=<token> sh -
```

#### 方式二：指定版本安装

```bash
# 安装特定版本
curl -sfL https://get.k3s.io | INSTALL_K3S_VERSION=v1.28.0 sh -
```

#### 配置 kubectl

```bash
# K3s 的 kubeconfig 默认位置
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml

# 或者复制到默认位置
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $USER ~/.kube/config

# 验证安装
kubectl get nodes
```

## 快速开始

### 1. 前置准备

确保 K3s 集群正常运行：

```bash
# 检查集群状态
kubectl cluster-info
kubectl get nodes

# 检查 Traefik（K3s 默认 Ingress Controller）
kubectl get svc -n kube-system traefik
```

### 2. 部署 Redis（如果集群内未部署）

```bash
# 方式一：使用 Helm
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install redis bitnami/redis -n gateway-system --create-namespace

# 方式二：直接部署
kubectl apply -f - <<EOF
apiVersion: v1
kind: Namespace
metadata:
  name: gateway-system
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: gateway-system
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        ports:
        - containerPort: 6379
---
apiVersion: v1
kind: Service
metadata:
  name: redis-service
  namespace: gateway-system
spec:
  selector:
    app: redis
  ports:
  - port: 6379
    targetPort: 6379
EOF
```

### 3. 构建并推送镜像

```bash
# 构建镜像
docker build -t your-registry/gateway:1.0.0 .

# 推送镜像（如果使用私有仓库）
docker push your-registry/gateway:1.0.0

# 或者在 K3s 节点上直接导入（本地部署）
sudo k3s ctr images import gateway-1.0.0.tar
```

### 4. 使用部署脚本部署

```bash
# 设置镜像地址
export GATEWAY_IMAGE=your-registry/gateway:1.0.0

# 执行部署
chmod +x scripts/deploy-k3s.sh
./scripts/deploy-k3s.sh deploy 1.0.0
```

## 部署步骤

### 使用自动化部署脚本（推荐）

项目提供了完整的 K3s 部署脚本 `scripts/deploy-k3s.sh`，支持以下功能：

#### 基本部署

```bash
# 部署最新版本
./scripts/deploy-k3s.sh deploy

# 部署指定版本
./scripts/deploy-k3s.sh deploy 1.0.0

# 使用环境变量指定镜像
export GATEWAY_IMAGE=your-registry/gateway:1.0.0
./scripts/deploy-k3s.sh deploy
```

#### 查看状态

```bash
# 查看部署状态
./scripts/deploy-k3s.sh status

# 查看日志
./scripts/deploy-k3s.sh logs

# 查看服务发现配置
./scripts/deploy-k3s.sh discover
```

#### 更新和回滚

```bash
# 更新镜像
./scripts/deploy-k3s.sh update-image your-registry/gateway:1.0.1

# 更新服务配置
./scripts/deploy-k3s.sh update-service

# 故障排查
./scripts/deploy-k3s.sh troubleshoot
```

#### 删除部署

```bash
# 删除所有资源
./scripts/deploy-k3s.sh delete
```

### 手动部署步骤

如果不想使用脚本，可以手动执行以下步骤：

#### 1. 创建命名空间

```bash
kubectl apply -f k8s/namespace.yaml
```

#### 2. 配置 RBAC

```bash
kubectl apply -f k8s/gateway/gateway-rbac.yaml
```

#### 3. 创建 ConfigMap

```bash
kubectl apply -f k8s/gateway/gateway-configmap.yaml
kubectl apply -f k8s/gateway/gateway-ratelimit-config.yaml
```

#### 4. 部署应用

```bash
# 修改镜像地址
sed -i "s|image: .*|image: your-registry/gateway:1.0.0|" k8s/gateway/gateway-deployment.yaml

# 部署
kubectl apply -f k8s/gateway/gateway-deployment.yaml
```

#### 5. 创建 Service 和 Ingress

```bash
kubectl apply -f k8s/gateway/gateway-service.yaml
```

#### 6. 验证部署

```bash
# 等待 Pod 就绪
kubectl wait --for=condition=ready pod -l app=api-gateway -n gateway-system --timeout=300s

# 检查状态
kubectl get pods -n gateway-system
kubectl get svc -n gateway-system
kubectl get ingress -n gateway-system
```

## 配置说明

### 环境变量配置

网关服务通过环境变量进行配置，主要配置项如下：

```yaml
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "k8s"  # 使用 K8s 配置文件
  
  # Redis 配置
  - name: REDIS_HOST
    value: "redis-service"  # Redis Service 名称
  - name: REDIS_PORT
    value: "6379"
  
  # 限流配置
  - name: GATEWAY_RATELIMIT_ENABLED
    value: "true"
  - name: GATEWAY_RATELIMIT_CAPACITY
    value: "1000"
  
  # 服务发现配置
  - name: GATEWAY_DISCOVERY_TYPE
    value: "kubernetes"
  - name: GATEWAY_K8S_NAMESPACE
    value: "services"  # 服务发现的命名空间
  
  # JVM 配置
  - name: JAVA_OPTS
    value: "--enable-preview -XX:+UseContainerSupport -XX:MaxRAMPercentage=60.0"
```

### ConfigMap 配置

网关使用 ConfigMap 存储配置文件，主要配置在 `k8s/gateway/gateway-configmap.yaml`：

```yaml
gateway:
  discovery:
    type: kubernetes
    kubernetes:
      namespace: services
      label-selector: "gateway.enabled=true"  # 服务选择器
  ratelimit:
    enabled: true
    default-capacity: 1000
    default-refill-rate: 100
```

### Ingress 配置

K3s 默认使用 Traefik 作为 Ingress Controller，配置文件位于 `k8s/gateway/gateway-service.yaml`：

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: gateway-ingress
  namespace: gateway-system
spec:
  ingressClassName: traefik  # K3s 默认使用 traefik
  rules:
    - host: gateway.example.com  # 域名访问（可选）
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: api-gateway-service
                port:
                  number: 80
    - http:  # IP 访问（无域名时）
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: api-gateway-service
                port:
                  number: 80
```

### 资源限制配置

```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "200m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

根据实际负载调整资源限制，K3s 资源占用较低，可以适当减小。

## 服务发现

### Kubernetes 服务发现

网关支持自动发现 Kubernetes 集群中的服务，配置要点：

1. **服务命名空间**：在 `gateway-system` 命名空间中部署网关，自动发现 `services` 命名空间的服务

2. **服务标签**：服务需要添加 `gateway.enabled=true` 标签才能被网关发现

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
  namespace: services
  labels:
    gateway.enabled: "true"  # 必须添加此标签
spec:
  selector:
    app: my-service
  ports:
    - port: 8080
      targetPort: 8080
```

3. **服务端口**：默认发现服务的 8080 端口，可在 ConfigMap 中配置

4. **健康检查**：网关会自动检查服务的 `/actuator/health` 端点

### 示例：部署可被发现的服务

```bash
# 创建服务并添加标签
kubectl apply -f - <<EOF
apiVersion: v1
kind: Service
metadata:
  name: example-service
  namespace: services
  labels:
    gateway.enabled: "true"
spec:
  selector:
    app: example-service
  ports:
    - port: 8080
      targetPort: 8080
EOF

# 验证服务发现
kubectl get services -n services -l gateway.enabled=true
```

### 跨命名空间访问

如果服务在其他命名空间，网关可以通过完整 Service 地址访问：

```
http://service-name.namespace.svc.cluster.local:port
```

## 访问网关

### 方式一：通过 Traefik Ingress（推荐）

K3s 默认在端口 80（HTTP）和 443（HTTPS）暴露 Traefik。

```bash
# 获取节点 IP
NODE_IP=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')

# 健康检查
curl http://$NODE_IP/actuator/health

# 访问网关 API
curl http://$NODE_IP/api/v1/your-endpoint

# 如果有域名，配置 DNS 解析到节点 IP，然后访问
curl http://gateway.example.com/actuator/health
```

### 方式二：通过 Service（集群内部）

```bash
# 端口转发
kubectl port-forward -n gateway-system svc/api-gateway-service 8080:80

# 访问
curl http://localhost:8080/actuator/health
```

### 方式三：NodePort（如果配置了）

```yaml
apiVersion: v1
kind: Service
metadata:
  name: api-gateway-service
  namespace: gateway-system
spec:
  type: NodePort
  ports:
    - port: 80
      targetPort: 8080
      nodePort: 30080
```

访问：`http://<节点IP>:30080`

### 方式四：LoadBalancer（如果 K3s 配置了 LoadBalancer）

某些云环境或 MetalLB 可以提供 LoadBalancer 类型的服务。

## 运维管理

### 查看日志

```bash
# 实时日志
kubectl logs -n gateway-system -l app=api-gateway -f

# 查看最近 100 行
kubectl logs -n gateway-system -l app=api-gateway --tail=100

# 查看特定 Pod 日志
kubectl logs -n gateway-system <pod-name>
```

### 查看资源使用

```bash
# Pod 资源使用
kubectl top pods -n gateway-system

# 节点资源使用
kubectl top nodes
```

### 扩缩容

```bash
# 扩容到 3 个副本
kubectl scale deployment api-gateway -n gateway-system --replicas=3

# 查看副本状态
kubectl get deployment api-gateway -n gateway-system
```

### 更新配置

```bash
# 更新 ConfigMap
kubectl apply -f k8s/gateway/gateway-configmap.yaml

# 重启 Pod 使配置生效
kubectl rollout restart deployment api-gateway -n gateway-system
```

### 更新镜像

```bash
# 方式一：使用脚本
./scripts/deploy-k3s.sh update-image your-registry/gateway:1.0.1

# 方式二：手动更新
kubectl set image deployment/api-gateway \
  api-gateway=your-registry/gateway:1.0.1 \
  -n gateway-system

# 查看更新状态
kubectl rollout status deployment/api-gateway -n gateway-system
```

### 回滚

```bash
# 查看历史版本
kubectl rollout history deployment/api-gateway -n gateway-system

# 回滚到上一版本
kubectl rollout undo deployment/api-gateway -n gateway-system

# 回滚到指定版本
kubectl rollout undo deployment/api-gateway --to-revision=2 -n gateway-system
```

### 健康检查

```bash
# 检查 Pod 健康状态
kubectl get pods -n gateway-system -l app=api-gateway

# 检查健康端点
curl http://<节点IP>/actuator/health

# 检查就绪状态
kubectl get endpoints api-gateway-service -n gateway-system
```

## 故障排查

### Pod 无法启动

```bash
# 查看 Pod 状态
kubectl get pods -n gateway-system

# 查看 Pod 详细信息
kubectl describe pod <pod-name> -n gateway-system

# 查看事件
kubectl get events -n gateway-system --sort-by='.lastTimestamp'

# 常见问题：
# 1. 镜像拉取失败 - 检查镜像地址和拉取权限
# 2. 资源不足 - 检查节点资源
# 3. 配置错误 - 检查 ConfigMap 和环境变量
```

### 服务无法访问

```bash
# 检查 Service
kubectl get svc -n gateway-system

# 检查 Endpoints
kubectl get endpoints api-gateway-service -n gateway-system

# 检查 Ingress
kubectl describe ingress gateway-ingress -n gateway-system

# 检查 Traefik
kubectl get pods -n kube-system -l app.kubernetes.io/name=traefik
kubectl logs -n kube-system -l app.kubernetes.io/name=traefik
```

### Redis 连接失败

```bash
# 检查 Redis 服务
kubectl get svc redis-service -n gateway-system

# 测试 Redis 连接
kubectl run -it --rm redis-test --image=redis:7-alpine --restart=Never -- \
  redis-cli -h redis-service.gateway-system.svc.cluster.local ping

# 检查网关日志中的 Redis 错误
kubectl logs -n gateway-system -l app=api-gateway | grep -i redis
```

### 服务发现失败

```bash
# 检查 RBAC 权限
kubectl get clusterrole gateway-service-reader
kubectl get clusterrolebinding gateway-service-reader-binding

# 检查可发现的服务
kubectl get services -n services -l gateway.enabled=true

# 检查网关日志
kubectl logs -n gateway-system -l app=api-gateway | grep -i discovery
```

### 性能问题

```bash
# 查看资源使用
kubectl top pods -n gateway-system

# 查看 JVM 内存使用（需要启用相关端点）
curl http://<节点IP>/actuator/metrics/jvm.memory.used

# 增加资源限制
kubectl edit deployment api-gateway -n gateway-system
```

### 常见错误和解决方案

| 问题 | 可能原因 | 解决方案 |
|------|---------|---------|
| Pod 一直处于 Pending | 资源不足或节点不可调度 | 检查节点资源，添加更多节点 |
| Pod 一直处于 CrashLoopBackOff | 应用启动失败 | 查看日志，检查配置和依赖 |
| Service 无 Endpoints | Pod 未就绪或标签不匹配 | 检查 Pod 状态和 Service 选择器 |
| Ingress 无法访问 | Traefik 未运行或配置错误 | 检查 Traefik 状态和 Ingress 配置 |
| 连接超时 | 网络策略或防火墙 | 检查网络策略和防火墙规则 |

## 与标准 Kubernetes 的区别

### 主要差异

| 特性 | K3s | 标准 Kubernetes |
|------|-----|----------------|
| **Ingress Controller** | 内置 Traefik | 需要手动安装（如 Nginx、Traefik） |
| **CNI** | 内置 Flannel | 需要手动配置 |
| **DNS** | 内置 CoreDNS | 需要手动部署 |
| **存储类** | 内置 local-path | 需要配置存储类 |
| **API Server** | 单个进程 | 多个组件 |
| **资源占用** | 极低（<512MB） | 较高（>2GB） |
| **启动时间** | 几秒钟 | 几分钟 |

### K3s 特有配置

#### 1. Traefik 配置

K3s 的 Traefik 配置位于 `/var/lib/rancher/k3s/server/manifests/traefik.yaml`，可以自定义配置。

#### 2. 存储类

K3s 默认提供 `local-path` 存储类，适合单节点或开发环境。

#### 3. 镜像仓库

K3s 可以使用本地镜像仓库，配置方法：

```bash
# 创建镜像仓库配置
sudo mkdir -p /etc/rancher/k3s
sudo tee /etc/rancher/k3s/registries.yaml <<EOF
mirrors:
  "your-registry.com":
    endpoint:
      - "https://your-registry.com"
EOF

# 重启 K3s
sudo systemctl restart k3s
```

### 迁移注意事项

如果从标准 Kubernetes 迁移到 K3s：

1. **Ingress**：修改 `ingressClassName` 为 `traefik`
2. **存储类**：检查存储类兼容性，可能需要使用 `local-path`
3. **网络策略**：K3s 使用 Flannel，网络策略配置可能不同
4. **资源限制**：K3s 资源占用更低，可以适当减小资源请求

## 最佳实践

### 1. 资源规划

- **开发环境**：1-2 个副本，256Mi 内存
- **生产环境**：2-3 个副本，512Mi-1Gi 内存
- **高负载环境**：3+ 个副本，1Gi+ 内存

### 2. 高可用部署

```yaml
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
```

### 3. 监控和日志

- 集成 Prometheus 监控（K3s 可以安装 Prometheus Operator）
- 配置日志收集（如 Loki + Grafana）
- 设置告警规则

### 4. 备份和恢复

定期备份重要的 ConfigMap 和 Secret：

```bash
# 备份配置
kubectl get configmap gateway-config -n gateway-system -o yaml > backup-configmap.yaml
kubectl get secret -n gateway-system -o yaml > backup-secrets.yaml
```

### 5. 安全加固

- 使用 RBAC 限制权限
- 启用 TLS/SSL
- 定期更新镜像版本
- 扫描镜像漏洞

## 相关文档

- [主 README](README.md) - 项目总体介绍
- [部署指南](docs/deployment.md) - 通用部署文档
- [配置指南](docs/configuration.md) - 详细配置说明
- [服务接入指南](docs/integration.md) - 业务服务接入方法
- [前端部署指南](docs/frontend-deployment.md) - 前端服务部署

## 支持和反馈

如有问题或建议，请：

1. 查看 [故障排查](#故障排查) 章节
2. 提交 [Issue](https://github.com/xmopher/gateway/issues)
3. 联系维护者：xmopher@hotmail.com

## 许可证

本项目采用 MIT 许可证

---

**最后更新**：2024年
