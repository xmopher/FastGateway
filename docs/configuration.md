# 配置指南

本文档详细说明了 Mo Gateway 的配置选项。

## 基础配置

### 服务配置

```yaml
server:
  port: 8080
  ssl:
    enabled: false
    key-store: classpath:keystore.p12
    key-store-password: your-password
    key-store-type: PKCS12
  http2:
    enabled: true
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain
```

### 应用配置

```yaml
spring:
  application:
    name: gateway-service
  profiles:
    active: dev
```

## 核心功能配置

### 限流配置

```yaml
gateway:
  ratelimit:
    enabled: true
    default-algorithm: tokenBucket
    default-capacity: 1000
    default-refill-rate: 100
    default-window-size-ms: 60000
    fail-open: true
    service-strategies:
      your-service:
        capacity: 2000
        refill-rate: 200
        algorithm: slidingWindow
```

### 负载均衡配置

```yaml
gateway:
  loadbalancer:
    default-strategy: roundRobin
    health-check-interval-ms: 30000
    connection-timeout-ms: 5000
    read-timeout-ms: 10000
    enable-circuit-breaker: true
    service-strategies:
      your-service:
        type: weightedRoundRobin
        weights:
          instance1: 2
          instance2: 1
```

### 服务发现配置

#### Kubernetes 模式

```yaml
gateway:
  discovery:
    type: kubernetes
    kubernetes:
      namespace: services
      label-selector: gateway.enabled=true
      service-port: 8080
      health-check-path: /actuator/health
```

#### 内存模式

```yaml
gateway:
  discovery:
    type: memory
    services:
      - name: your-service
        instances:
          - host: localhost
            port: 8081
            weight: 1
          - host: localhost
            port: 8082
            weight: 2
```

## 监控配置

### Prometheus 配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    tags:
      application: ${spring.application.name}
```

### 日志配置

```yaml
logging:
  level:
    com.mo.gateway: INFO
    org.springframework.web: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/gateway.log
```

## 环境变量配置

支持通过环境变量覆盖默认配置：

| 环境变量 | 说明 | 默认值 |
|---------|------|--------|
| SPRING_PROFILES_ACTIVE | 激活的配置文件 | dev |
| REDIS_HOST | Redis 主机地址 | localhost |
| REDIS_PORT | Redis 端口 | 6379 |
| GATEWAY_RATELIMIT_ENABLED | 是否启用限流 | true |
| GATEWAY_RATELIMIT_CAPACITY | 限流容量 | 1000 |
| GATEWAY_RATELIMIT_REFILL_RATE | 限流补充速率 | 100 |
| GATEWAY_DISCOVERY_TYPE | 服务发现类型 | memory |
| GATEWAY_LOG_LEVEL | 日志级别 | INFO |

## 配置最佳实践

### 1. 限流配置建议

- 根据服务容量设置合理的限流阈值
- 使用 fail-open 模式保证系统可用性
- 为不同服务配置不同的限流策略

### 2. 负载均衡配置建议

- 根据服务特性选择合适的负载均衡算法
- 配置合理的健康检查间隔
- 设置适当的超时时间

### 3. 监控配置建议

- 启用详细的健康检查
- 配置合适的日志级别
- 添加必要的监控标签

## 配置验证

### 1. 配置检查

访问配置检查端点：

```bash
curl http://localhost:8080/actuator/configprops
```

### 2. 环境变量检查

访问环境变量端点：

```bash
curl http://localhost:8080/actuator/env
```

## 常见问题

### 1. 配置不生效

- 检查配置文件位置是否正确
- 确认配置项名称是否正确
- 验证环境变量是否正确设置

### 2. 配置冲突

- 检查不同配置文件中的配置项
- 确认配置优先级
- 查看配置合并结果

## 相关文档

- [服务接入指南](integration.md)
- [部署指南](deployment.md) 