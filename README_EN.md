# Mo Gateway

Mo Gateway is a high-performance, lightweight API gateway built with Spring Cloud Gateway. It provides comprehensive features including service discovery, load balancing, rate limiting, and monitoring.

## Features

- **Service Discovery**: Support for Kubernetes and memory-based service discovery
- **Load Balancing**: Multiple load balancing strategies (Round Robin, Weighted Round Robin, Least Connections)
- **Rate Limiting**: Token bucket and sliding window algorithms with Redis-based distributed rate limiting
- **Pluggable Authentication**: SPI-based authentication system with JWT, API Key, and custom provider support
- **Monitoring**: Prometheus metrics and comprehensive health checks
- **Security**: SSL/TLS encryption, JWT authentication, and API key management
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

## Configuration

### Authentication Configuration

#### JWT Authentication
```yaml
gateway:
  auth:
    enabled: true
    plugin-directory: plugins
    enabled-providers:
      - jwt
      - apikey
    provider-configs:
      jwt:
        secret: ${JWT_SECRET:your-secret-key}
        issuer: ${JWT_ISSUER:mo-gateway}
        expiration-minutes: ${JWT_EXPIRATION_MINUTES:60}
```

#### API Key Authentication
```yaml
gateway:
  auth:
    provider-configs:
      apikey:
        cache-enabled: ${APIKEY_CACHE_ENABLED:true}
        cache-expiration-minutes: ${APIKEY_CACHE_EXPIRATION_MINUTES:30}
```

#### Custom Authentication Plugin
To add a custom authentication plugin:
1. Implement the `AuthenticationProvider` interface
2. Register in `META-INF/services/com.mo.gateway.spi.auth.AuthenticationProvider`
3. Place the plugin JAR in the `plugins` directory

### Authentication Usage Examples

#### JWT Authentication Request
```bash
# Access protected API with JWT Token
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
     -X GET "http://gateway:8080/api/v1/protected-resource"
```

#### API Key Authentication Request
```bash
# Access protected API with API Key
curl -H "X-API-Key: your-api-key-here" \
     -X GET "http://gateway:8080/api/v1/protected-resource"
```

#### Authentication Failure Response
```json
{
  "timestamp": "2024-01-20T10:30:00Z",
  "status": 401,
  "error": "AUTHENTICATION_FAILED",
  "message": "Invalid JWT token",
  "path": "/api/v1/protected-resource"
}
```

## Documentation

- [Quick Start Guide](docs/quickstart_en.md)
- [Configuration Guide](docs/configuration_en.md)
- [Service Integration Guide](docs/integration_en.md)
- [Deployment Guide](docs/deployment_en.md)

## Features in Detail

### Pluggable Authentication System
- **SPI Architecture**: Extensible authentication system based on Java SPI mechanism
- **Multiple Authentication Strategies**: Support for JWT, API Key, and custom authentication methods
- **Hot-pluggable Support**: Authentication plugins can be dynamically loaded and unloaded
- **Chain Processing**: Multiple authentication providers execute in priority order
- **Caching Mechanism**: Redis caching for authentication results to improve performance
- **Complete SSL/TLS Encryption**: Full support for secure communications

#### Authentication Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Authentication Flow                              │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────┐    ┌─────────────────────────────────────────────────┐
│   Client    │────│               Gateway Entry                     │
│   Request   │    │            (GatewayController)                 │
└─────────────┘    └─────────────────┬───────────────────────────────┘
                                    │
                                    ▼
                   ┌─────────────────────────────────────────────────┐
                   │           Authentication Service                │
                   │         (AuthenticationService)                │
                   └─────────────────┬───────────────────────────────┘
                                    │
                                    ▼
                   ┌─────────────────────────────────────────────────┐
                   │            Plugin Manager                       │
                   │          (AuthPluginManager)                   │
                   │                                                │
                   │  ┌─────────────┐  ┌─────────────────────────┐  │
                   │  │ JWT Plugin  │  │    API Key Plugin       │  │
                   │  │             │  │                         │  │
                   │  │ ┌─────────┐ │  │ ┌─────────────────────┐ │  │
                   │  │ │JWT Parse│ │  │ │   API Key Verify    │ │  │
                   │  │ │Token    │ │  │ │                     │ │  │
                   │  │ │Verify   │ │  │ │   Permission Check  │ │  │
                   │  │ │Extract  │ │  │ │                     │ │  │
                   │  │ └─────────┘ │  │ └─────────────────────┘ │  │
                   │  └─────────────┘  └─────────────────────────┘  │
                   └─────────────────┬───────────────────────────────┘
                                    │
                                    ▼
                   ┌─────────────────────────────────────────────────┐
                   │          Authentication Context                 │
                   │         (AuthenticationContext)                │
                   │                                                │
                   │  ┌─────────────┐  ┌─────────────────────────┐  │
                   │  │Cache Mgmt   │  │    Config Management    │  │
                   │  │             │  │                         │  │
                   │  │ ┌─────────┐ │  │ ┌─────────────────────┐ │  │
                   │  │ │Redis    │ │  │ │  Dynamic Config     │ │  │
                   │  │ │Cache    │ │  │ │                     │ │  │
                   │  │ │Results  │ │  │ │  Environment Vars   │ │  │
                   │  │ └─────────┘ │  │ │                     │ │  │
                   │  └─────────────┘  │ │  Property Files     │ │  │
                   │                   │ └─────────────────────┘ │  │
                   │                   └─────────────────────────┘  │
                   └─────────────────┬───────────────────────────────┘
                                    │
                                    ▼
                   ┌─────────────────────────────────────────────────┐
                   │         Authentication Result                   │
                   │        (AuthenticationResult)                  │
                   │                                                │
                   │ Success: User Info + Roles + Permissions       │
                   │ Failure: Error Message + Error Code            │
                   └─────────────────────────────────────────────────┘
```

#### SPI-based Authentication Providers

```
┌─────────────────────────────────────────────────────────────────────┐
│                  SPI Authentication Plugin Architecture             │
└─────────────────────────────────────────────────────────────────────┘

META-INF/services/com.mo.gateway.spi.auth.AuthenticationProvider
├── com.mo.gateway.plugin.auth.jwt.JwtAuthenticationProvider
└── com.mo.gateway.plugin.auth.apikey.ApiKeyAuthenticationProvider

┌─────────────────────────────────────────────────────────────────────┐
│                AuthenticationProvider SPI Interface                 │
├─────────────────────────────────────────────────────────────────────┤
│  + getProviderName() : String                                      │
│  + getVersion() : String                                           │
│  + supports(request) : boolean                                     │
│  + authenticate(request, context) : AuthenticationResult          │
│  + initialize(config) : void                                      │
│  + destroy() : void                                               │
│  + health() : HealthStatus                                        │
└─────────────────────────────────────────────────────────────────────┘
                                    ▲
                                    │
                    ┌───────────────┴───────────────┐
                    │                               │
          ┌─────────────────────┐        ┌─────────────────────┐
          │  JWT Auth Provider  │        │ API Key Auth Provider│
          │                    │        │                     │
          │ • Token Parsing    │        │ • Key Verification  │
          │ • Signature Verify │        │ • Permission Check  │
          │ • Expiry Check     │        │ • Cache Support     │
          │ • Role Extraction  │        │ • Rate Limiting     │
          │ • Permission Extract│        │                     │
          └─────────────────────┘        └─────────────────────┘
```

### Service Discovery
- Kubernetes service discovery
- Memory-based service discovery
- Health check support
- Service metadata management

### Load Balancing
- Round Robin
- Weighted Round Robin
- Least Connections
- Custom load balancing strategies

### Rate Limiting
- Token bucket algorithm
- Sliding window algorithm
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