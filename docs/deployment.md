# 部署指南

本文档详细说明了如何部署 Mo Gateway 到不同环境。

## 部署方式

### 1. Docker 部署

#### 1.1 单机部署

1. 构建镜像：

```bash
docker build -t mo/gateway:1.0.0 .
```

2. 运行容器：

```bash
docker run -d \
  --name gateway \
  -p 8080:8080 \
  -e REDIS_HOST=redis \
  -e GATEWAY_RATELIMIT_ENABLED=true \
  -e GATEWAY_DISCOVERY_TYPE=memory \
  mo/gateway:1.0.0
```

#### 1.2 集群部署

1. 创建 Docker 网络：

```bash
docker network create gateway-network
```

2. 启动 Redis：

```bash
docker run -d \
  --name redis \
  --network gateway-network \
  redis:7-alpine
```

3. 启动多个网关实例：

```bash
# 实例 1
docker run -d \
  --name gateway-1 \
  --network gateway-network \
  -p 8080:8080 \
  -e REDIS_HOST=redis \
  -e GATEWAY_RATELIMIT_ENABLED=true \
  mo/gateway:1.0.0

# 实例 2
docker run -d \
  --name gateway-2 \
  --network gateway-network \
  -p 8081:8080 \
  -e REDIS_HOST=redis \
  -e GATEWAY_RATELIMIT_ENABLED=true \
  mo/gateway:1.0.0
```

### 2. Kubernetes 部署

#### 2.1 基础部署

1. 创建命名空间：

```bash
kubectl create namespace gateway
```

2. 创建 ConfigMap：

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: gateway-config
  namespace: gateway
data:
  application.yaml: |
    gateway:
      ratelimit:
        enabled: true
      discovery:
        type: kubernetes
```

3. 创建 Deployment：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway
  namespace: gateway
spec:
  replicas: 2
  selector:
    matchLabels:
      app: gateway
  template:
    metadata:
      labels:
        app: gateway
    spec:
      containers:
      - name: gateway
        image: mo/gateway:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: REDIS_HOST
          value: redis
        volumeMounts:
        - name: config
          mountPath: /app/config
      volumes:
      - name: config
        configMap:
          name: gateway-config
```

4. 创建 Service：

```yaml
apiVersion: v1
kind: Service
metadata:
  name: gateway
  namespace: gateway
spec:
  selector:
    app: gateway
  ports:
  - port: 80
    targetPort: 8080
  type: LoadBalancer
```

#### 2.2 高可用部署

1. 创建 Redis 集群：

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: redis
  namespace: gateway
spec:
  serviceName: redis
  replicas: 3
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
```

2. 配置网关高可用：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway
  namespace: gateway
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: gateway
    spec:
      containers:
      - name: gateway
        image: mo/gateway:1.0.0
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 15
```

## 部署检查

### 1. 健康检查

```bash
# 检查服务健康状态
curl http://localhost:8080/actuator/health
```

### 2. 服务发现检查

```bash
# 检查服务注册状态
curl http://localhost:8080/actuator/gateway/routes
```

### 3. 监控检查

```bash
# 检查监控指标
curl http://localhost:8080/actuator/prometheus
```

## 部署最佳实践

### 1. 资源规划

- CPU: 2-4 核
- 内存: 4-8GB
- 磁盘: 20GB+
- 网络: 1Gbps+

### 2. 高可用配置

- 使用多实例部署
- 配置健康检查
- 设置合理的资源限制
- 实现优雅关闭

### 3. 监控配置

- 配置 Prometheus 监控
- 设置告警规则
- 配置日志收集
- 设置性能指标

## 常见问题

### 1. 部署失败

- 检查资源配额
- 验证网络连接
- 确认配置正确性
- 查看错误日志

### 2. 性能问题

- 检查资源使用情况
- 优化 JVM 参数
- 调整线程池配置
- 检查网络延迟

### 3. 高可用问题

- 检查服务发现
- 验证负载均衡
- 确认故障转移
- 测试服务恢复

## 相关文档

- [配置指南](configuration.md)
- [服务接入指南](integration.md) 