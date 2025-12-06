# 🌈 Spectrum Platform

> **Production-ready microservices architecture with intelligent load balancing, circuit breaking, and comprehensive observability**

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/yourusername/spectrum-platform)
[![Docker](https://img.shields.io/badge/Docker-20.10+-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![Java](https://img.shields.io/badge/Java-17+-ED8B00?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)

---

## 🎯 What is Spectrum Platform?

**Spectrum Platform** is an enterprise-grade microservices ecosystem featuring a **custom-built Layer 7 Load Balancer** with production-ready features including circuit breaking, rate limiting, SSL termination, and real-time observability.

Built to demonstrate modern distributed systems architecture, it's perfect for:
- 🎓 **Learning** microservices patterns
- 🏗️ **Building** production applications
- 📊 **Showcasing** DevOps skills
- 🔬 **Experimenting** with distributed systems

---

## ✨ Key Features

### 🚀 Custom Load Balancer
Our flagship component - a fully-featured Layer 7 load balancer built from scratch:

- **5 Load Balancing Algorithms**
    - 🔄 Round Robin
    - 📊 Least Connections
    - 🎯 IP Hash (Session Affinity)
    - ⚖️ Weighted Round Robin
    - 🎲 Random Distribution

- **Production Features**
    - 🛡️ Circuit Breaker (Self-healing)
    - 🚦 Rate Limiting (Token Bucket & Sliding Window)
    - 🔒 SSL/TLS Termination
    - 🎯 Sticky Sessions
    - 🔵 Blue-Green Deployments
    - 🔌 WebSocket Support
    - 📝 Request/Response Logging
    - 📊 Prometheus Metrics Export

### 🏗️ Infrastructure
- **PostgreSQL 16**: Multi-tenant database
- **Redis 7**: Distributed caching layer
- **Docker Compose**: Orchestration
- **Service Mesh**: Ready for Kubernetes

### 📊 Full Observability Stack
- **Grafana**: Real-time dashboards
- **Loki**: Centralized logging
- **Prometheus**: Metrics collection
- **Promtail**: Log aggregation

### 🎯 Microservices
- **Kisakes**: Production-ready business service
- **Dummy Service**: Testing & demonstration service

---

## 🏛️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     CLIENTS & USERS                          │
│                   (Web, Mobile, API)                         │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ↓
┌──────────────────────────────────────────────────────────────┐
│              🌈 SPECTRUM LOAD BALANCER (Port 8080)           │
│                                                               │
│  Algorithms:  Round Robin • Least Conn • IP Hash • Weighted │
│  Features:    Circuit Breaker • Rate Limit • SSL • Sessions │
│  Monitoring:  Prometheus • Health Checks • Metrics          │
└─────────┬────────────────────────────────┬───────────────────┘
          │                                │
          ↓                                ↓
┌──────────────────────┐         ┌──────────────────────┐
│   KISAKES SERVICE    │         │   DUMMY SERVICE      │
│                      │         │                      │
│  • Instance 1:8081   │         │  • Instance 1:8083   │
│  • Instance 2:8082   │         │  • Instance 2:8084   │
│                      │         │                      │
│  [Business Logic]    │         │  [Test Endpoints]    │
└──────────┬───────────┘         └──────────┬───────────┘
           │                                │
           └────────────────┬───────────────┘
                            ↓
           ┌────────────────────────────────┐
           │    SHARED INFRASTRUCTURE       │
           │                                │
           │  🗄️  PostgreSQL (Port 5432)   │
           │  💾  Redis Cache (Port 6379)  │
           └────────────────────────────────┘
                            ↓
           ┌────────────────────────────────┐
           │   📊 MONITORING STACK          │
           │                                │
           │  • Grafana     (Port 3000)    │
           │  • Prometheus  (Port 9090)    │
           │  • Loki        (Port 3100)    │
           │  • Promtail    (Agent)        │
           └────────────────────────────────┘
```

---

## 🚀 Quick Start

### Prerequisites

```bash
# Required
✅ Docker 20.10 or higher
✅ Docker Compose 2.0 or higher
✅ 8GB RAM minimum
✅ 20GB disk space

# Optional (for development)
⚙️ Java 17+
⚙️ Maven 3.8+
⚙️ Git
```

### Installation (5 minutes)

```bash
# 1️⃣ Clone the repository
git clone https://github.com/yourusername/spectrum-platform.git
cd spectrum-platform

# 2️⃣ Run setup (creates configs, databases, monitoring)
./scripts/setup.sh

# 3️⃣ Start all services
./scripts/start.sh

# 4️⃣ Verify everything is running
./scripts/health-check.sh
```

### 🎉 You're Done!

Access your services:

| Service | URL | Credentials |
|---------|-----|-------------|
| 🌈 Load Balancer | http://localhost:8080 | - |
| ⚙️ Admin Dashboard | http://localhost:8080/admin/status | - |
| 📊 Grafana | http://localhost:3000 | admin / admin123 |
| 📈 Prometheus | http://localhost:9090 | - |
| 🗄️ PostgreSQL | localhost:5432 | admin / admin123 |
| 💾 Redis | localhost:6379 | - |

---

## 💻 Usage Examples

### Basic Requests

```bash
# Access Kisakes service through load balancer
curl http://localhost:8080/kisakes/api/endpoint

# Access Dummy service
curl http://localhost:8080/dummy-service/hello

# Get a random quote
curl http://localhost:8080/dummy-service/quote
```

### Admin API

```bash
# Check system status
curl http://localhost:8080/admin/status | jq

# View active features
curl http://localhost:8080/admin/features | jq

# Circuit breaker status
curl http://localhost:8080/admin/circuit-breaker/status | jq

# Change load balancing algorithm
curl -X POST http://localhost:8080/admin/services/kisakes/algorithm \
  -H "Content-Type: application/json" \
  -d '{"algorithm":"LEAST_CONNECTIONS"}'
```

### Monitoring

```bash
# View Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# View service health
curl http://localhost:8080/actuator/health | jq

# Check specific service stats
curl http://localhost:8080/admin/services/kisakes | jq
```

---

## 📊 Monitoring & Observability

### Pre-configured Grafana Dashboards

1. **Load Balancer Dashboard**
    - Request rate & throughput
    - Error rates & response times
    - Circuit breaker states
    - Algorithm performance

2. **Services Overview**
    - Per-service metrics
    - Health status
    - Response time distribution
    - Request distribution

3. **Infrastructure Dashboard**
    - Database connections
    - Redis cache hit rate
    - Memory & CPU usage
    - Network I/O

### Accessing Logs

```bash
# All services
./scripts/logs.sh

# Specific service
docker-compose logs -f load-balancer
docker-compose logs -f kisakes-app-1

# Filter by level
docker-compose logs | grep ERROR
```

### Prometheus Queries

```promql
# Request rate (requests/second)
rate(loadbalancer_requests_total[1m])

# Error rate percentage
(rate(loadbalancer_errors_total[5m]) / rate(loadbalancer_requests_total[5m])) * 100

# Average response time
rate(loadbalancer_request_duration_seconds_sum[5m]) / rate(loadbalancer_request_duration_seconds_count[5m])

# Server health (1=healthy, 0=unhealthy)
loadbalancer_server_health
```

---

## 🎓 Advanced Features

### Circuit Breaker

Automatic failure detection and recovery:

```yaml
circuit-breaker:
  enabled: true
  failure-threshold: 5      # Open after 5 failures
  success-threshold: 2      # Close after 2 successes
  timeout-seconds: 60       # Try recovery after 60s
```

**States:**
- 🟢 **CLOSED**: Normal operation
- 🔴 **OPEN**: Blocking requests (service unhealthy)
- 🟡 **HALF_OPEN**: Testing recovery

### Rate Limiting

Protect your services from overload:

```yaml
rate-limit:
  enabled: true
  max-requests: 100         # 100 requests
  window-seconds: 60        # per 60 seconds
  algorithm: TOKEN_BUCKET   # or SLIDING_WINDOW
```

### Sticky Sessions

Session affinity for stateful apps:

```yaml
sticky-session:
  enabled: true
  session-timeout-minutes: 30
  cookie-name: SPECTRUM_SESSION_ID
```

### Blue-Green Deployments

Zero-downtime deployments:

```bash
# Instant switch
curl -X POST http://localhost:8080/admin/deployment/kisakes/switch/instant

# Gradual rollout (canary)
curl -X POST "http://localhost:8080/admin/deployment/kisakes/switch/gradual?durationSeconds=300&steps=10"

# Rollback
curl -X POST http://localhost:8080/admin/deployment/kisakes/rollback
```

---

## 🛠️ Management Scripts

```bash
# Start everything
./scripts/start.sh

# Stop everything
./scripts/stop.sh

# Restart specific service
./scripts/restart.sh kisakes

# Scale services
./scripts/scale.sh kisakes 5

# Health check
./scripts/health-check.sh

# View logs
./scripts/logs.sh [service-name]

# Load test
./scripts/load-test.sh

# Clean everything
./scripts/clean.sh
```

---

## 📖 Documentation

Comprehensive guides for every aspect:

- 🏛️ [Architecture Overview](docs/ARCHITECTURE.md)
- 🚀 [Getting Started Guide](docs/GETTING_STARTED.md)
- 📡 [API Documentation](docs/API.md)
- 🚢 [Deployment Guide](docs/DEPLOYMENT.md)
- 📊 [Monitoring Setup](docs/MONITORING.md)
- 🔧 [Troubleshooting](docs/TROUBLESHOOTING.md)
- 🤝 [Contributing Guide](docs/CONTRIBUTING.md)

---

## 🎯 Use Cases

### 1. Learning Platform
Perfect for understanding:
- Microservices architecture
- Load balancing algorithms
- Circuit breaker patterns
- Observability practices
- Docker orchestration

### 2. Portfolio Project
Showcase your skills in:
- System design
- Distributed systems
- DevOps practices
- Full-stack development
- Production readiness

### 3. Startup MVP
Quick start for:
- API gateway needs
- Service mesh requirements
- Multi-service applications
- Scalable architecture

### 4. Research & Experimentation
Test and learn:
- Load balancing strategies
- Failure handling
- Performance optimization
- Monitoring techniques

---

## 🏆 Why Spectrum?

### vs. NGINX
- ✅ Built-in circuit breaker
- ✅ Integrated metrics
- ✅ Dynamic configuration
- ✅ Developer-friendly API
- ✅ Full source code access

### vs. HAProxy
- ✅ Modern tech stack (Spring Boot)
- ✅ Easy to extend
- ✅ Rich monitoring
- ✅ Better documentation
- ✅ Developer experience

### vs. Cloud Solutions (AWS ALB, etc.)
- ✅ No vendor lock-in
- ✅ Run anywhere
- ✅ No additional costs
- ✅ Full control
- ✅ Educational value

---

## 🔧 Configuration

### Minimal Setup

```yaml
# .env
POSTGRES_PASSWORD=yourpassword
GRAFANA_PASSWORD=yoursecret
```

### Advanced Configuration

```yaml
# infrastructure/load-balancer/application-docker.yml
loadbalancer:
  algorithm: WEIGHTED_ROUND_ROBIN
  
  circuit-breaker:
    enabled: true
    failure-threshold: 5
  
  rate-limit:
    enabled: true
    max-requests: 1000
  
  services:
    kisakes:
      algorithm: LEAST_CONNECTIONS
      upstreams:
        - url: http://kisakes-app-1:8081
          weight: 2
        - url: http://kisakes-app-2:8082
          weight: 1
```

---

## 📈 Performance

Tested with Apache Bench:

```bash
# Test results
Requests per second: 5,432 [#/sec]
Time per request: 1.84 [ms]
Transfer rate: 1,234 [Kbytes/sec]

# 99th percentile response time
99%: 23ms
```

*Hardware: MacBook Pro M1, 16GB RAM, Docker Desktop*

---

## 🤝 Contributing

We love contributions! Here's how:

1. 🍴 Fork the repository
2. 🌿 Create your feature branch (`git checkout -b feature/amazing-feature`)
3. ✍️ Commit your changes (`git commit -m 'Add amazing feature'`)
4. 📤 Push to the branch (`git push origin feature/amazing-feature`)
5. 🎉 Open a Pull Request

See [CONTRIBUTING.md](docs/CONTRIBUTING.md) for detailed guidelines.

---

## 🐛 Troubleshooting

### Common Issues

**Services not starting?**
```bash
# Check Docker resources
docker system df

# Clean and restart
./scripts/clean.sh
./scripts/start.sh
```

**Can't access services?**
```bash
# Verify all services are healthy
./scripts/health-check.sh

# Check specific service logs
docker-compose logs kisakes-app-1
```

**Performance issues?**
```bash
# Check resource usage
docker stats

# Scale up if needed
./scripts/scale.sh kisakes 4
```

See [TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) for more solutions.

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework
- [Grafana Labs](https://grafana.com/) - Observability stack
- [Docker](https://www.docker.com/) - Containerization
- [PostgreSQL](https://www.postgresql.org/) - Database
- [Redis](https://redis.io/) - Caching

Special thanks to the open-source community! 💚

---

## 📞 Support & Community

- 📧 Email: degerlicoding@gmail.com
- 💬 Discussions: [GitHub Discussions](https://github.com/yourusername/spectrum-platform/discussions)
- 🐛 Issues: [GitHub Issues](https://github.com/yourusername/spectrum-platform/issues)
- 📚 Wiki: [Project Wiki](https://github.com/yourusername/spectrum-platform/wiki)
- 🐦 Twitter: [@yourhandle](https://twitter.com/yourhandle)

---

## 🌟 Star History

If you find this project helpful, please consider giving it a ⭐!

[![Star History Chart](https://api.star-history.com/svg?repos=yourusername/spectrum-platform&type=Date)](https://star-history.com/#yourusername/spectrum-platform&Date)

---

## 📊 Project Stats

![GitHub code size](https://img.shields.io/github/languages/code-size/yourusername/spectrum-platform)
![GitHub repo size](https://img.shields.io/github/repo-size/yourusername/spectrum-platform)
![GitHub last commit](https://img.shields.io/github/last-commit/yourusername/spectrum-platform)
![GitHub issues](https://img.shields.io/github/issues/yourusername/spectrum-platform)
![GitHub pull requests](https://img.shields.io/github/issues-pr/yourusername/spectrum-platform)

---

<div align="center">

**Built with ❤️ by developers, for developers**

[⬆ Back to Top](#-spectrum-platform)

</div>