# Configuration Guide

This document details the configuration options available in Mo Gateway.

## Basic Configuration

### Service Settings

```yaml
server:
  port: 8080
  servlet:
    context-path: /
```

### Application Settings

```yaml
spring:
  application:
    name: mo-gateway
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
```

## Core Functionality Configuration

### Rate Limiting

```yaml
gateway:
  ratelimit:
    enabled: true
    redis:
      host: localhost
      port: 6379
    default:
      replenishRate: 10
      burstCapacity: 20
    services:
      service-a:
        replenishRate: 5
        burstCapacity: 10
```

### Load Balancing

```yaml
gateway:
  loadbalancer:
    type: round_robin  # round_robin, weighted_round_robin, response_time_weighted
    services:
      service-a:
        type: weighted_round_robin
        weights:
          instance1: 2
          instance2: 1
```

### Service Discovery

#### Kubernetes Mode

```yaml
gateway:
  discovery:
    type: kubernetes
    kubernetes:
      namespace: default
      service-label: gateway-enabled
```

#### Memory Mode

```yaml
gateway:
  discovery:
    type: memory
    services:
      service-a:
        - url: http://localhost:8081
          weight: 1
        - url: http://localhost:8082
          weight: 2
```

## Monitoring Configuration

### Prometheus

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,gateway
  metrics:
    export:
      prometheus:
        enabled: true
```

### Logging

```yaml
logging:
  level:
    root: INFO
    com.mo.gateway: DEBUG
  file:
    name: logs/gateway.log
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| GATEWAY_RATELIMIT_ENABLED | Enable rate limiting | false |
| GATEWAY_DISCOVERY_TYPE | Service discovery type | memory |
| REDIS_HOST | Redis host | localhost |
| REDIS_PORT | Redis port | 6379 |

## Best Practices

### Rate Limiting

- Set appropriate replenish rates based on service capacity
- Configure burst capacity for traffic spikes
- Use service-specific rate limits for critical services

### Load Balancing

- Use weighted round-robin for services with different capacities
- Monitor response times for response-time-weighted strategy
- Configure health checks for all instances

### Monitoring

- Enable all necessary metrics
- Configure appropriate log levels
- Set up alerting rules

## Configuration Validation

### Check Configuration

```bash
curl http://localhost:8080/actuator/configprops
```

### Check Environment Variables

```bash
curl http://localhost:8080/actuator/env
```

## Common Issues

### Configuration Not Taking Effect

- Check configuration file location
- Verify environment variables
- Restart the application

### Configuration Conflicts

- Check for duplicate configurations
- Verify property precedence
- Review environment variables

## Related Documents

- [Quick Start Guide](quickstart_en.md)
- [Service Integration Guide](integration_en.md)
- [Deployment Guide](deployment_en.md) 