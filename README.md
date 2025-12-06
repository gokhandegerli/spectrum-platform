# 🌈 Spectrum Platform

> **Production-ready microservices architecture with intelligent load balancing, circuit breaking, and comprehensive observability**

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/yourusername/spectrum-platform)
[![Docker](https://img.shields.io/badge/Docker-20.10+-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
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

## 🚀 Quick Start

### Prerequisites

```bash
# Required
✅ Docker 20.10 or higher
✅ Docker Compose 2.0 or higher
✅ 8GB RAM minimum
✅ 20GB disk space

# Optional (for development)
⚙️ Java 21+
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

---

## 📦 Tech Stack

### Core Technologies
- **Language/Runtime:** Java 21 (Eclipse Temurin)
- **Framework:** Spring Boot 3.3.0
- **Build Tool:** Maven 3.8+

### Infrastructure
- **Database:** PostgreSQL 16
- **Cache:** Redis 7
- **DB Migrations:** Liquibase 4.28
- **Containerization:** Docker & Docker Compose

### Observability Stack
- **Metrics:** Prometheus + Micrometer
- **Dashboards:** Grafana
- **Logging:** Loki + Promtail
- **Tracing:** Micrometer Tracing (Brave)

### Security & Resilience
- **Circuit Breaker:** Custom Implementation
- **Rate Limiting:** Token Bucket & Sliding Window
- **Connection Pooling:** HikariCP

---

## ⚠️ Security Warning

**NEVER use default passwords in production!**

Before deploying to production, change these in `.env` file:
- `POSTGRES_PASSWORD` (default: admin123)
- `GRAFANA_PASSWORD` (default: admin123)
- `REDIS_PASSWORD` (default: empty - SET THIS!)

```bash
# Generate secure passwords
openssl rand -base64 32
```

---

## 🏗️ Project Structure

```
spectrum-platform/
├── infrastructure/
│   ├── load-balancer/          # Custom L7 Load Balancer
│   ├── databases/postgres/     # DB init scripts
│   └── monitoring/             # Grafana, Loki, Prometheus configs
├── services/
│   ├── kisakes/                # URL Shortener service
│   └── dummy-service/          # Test/Demo service
├── scripts/                    # Operational scripts
│   ├── setup.sh
│   ├── start.sh
│   ├── health-check.sh
│   └── build-all.sh
├── docker-compose.yml          # Service orchestration
├── pom.xml                     # Parent POM
└── README.md
```

---

## 🎉 You're Done!

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

For detailed documentation, see the `/docs` directory or visit our [Wiki](https://github.com/yourusername/spectrum-platform/wiki).