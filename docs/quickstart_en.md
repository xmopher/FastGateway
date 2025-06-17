# Quick Start Guide

This guide will help you get started with Mo Gateway quickly.

## Required Components

- JDK 21
- Maven 3.8+
- Docker
- Redis

## Local Development Setup

### 1. Clone the Repository

```bash
git clone https://github.com/xmopher/gateway.git
cd mo-gateway
```

### 2. Configure Maven

The project uses Maven for dependency management. The `pom.xml` file includes all necessary dependencies.

### 3. Start Services with Docker Compose

```bash
docker-compose up -d
```

This will start:
- Redis on port 6379
- Prometheus on port 9090
- Grafana on port 3000

### 4. Build the Project

```bash
mvn clean package
```

### 5. Run the Application

```bash
java -jar target/mo-gateway-1.0.0.jar
```

The gateway will start on port 8080.

## Verify Installation

### 1. Check Health Status

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
    "status": "UP"
}
```

### 2. Check Service Discovery

```bash
curl http://localhost:8080/actuator/gateway/routes
```

### 3. Check Monitoring

```bash
curl http://localhost:8080/actuator/prometheus
```

## Common Issues

### 1. Port Conflicts

If port 8080 is already in use, you can change it by setting the `server.port` property:

```bash
java -jar target/mo-gateway-1.0.0.jar --server.port=8081
```

### 2. Redis Connection Failure

If Redis connection fails, check:
- Redis is running: `docker ps | grep redis`
- Redis port is accessible: `telnet localhost 6379`
- Redis connection settings in `application.yaml`

### 3. Service Discovery Issues

If services are not discovered:
- Check service registration
- Verify service health endpoints
- Check service discovery configuration

## Development Modes

### Debug Mode

```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar target/mo-gateway-1.0.0.jar
```

### Hot Reload

For development with hot reload:

```bash
mvn spring-boot:run
```

## Next Steps

- Read the [Configuration Guide](configuration_en.md) to learn about available configuration options
- Check the [Service Integration Guide](integration_en.md) to learn how to connect your services
- Review the [Deployment Guide](deployment_en.md) for production deployment instructions 