# Deployment Guide

This document provides detailed instructions for deploying Mo Gateway in different environments.

## Deployment Methods

### 1. Docker Deployment

#### 1.1 Single Instance

1. Build the image:

```bash
docker build -t mo/gateway:1.0.0 .
```

2. Run the container:

```bash
docker run -d \
  --name gateway \
  -p 8080:8080 \
  -e REDIS_HOST=redis \
  -e GATEWAY_RATELIMIT_ENABLED=true \
  -e GATEWAY_DISCOVERY_TYPE=memory \
  mo/gateway:1.0.0
```

#### 1.2 Cluster Deployment

1. Create Docker network:

```bash
docker network create gateway-network
```

2. Start Redis:

```bash
docker run -d \
  --name redis \
  --network gateway-network \
  redis:7-alpine
```

3. Start multiple gateway instances:

```bash
# Instance 1
docker run -d \
  --name gateway-1 \
  --network gateway-network \
  -p 8080:8080 \
  -e REDIS_HOST=redis \
  -e GATEWAY_RATELIMIT_ENABLED=true \
  mo/gateway:1.0.0

# Instance 2
docker run -d \
  --name gateway-2 \
  --network gateway-network \
  -p 8081:8080 \
  -e REDIS_HOST=redis \
  -e GATEWAY_RATELIMIT_ENABLED=true \
  mo/gateway:1.0.0
```

### 2. Kubernetes Deployment

#### 2.1 Basic Deployment

1. Create namespace:

```bash
kubectl create namespace gateway
```

2. Create ConfigMap:

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

3. Create Deployment:

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

4. Create Service:

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

#### 2.2 High Availability Deployment

1. Create Redis cluster:

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

2. Configure gateway high availability:

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

## Deployment Verification

### 1. Health Check

```bash
# Check service health status
curl http://localhost:8080/actuator/health
```

### 2. Service Discovery Check

```bash
# Check service registration status
curl http://localhost:8080/actuator/gateway/routes
```

### 3. Monitoring Check

```bash
# Check monitoring metrics
curl http://localhost:8080/actuator/prometheus
```

## Deployment Best Practices

### 1. Resource Planning

- CPU: 2-4 cores
- Memory: 4-8GB
- Disk: 20GB+
- Network: 1Gbps+

### 2. High Availability Configuration

- Use multiple instances
- Configure health checks
- Set appropriate resource limits
- Implement graceful shutdown

### 3. Monitoring Configuration

- Configure Prometheus monitoring
- Set up alerting rules
- Configure log collection
- Set performance metrics

## Common Issues

### 1. Deployment Failure

- Check resource quotas
- Verify network connectivity
- Confirm configuration correctness
- Check error logs

### 2. Performance Issues

- Check resource usage
- Optimize JVM parameters
- Adjust thread pool configuration
- Check network latency

### 3. High Availability Issues

- Check service discovery
- Verify load balancing
- Confirm failover
- Test service recovery

## Related Documents

- [Quick Start Guide](quickstart_en.md)
- [Configuration Guide](configuration_en.md)
- [Service Integration Guide](integration_en.md) 