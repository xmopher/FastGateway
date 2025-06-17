# Mo Gateway

Mo Gateway is a high-performance, lightweight API gateway built with Spring Cloud Gateway. It provides comprehensive features including service discovery, load balancing, rate limiting, and monitoring.

## Features

- **Service Discovery**: Support for Kubernetes and memory-based service discovery
- **Load Balancing**: Multiple load balancing strategies (Round Robin, Weighted Round Robin, Response Time Weighted)
- **Rate Limiting**: Token bucket algorithm with Redis-based distributed rate limiting
- **Monitoring**: Prometheus metrics and health checks
- **Security**: JWT authentication and authorization
- **High Availability**: Support for cluster deployment and failover

## Quick Start

### Prerequisites

- JDK 21
- Maven 3.8+
- Docker
- Redis

### Local Development

1. Clone the repository:
```bash
git clone https://github.com/xmopher/gateway.git
cd mo-gateway
```

2. Start Redis:
```bash
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

3. Build the project:
```bash
mvn clean package
```

4. Run the application:
```bash
java -jar target/mo-gateway-1.0.0.jar
```

The gateway will start on port 8080.

## Documentation

- [Quick Start Guide](docs/quickstart_en.md)
- [Configuration Guide](docs/configuration_en.md)
- [Service Integration Guide](docs/integration_en.md)
- [Deployment Guide](docs/deployment_en.md)

## Features in Detail

### Service Discovery
- Kubernetes service discovery
- Memory-based service discovery
- Health check support
- Service metadata management

### Load Balancing
- Round Robin
- Weighted Round Robin
- Response Time Weighted
- Custom load balancing strategies

### Rate Limiting
- Token bucket algorithm
- Redis-based distributed rate limiting
- Service-level and API-level rate limiting
- Custom rate limiting rules

### Monitoring
- Prometheus metrics
- Health check endpoints
- Performance monitoring
- Service status monitoring

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contact

- Project Link: [https://github.com/xmopher/gateway.git](https://github.com/yourusername/mo-gateway)
- Email: xmopher@hotmail.com

## Acknowledgments

- Spring Cloud Gateway
- Spring Boot
- Redis
- Prometheus 