# 服务接入指南

本文档详细说明了如何将您的服务接入 Mo Gateway。

## 接入流程

### 1. 服务准备

#### 1.1 健康检查接口

确保您的服务实现了健康检查接口：

```java
@RestController
public class HealthController {
    @GetMapping("/actuator/health")
    public ResponseEntity<Health> health() {
        return ResponseEntity.ok(Health.up().build());
    }
}
```

#### 1.2 监控接口

实现基本的监控指标：

```java
@Configuration
public class MetricsConfig {
    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}
```

### 2. 服务注册

#### 2.1 Kubernetes 环境

1. 添加服务标签：

```yaml
metadata:
  labels:
    gateway.enabled: "true"
    gateway.service-type: "your-service-type"
```

2. 配置健康检查：

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
```

#### 2.2 非 Kubernetes 环境

1. 配置服务路由：

```yaml
gateway:
  routes:
    - id: your-service
      uri: http://your-service:8080
      predicates:
        - Path=/api/your-service/**
      filters:
        - StripPrefix=1
```

### 3. 限流配置

#### 3.1 服务级别限流

```yaml
gateway:
  ratelimit:
    service-strategies:
      your-service:
        capacity: 1000
        refill-rate: 100
        algorithm: tokenBucket
```

#### 3.2 接口级别限流

```yaml
gateway:
  ratelimit:
    api-strategies:
      your-service:
        /api/v1/users:
          capacity: 100
          refill-rate: 10
        /api/v1/products:
          capacity: 200
          refill-rate: 20
```

### 4. 负载均衡配置

#### 4.1 基本配置

```yaml
gateway:
  loadbalancer:
    service-strategies:
      your-service: 
        type: weightedRoundRobin
        weights:
          instance1: 2
          instance2: 1
```

#### 4.2 高级配置

```yaml
gateway:
  loadbalancer:
    service-strategies:
      your-service:
        type: responseTimeWeighted
        response-time-weight: 0.7
        connection-weight: 0.3
```

### 5. 安全配置

#### 5.1 认证配置

```yaml
gateway:
  security:
    auth:
      type: jwt
      jwt:
        secret: your-secret-key
        expiration: 3600
```

#### 5.2 授权配置

```yaml
gateway:
  security:
    authorization:
      rules:
        - path: /api/v1/admin/**
          roles: [ADMIN]
        - path: /api/v1/user/**
          roles: [USER]
```

## 最佳实践

### 1. 服务设计建议

- 实现优雅的服务启动和关闭
- 提供详细的健康检查信息
- 实现服务降级策略
- 添加必要的监控指标

### 2. 性能优化建议

- 使用连接池
- 实现请求缓存
- 优化响应数据大小
- 使用异步处理

### 3. 安全建议

- 实现请求签名验证
- 使用 HTTPS
- 实现请求频率限制
- 添加必要的日志记录

## 测试验证

### 1. 服务注册测试

```bash
# 检查服务是否注册成功
curl http://localhost:8080/actuator/gateway/routes
```

### 2. 限流测试

```bash
# 测试限流功能
for i in {1..100}; do
  curl http://localhost:8080/api/your-service/test
done
```

### 3. 负载均衡测试

```bash
# 测试负载均衡
for i in {1..10}; do
  curl http://localhost:8080/api/your-service/test
done
```

## 常见问题

### 1. 服务注册失败

- 检查服务健康检查接口
- 验证服务标签配置
- 确认网络连接

### 2. 限流不生效

- 检查限流配置
- 验证 Redis 连接
- 确认限流算法配置

### 3. 负载均衡异常

- 检查服务实例状态
- 验证负载均衡配置
- 确认服务权重设置

## 相关文档

- [配置指南](configuration.md)
- [部署指南](deployment.md) 