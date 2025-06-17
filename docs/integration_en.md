# Service Integration Guide

This guide explains how to integrate your services with Mo Gateway.

## Integration Process

### 1. Service Preparation

#### Health Check Implementation

```java
@RestController
@RequestMapping("/actuator")
public class HealthController {
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        return ResponseEntity.ok(response);
    }
}
```

#### Monitoring Implementation

```java
@Configuration
public class MonitoringConfig {
    
    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}
```

### 2. Service Registration

#### Kubernetes Environment

Add labels to your service:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: your-service
  labels:
    gateway-enabled: "true"
spec:
  selector:
    app: your-service
  ports:
    - port: 8080
      targetPort: 8080
```

#### Non-Kubernetes Environment

Register service in memory mode:

```yaml
gateway:
  discovery:
    type: memory
    services:
      your-service:
        - url: http://localhost:8080
          weight: 1
        - url: http://localhost:8081
          weight: 1
```

### 3. Rate Limiting Configuration

#### Service Level

```yaml
gateway:
  ratelimit:
    services:
      your-service:
        replenishRate: 10
        burstCapacity: 20
```

#### API Level

```yaml
gateway:
  ratelimit:
    apis:
      your-service:
        /api/v1/users:
          replenishRate: 5
          burstCapacity: 10
        /api/v1/orders:
          replenishRate: 20
          burstCapacity: 40
```

### 4. Load Balancing Configuration

#### Basic Configuration

```yaml
gateway:
  loadbalancer:
    type: round_robin
```

#### Advanced Configuration

```yaml
gateway:
  loadbalancer:
    type: weighted_round_robin
    services:
      your-service:
        weights:
          instance1: 2
          instance2: 1
```

### 5. Security Configuration

#### Authentication

```yaml
gateway:
  security:
    jwt:
      enabled: true
      secret: your-secret-key
      expiration: 3600
```

#### Authorization

```yaml
gateway:
  security:
    authorization:
      enabled: true
      rules:
        /api/v1/admin/**:
          roles: [ADMIN]
        /api/v1/user/**:
          roles: [USER]
```

## Best Practices

### Service Design

- Implement health check endpoints
- Use standard monitoring metrics
- Follow RESTful API design principles
- Implement proper error handling

### Performance Optimization

- Use connection pooling
- Implement caching where appropriate
- Optimize response payloads
- Use compression for large responses

### Security Measures

- Implement proper authentication
- Use HTTPS for all communications
- Implement rate limiting
- Use proper authorization

## Testing and Validation

### Test Service Registration

```bash
curl http://localhost:8080/actuator/gateway/routes
```

### Test Rate Limiting

```bash
# Test rate limit
for i in {1..11}; do
  curl http://localhost:8080/your-service/api
done
```

### Test Load Balancing

```bash
# Test load balancing
for i in {1..10}; do
  curl http://localhost:8080/your-service/api
done
```

## Common Issues

### Service Registration Failure

- Check service health endpoint
- Verify service labels
- Check network connectivity
- Review service discovery configuration

### Rate Limiting Not Working

- Check Redis connection
- Verify rate limit configuration
- Check service registration
- Review rate limit rules

### Load Balancing Issues

- Check service health
- Verify load balancing configuration
- Check instance weights
- Review service discovery

## Related Documents

- [Quick Start Guide](quickstart_en.md)
- [Configuration Guide](configuration_en.md)
- [Deployment Guide](deployment_en.md) 