# HSurveys Gateway Service

This is the API Gateway service for the HSurveys microservices architecture, built using Spring Cloud Gateway.

## Features

- **JWT Authentication**: Validates JWT tokens and extracts user context
- **Route Management**: Routes requests to appropriate microservices
- **Circuit Breaker**: Implements resilience patterns using Resilience4j
- **Rate Limiting**: Redis-based rate limiting with configurable policies
- **CORS Support**: Cross-origin resource sharing configuration
- **Request Logging**: Comprehensive request/response logging with request IDs
- **Health Checks**: Actuator endpoints for monitoring
- **Fallback Handling**: Graceful degradation when services are unavailable

## Architecture

The gateway routes requests to three main services:

- **User Service**: `/api/users/**`, `/api/auth/**`, `/api/roles/**`, `/api/permissions/**`
- **Organization Service**: `/api/organizations/**`, `/api/departments/**`, `/api/teams/**`
- **Survey Service**: `/api/surveys/**`, `/api/questions/**`, `/api/options/**`

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `JWT_SECRET` | JWT signing secret | Base64 encoded default |
| `JWT_EXPIRATION` | JWT expiration time (ms) | 900000 (15 minutes) |
| `REDIS_HOST` | Redis server host | localhost |
| `REDIS_PORT` | Redis server port | 6379 |
| `REDIS_PASSWORD` | Redis password | (empty) |

### Rate Limiting

- **Replenish Rate**: 10 requests per second
- **Burst Capacity**: 20 requests
- **Requested Tokens**: 1 per request

### Circuit Breaker Settings

- **Sliding Window Size**: 10 calls
- **Minimum Calls**: 5 calls before circuit breaker activates
- **Failure Rate Threshold**: 50%
- **Wait Duration**: 5 seconds in open state

## Public Endpoints

The following endpoints bypass JWT authentication:

- `/api/auth/login`
- `/api/auth/register`
- `/api/auth/refresh`
- `/api/organizations/register`
- `/actuator/**`

## User Context Headers

When a valid JWT token is provided, the gateway adds the following headers to downstream requests:

- `X-User-Id`: User's unique identifier
- `X-Username`: User's username
- `X-User-Name`: User's display name
- `X-Organization-Id`: User's organization ID
- `X-Department-Id`: User's department ID
- `X-Team-Id`: User's team ID
- `X-Authorities`: User's authorities (comma-separated)
- `X-Roles`: User's roles (comma-separated)
- `X-Authenticated`: Always "true"

## Monitoring

### Actuator Endpoints

- `/actuator/health`: Health check with circuit breaker status
- `/actuator/info`: Application information
- `/actuator/metrics`: Application metrics
- `/actuator/prometheus`: Prometheus metrics
- `/actuator/circuitbreakers`: Circuit breaker status
- `/actuator/gateway`: Gateway route information

### Logging

The gateway provides structured logging with:
- Request IDs for request tracing
- Request/response timing
- Error logging with stack traces
- Debug logging for JWT processing

## Building and Running

### Prerequisites

- Java 17
- Maven 3.8+
- Redis (for rate limiting)

### Local Development

```bash
# Build the project
mvn clean package

# Run with default configuration
java -jar target/gateway-0.0.1-SNAPSHOT.jar

# Run with custom configuration
java -jar target/gateway-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=dev \
  --jwt.secret=your-secret \
  --redis.host=localhost
```

### Docker

```bash
# Build Docker image
docker build -t hsurveys-gateway .

# Run with Docker Compose
docker-compose up gateway
```

### Docker Compose Example

```yaml
version: '3.8'
services:
  gateway:
    build: .
    ports:
      - "8080:8080"
    environment:
      - JWT_SECRET=your-jwt-secret
      - REDIS_HOST=redis
    depends_on:
      - redis
      - user-service
      - organization-service
      - survey-service
  
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
  
  user-service:
    image: hsurveys-user-service
    ports:
      - "8081:8080"
  
  organization-service:
    image: hsurveys-organization-service
    ports:
      - "8082:8080"
  
  survey-service:
    image: hsurveys-survey-service
    ports:
      - "8083:8080"
```

## Testing

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Authentication Test

```bash
# Public endpoint (no auth required)
curl http://localhost:8080/api/auth/login

# Protected endpoint (auth required)
curl -H "Authorization: Bearer your-jwt-token" \
     http://localhost:8080/api/users/profile
```

### Rate Limiting Test

```bash
# Test rate limiting
for i in {1..25}; do
  curl -H "Authorization: Bearer your-jwt-token" \
       http://localhost:8080/api/users/profile
  echo "Request $i"
done
```

## Troubleshooting

### Common Issues

1. **Circuit Breaker Open**: Check if downstream services are running
2. **Rate Limiting**: Monitor Redis connection and configuration
3. **JWT Validation**: Verify JWT secret and token format
4. **CORS Issues**: Check allowed origins in configuration

### Debug Mode

Enable debug logging by setting:

```yaml
logging:
  level:
    com.hsurveys.gateway: DEBUG
    org.springframework.cloud.gateway: DEBUG
```

## Security Considerations

- JWT tokens are validated for expiration and signature
- Rate limiting prevents abuse
- CORS is configured for specific origins
- Circuit breakers prevent cascading failures
- All sensitive configuration uses environment variables

## Performance

- Reactive programming with WebFlux
- Non-blocking I/O
- Connection pooling for downstream services
- Redis caching for rate limiting
- Efficient JWT validation

## Contributing

1. Follow the existing code style
2. Add tests for new features
3. Update documentation
4. Ensure all endpoints are properly configured
5. Test with the full microservices stack 