# 📚 Kisakes Service

> Part of the **Spectrum Platform** microservices ecosystem

## Overview

Kisakes is the main business service in the Spectrum Platform, demonstrating production-ready microservice patterns.

## Features

- ✅ RESTful API endpoints
- ✅ PostgreSQL database integration
- ✅ Redis caching layer
- ✅ Liquibase migrations
- ✅ Health checks & metrics
- ✅ Structured JSON logging
- ✅ Circuit breaker ready
- ✅ Load balancer compatible

## Architecture

```
┌─────────────────────────────────────┐
│     Spectrum Load Balancer          │
│         (Port 8080)                  │
└──────────────┬──────────────────────┘
               │
        ┌──────┴──────┐
        ↓             ↓
┌──────────────┐ ┌──────────────┐
│ Kisakes #1   │ │ Kisakes #2   │
│ Port: 8081   │ │ Port: 8082   │
└──────┬───────┘ └──────┬────────┘
       │                │
       └────────┬───────┘
                ↓
    ┌───────────────────────┐
    │   Shared Resources    │
    ├───────────────────────┤
    │ PostgreSQL (kisakes)  │
    │ Redis Cache           │
    └───────────────────────┘
```

## API Endpoints

Access through Load Balancer: `http://localhost:8080/kisakes/...`

### Core Endpoints

```bash
# Your existing Kisakes endpoints here
GET    /kisakes/api/...
POST   /kisakes/api/...
PUT    /kisakes/api/...
DELETE /kisakes/api/...
```

### Health & Monitoring

```bash
# Health check
GET /kisakes/actuator/health

# Metrics (Prometheus format)
GET /kisakes/actuator/metrics
GET /kisakes/actuator/prometheus

# Info
GET /kisakes/actuator/info
```

## Configuration

### Environment Variables

In Spectrum Platform, these are automatically configured:

```yaml
# Database
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-db:5432/kisakes
SPRING_DATASOURCE_USERNAME: admin
SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}

# Redis
REDIS_HOST: redis-cache
REDIS_PORT: 6379

# Application
SERVER_PORT: 8081 or 8082 (per instance)
SPRING_PROFILES_ACTIVE: docker
```

### Custom Configuration

To modify Kisakes configuration:

1. Edit `services/kisakes/src/main/resources/application.yml`
2. Add environment-specific overrides in `application-docker.yml`
3. Rebuild: `docker-compose build kisakes-app-1 kisakes-app-2`

## Local Development

### Without Docker

```bash
cd services/kisakes

# Set environment variables
export SPRING_PROFILES_ACTIVE=dev
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/kisakes
export SPRING_DATASOURCE_USERNAME=admin
export SPRING_DATASOURCE_PASSWORD=admin123

# Run
./mvnw spring-boot:run
```

### With Docker (Standalone)

```bash
cd services/kisakes

# Build
docker build -t kisakes:local .

# Run
docker run -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/kisakes \
  -e REDIS_HOST=host.docker.internal \
  kisakes:local
```

## Testing

### Unit Tests

```bash
cd services/kisakes
./mvnw test
```

### Integration Tests

```bash
./mvnw verify -P integration-tests
```

### Via Load Balancer

```bash
# Through Spectrum Load Balancer
curl http://localhost:8080/kisakes/actuator/health

# Test load balancing
for i in {1..10}; do
  curl -s http://localhost:8080/kisakes/api/endpoint | jq '.instance'
done
```

## Monitoring

### Metrics

Kisakes exports Prometheus metrics at `/actuator/prometheus`:

```promql
# Request rate
rate(http_server_requests_seconds_count{application="kisakes"}[1m])

# Error rate
rate(http_server_requests_seconds_count{application="kisakes",status=~"5.."}[1m])

# Response time (95th percentile)
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{application="kisakes"}[5m]))
```

### Logs

Kisakes uses structured JSON logging for Loki:

```bash
# View logs in Grafana
{service="kisakes"}

# Filter by level
{service="kisakes"} |= "ERROR"

# Filter by trace ID
{service="kisakes"} | json | trace_id="abc123"
```

## Scaling

### Horizontal Scaling

```bash
# Scale to 4 instances
./scripts/scale.sh kisakes 4

# Or manually
docker-compose up -d --scale kisakes-app=4
```

Load balancer automatically discovers new instances!

### Vertical Scaling

Adjust JVM memory in Dockerfile:

```dockerfile
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Xms512m -Xmx2g"
```

## Database Migrations

Kisakes uses Liquibase for database versioning:

### Create Migration

```bash
# Add to src/main/resources/db/changelog/
# File: V002__add_new_table.sql
```

### Apply Migrations

```bash
# Automatic on startup
SPRING_LIQUIBASE_ENABLED=true

# Manual
./mvnw liquibase:update
```

## Troubleshooting

### Service Not Starting

```bash
# Check logs
docker-compose logs kisakes-app-1

# Check health
curl http://localhost:8080/kisakes/actuator/health

# Restart
docker-compose restart kisakes-app-1
```

### Database Connection Issues

```bash
# Verify PostgreSQL is running
docker exec postgres-db pg_isready

# Check connection string
docker-compose exec kisakes-app-1 env | grep DATASOURCE
```

### Redis Connection Issues

```bash
# Test Redis
docker exec redis-cache redis-cli ping

# Check connection
docker-compose exec kisakes-app-1 env | grep REDIS
```

## Performance Tuning

### JVM Tuning

```dockerfile
# In Dockerfile
ENV JAVA_OPTS="-XX:+UseG1GC \
               -XX:MaxGCPauseMillis=200 \
               -XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0"
```

### Connection Pooling

```yaml
# In application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

### Redis Caching

```java
@Cacheable(value = "yourCache", key = "#id")
public YourEntity findById(Long id) {
    // Expensive operation
}
```

## Security

### Production Checklist

- [ ] Change default database password
- [ ] Enable SSL for database connections
- [ ] Configure proper CORS settings
- [ ] Implement authentication/authorization
- [ ] Enable rate limiting
- [ ] Set up proper logging levels

### Secrets Management

```bash
# Use environment variables
export DB_PASSWORD=$(cat /run/secrets/db-password)

# Or use Docker secrets
docker secret create db_password /path/to/password.txt
```

## Contributing

When adding features to Kisakes:

1. Follow existing code structure
2. Add tests for new functionality
3. Update API documentation
4. Test with load balancer
5. Update this README

## Support

- Platform Issues: [Spectrum Platform Issues](../../issues)
- Service-Specific: [Kisakes Issues](../../issues?q=label:kisakes)
- Documentation: [Spectrum Docs](../../docs)

---

**Part of Spectrum Platform** | [Main Documentation](../../README.md) | [Architecture](../../docs/ARCHITECTURE.md)



[//]: # (# Kisakes: A Modern URL Shortener - A System Design Playground)

[//]: # ()
[//]: # (![Java]&#40;https://img.shields.io/badge/Java-21-blue.svg?style=for-the-badge&logo=openjdk&#41;)

[//]: # (![Spring Boot]&#40;https://img.shields.io/badge/Spring_Boot-3.3-green.svg?style=for-the-badge&logo=spring&#41;)

[//]: # (![PostgreSQL]&#40;https://img.shields.io/badge/PostgreSQL-16-blue.svg?style=for-the-badge&logo=postgresql&#41;)

[//]: # (![Redis]&#40;https://img.shields.io/badge/Redis-7-red.svg?style=for-the-badge&logo=redis&#41;)

[//]: # (![Docker]&#40;https://img.shields.io/badge/Docker-Compose-blue.svg?style=for-the-badge&logo=docker&#41;)

[//]: # (![Liquibase]&#40;https://img.shields.io/badge/Liquibase-4.2-orange.svg?style=for-the-badge&logo=liquibase&#41;)

[//]: # (![Grafana]&#40;https://img.shields.io/badge/Grafana_Loki-Logging-orange.svg?style=for-the-badge&logo=grafana&#41;)

[//]: # ()
[//]: # (Kisakes &#40;Turkish for "Shorten It"&#41; is a robust, scalable, and observable URL shortening service. But more importantly, it serves as a practical, hands-on laboratory for implementing and understanding advanced backend engineering and system design concepts.)

[//]: # ()
[//]: # (## The "Why": A System Design Playground)

[//]: # ()
[//]: # (The primary goal of this project is not just to build another URL shortener. It is to create a tangible learning environment to explore the challenges of building distributed, high-performance systems. Each feature and architectural decision is a deliberate step towards applying and mastering key system design principles.)

[//]: # ()
[//]: # (This repository documents the journey of evolving a simple application into a scalable and resilient service, tackling problems like database scaling, caching strategies, and system observability one concept at a time.)

[//]: # ()
[//]: # (## Implemented Concepts & Core Features)

[//]: # ()
[//]: # (This project currently implements the following concepts:)

[//]: # ()
[//]: # (#### 1. Scalability & Architecture)

[//]: # (*   **Stateless Architecture:** The `kisakes-app` is designed to be stateless, allowing for seamless horizontal scaling without requiring session affinity or complex state management.)

[//]: # (*   **Horizontal Scaling:** The `docker-compose.yml` is configured to run two instances of the `kisakes-app` to simulate a horizontally scaled environment, ready to be placed behind a load balancer.)

[//]: # (*   **Containerization &#40;Docker&#41;:** The entire application and its dependencies &#40;PostgreSQL, Redis, Grafana, etc.&#41; are containerized, ensuring consistency across development and production environments.)

[//]: # (*   **Multi-Stage Dockerfile:** Employs a multi-stage build to create a lean, production-ready final image, separating the build environment from the runtime environment.)

[//]: # ()
[//]: # (#### 2. Database & Performance)

[//]: # (*   **PostgreSQL:** The primary relational database for storing URL mappings.)

[//]: # (*   **Connection Pooling &#40;HikariCP&#41;:** Leverages the high-performance HikariCP connection pool, configured automatically by Spring Boot, to efficiently manage database connections under load.)

[//]: # (*   **Time-Based Partitioning:** The `urls` table is partitioned by `created_at` to improve query performance on large datasets and simplify data management &#40;e.g., archiving old data&#41;.)

[//]: # (*   **Indexing Strategies:** Utilizes B-tree indexes on `short_code` and `created_at` to ensure fast lookups and efficient partitioning.)

[//]: # (*   **Lookup Table Pattern:** A dedicated, non-partitioned `short_code_lookup` table is used to enforce global uniqueness for `short_code`, overcoming a limitation of unique constraints on partitioned tables in PostgreSQL.)

[//]: # (*   **Database Schema Management &#40;Liquibase&#41;:** Manages all database schema evolutions in a version-controlled, environment-agnostic way, ensuring reliability and repeatability.)

[//]: # ()
[//]: # (#### 3. Caching)

[//]: # (*   **Redis:** Used as a distributed cache to reduce database load and decrease latency for frequently accessed URLs.)

[//]: # (*   **Cache-Aside Pattern:** The application logic first checks Redis for a given short code. If it's a cache miss, it fetches the data from PostgreSQL and populates the cache for subsequent requests.)

[//]: # (*   **Spring Cache Abstraction &#40;`@Cacheable`&#41;:** Refactored the manual cache-aside logic to use Spring's declarative caching, resulting in cleaner, more maintainable code.)

[//]: # ()
[//]: # (#### 4. Observability & Monitoring &#40;The "LPG" Stack&#41;)

[//]: # (*   **Centralized & Distributed Logging:** The entire system is configured for centralized logging, allowing logs from multiple application instances to be aggregated and searched in one place.)

[//]: # (*   **Grafana Loki:** The core log aggregation system, chosen for its efficiency and deep integration with Grafana.)

[//]: # (*   **Promtail:** The agent responsible for discovering containers, collecting their logs, adding metadata labels &#40;like `app_name`, `container`&#41;, and shipping them to Loki.)

[//]: # (*   **Structured JSON Logging:** The Spring Boot application is configured &#40;via `logstash-logback-encoder`&#41; to output logs in a structured JSON format, enabling powerful filtering and analysis.)

[//]: # (*   **Distributed Tracing Foundation:** Using `Micrometer Tracing`, every incoming request is automatically assigned a `Trace-ID`, which is included in all logs. This allows for filtering all logs related to a single request across all services.)

[//]: # ()
[//]: # (#### 5. System Design Patterns & API)

[//]: # (*   **Unique ID Generation:** Employs a random, collision-resistant Base62-like string generation for short codes, validated against the lookup table for global uniqueness.)

[//]: # (*   **Graceful Error Handling:** Implements custom exceptions &#40;e.g., `UrlNotFoundException`&#41; and leverages Spring's `@ControllerAdvice` for clean and consistent API error responses.)

[//]: # ()
[//]: # (## Tech Stack & Tools)

[//]: # ()
[//]: # (*   **Language/Framework:** Java 21, Spring Boot 3.3)

[//]: # (*   **Database:** PostgreSQL 16)

[//]: # (*   **Caching:** Redis 7)

[//]: # (*   **DB Migration:** Liquibase)

[//]: # (*   **Containerization:** Docker, Docker Compose)

[//]: # (*   **Observability:** Grafana, Loki, Promtail, Micrometer)

[//]: # (*   **Build Tool:** Maven)

[//]: # ()
[//]: # (## Getting Started)

[//]: # ()
[//]: # (### Prerequisites)

[//]: # (*   Git)

[//]: # (*   Docker & Docker Compose)

[//]: # (*   Java 21 &#40;for local development outside Docker&#41;)

[//]: # (*   Maven &#40;for local development outside Docker&#41;)

[//]: # ()
[//]: # (### Running the System)

[//]: # (1.  Clone the repository:)

[//]: # (    ```bash)

[//]: # (    git clone <your-repo-url>)

[//]: # (    cd kisakes)

[//]: # (    ```)

[//]: # (2.  Build and run the entire stack using Docker Compose:)

[//]: # (    ```bash)

[//]: # (    docker compose up --build)

[//]: # (    ```)

[//]: # (    This will build the Spring Boot application, create Docker images, and start all services.)

[//]: # ()
[//]: # (### Accessing Services)

[//]: # (*   **Application Instance 1:** `http://localhost:8081`)

[//]: # (*   **Application Instance 2:** `http://localhost:8082`)

[//]: # (*   **Grafana &#40;for Logs&#41;:** `http://localhost:3000` &#40;user: `admin`, pass: `admin`&#41;)

[//]: # (*   **PostgreSQL:** `localhost:5432`)

[//]: # (*   **Redis:** `localhost:6379`)

[//]: # ()
[//]: # (## Project Roadmap &#40;TODO&#41;)

[//]: # ()
[//]: # (The following concepts are planned for future implementation:)

[//]: # ()
[//]: # (-   [ ] **Load Balancing:**)

[//]: # (    -   [ ] Implement a basic, custom Round-Robin Load Balancer in Java/Spring Boot to understand the core principles of reverse proxies and service registries.)

[//]: # (    -   [ ] Integrate a production-grade load balancer like Nginx.)

[//]: # (-   [ ] **Scalability:**)

[//]: # (    -   [ ] **Sharding &#40;Hash-Based&#41;:** Design and implement a sharding strategy for the `urls` table to distribute data across multiple database instances.)

[//]: # (-   [ ] **Performance:**)

[//]: # (    -   [ ] **Asynchronous Operations:** Introduce asynchronous event processing &#40;e.g., with `@Async` or a message queue like RabbitMQ/Kafka&#41; for tasks like analytics tracking.)

[//]: # (    -   [ ] **Batch Processing:** Implement batch inserts for high-throughput scenarios.)

[//]: # (-   [ ] **Reliability:**)

[//]: # (    -   [ ] **Rate Limiting:** Configure and apply fine-grained rate limiting using Resilience4j.)

[//]: # (    -   [ ] **Circuit Breaker Pattern:** Implement circuit breakers to prevent cascading failures when communicating with external or internal services.)

[//]: # (    -   [ ] **Graceful Degradation:** Implement strategies for non-critical features to fail gracefully without impacting core functionality.)

[//]: # (-   [ ] **Caching:**)

[//]: # (    -   [ ] Fine-tune caching with explicit **Time-To-Live &#40;TTL&#41;** configurations.)

[//]: # (    -   [ ] Implement a **cache invalidation** strategy for scenarios where URLs are updated or deleted.)

[//]: # (-   [ ] **API & Security:**)

[//]: # (    -   [ ] Introduce API versioning.)

[//]: # (    -   [ ] Add user authentication &#40;e.g., with JWT&#41; to manage user-specific URLs.)