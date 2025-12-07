# 🌈 Spectrum Platform - Kapsamlı Teknik Dokümantasyon

> **Production-Ready Mikroservis Mimarisi ile Özel Layer 7 Load Balancer**

## 📑 İçindekiler

1. [Proje Genel Bakış](#proje-genel-bakış)
2. [Mimari Yapı](#mimari-yapı)
3. [Dosya ve Klasör Yapısı](#dosya-ve-klasör-yapısı)
4. [Konfigürasyon Dosyaları](#konfigürasyon-dosyaları)
5. [Load Balancer Detaylı Analiz](#load-balancer-detaylı-analiz)
6. [Servisler](#servisler)
7. [Test ve Çalıştırma](#test-ve-çalıştırma)
8. [Değişiklik Senaryoları](#değişiklik-senaryoları)
9. [Troubleshooting](#troubleshooting)

---

## 🎯 Proje Genel Bakış

**Spectrum Platform**, modern mikroservis mimarisinin tüm best practice'lerini içeren, production-ready bir ekosistemdir.

### Temel Özellikler

- ✅ **Özel Layer 7 Load Balancer** - Circuit breaker, rate limiting, multiple stratejiler
- ✅ **Mikroservis Architecture** - İki örnek servis (Kisakes, Dummy Service)
- ✅ **Full Observability Stack** - Prometheus, Grafana, Loki, Promtail
- ✅ **Database Layer** - PostgreSQL 16 + Redis 7 caching
- ✅ **Containerized** - Docker Compose ile tam orkestrasyon
- ✅ **Production Features** - Health checks, metrics, tracing, structured logging

### Teknoloji Stack

```
Language:       Java 21 (Eclipse Temurin)
Framework:      Spring Boot 3.3.0
Build:          Maven 3.8+
Database:       PostgreSQL 16
Cache:          Redis 7
Migration:      Liquibase 4.28
Container:      Docker & Docker Compose
Monitoring:     Prometheus + Grafana
Logging:        Loki + Promtail
Tracing:        Micrometer Tracing (Brave)
```

---

## 🏗️ Mimari Yapı

### Genel Akış

```
                    ┌─────────────────────────────────┐
                    │       Client/User               │
                    └─────────────┬───────────────────┘
                                  │
                    ┌─────────────▼───────────────────┐
                    │   Load Balancer (Port 8080)     │
                    │   - Circuit Breaker             │
                    │   - Rate Limiting               │
                    │   - Health Checking             │
                    │   - Multiple Strategies         │
                    │   - Sticky Sessions             │
                    └──┬─────────────────────┬────────┘
                       │                     │
       ┌───────────────▼──────┐   ┌────────▼──────────────┐
       │   Kisakes Service    │   │   Dummy Service       │
       │   - Instance 1:8081  │   │   - Instance 1:8083   │
       │   - Instance 2:8082  │   │   - Instance 2:8084   │
       └──────┬───────────────┘   └───────────────────────┘
              │
    ┌─────────▼───────────┐
    │  Data Layer         │
    │  - PostgreSQL       │
    │  - Redis Cache      │
    └─────────────────────┘

    ┌─────────────────────────────────────────┐
    │      Observability Stack                │
    │  - Prometheus (metrics)                 │
    │  - Grafana (visualization)              │
    │  - Loki (log aggregation)               │
    │  - Promtail (log collection)            │
    └─────────────────────────────────────────┘
```

### Port Mapping

| Service          | Internal Port | External Port | Açıklama                    |
|------------------|---------------|---------------|-----------------------------|
| Load Balancer    | 8080          | 8080          | Ana giriş noktası          |
| Kisakes App 1    | 8081          | -             | Sadece internal            |
| Kisakes App 2    | 8082          | -             | Sadece internal            |
| Dummy Service 1  | 8083          | -             | Sadece internal            |
| Dummy Service 2  | 8084          | -             | Sadece internal            |
| PostgreSQL       | 5432          | 5432          | Database access            |
| Redis            | 6379          | 6379          | Cache access               |
| Prometheus       | 9090          | 9090          | Metrics UI                 |
| Grafana          | 3000          | 3000          | Dashboard UI               |
| Loki             | 3100          | 3100          | Log API                    |

---

## 📂 Dosya ve Klasör Yapısı

### Root Level

```
spectrum-platform/
├── docker-compose.yml          # Ana orkestrasyon dosyası
├── pom.xml                     # Parent Maven POM
├── settings.xml                # Maven settings
├── qodana.yaml                 # Code quality config
├── README.md                   # Proje dokümantasyonu
├── services/                   # Mikroservisler
├── infrastructure/             # Altyapı bileşenleri
├── scripts/                    # Otomasyon scriptleri
├── docs/                       # Dokümantasyon
├── examples/                   # Örnek kullanımlar
└── logs/                       # Log dosyaları
```

---

## 📋 Konfigürasyon Dosyaları

### 1. docker-compose.yml

**Amaç**: Tüm servisleri orkestre eder, network ve volume yönetimini sağlar.

**Kullanım Alanları**:
- Servis tanımları ve bağımlılıkları
- Environment variable'lar
- Port mapping
- Health check tanımları
- Volume mount'ları

**Değiştirmen Gereken Durumlar**:
1. **Yeni servis eklerken**:
```yaml
  my-new-service:
    build:
      context: .
      dockerfile: services/my-new-service/Dockerfile
    container_name: my-new-service
    networks:
      - microservices-net
    expose:
      - "8085"
    environment:
      - SERVER_PORT=8085
```

2. **Port değiştirirken**:
```yaml
ports:
  - "9090:8080"  # External:Internal
```

3. **Environment variable eklerken**:
```yaml
environment:
  - NEW_CONFIG=value
  - API_KEY=${API_KEY:-default}
```

**Test Etmen Gerekenler**:
- `docker-compose config` - Syntax kontrolü
- `docker-compose up` - Servis başlatma
- `./scripts/health-check.sh` - Health check

---

### 2. Load Balancer Configuration

**Dosya**: `infrastructure/load-balancer/src/main/resources/application.yml`

**Kritik Konfigürasyonlar**:

#### a) Load Balancing Algoritması
```yaml
loadbalancer:
  algorithm: ROUND_ROBIN  # Değiştirilebilir: LEAST_CONNECTIONS, WEIGHTED_ROUND_ROBIN, IP_HASH, RANDOM
```

**Ne Zaman Değiştirirsin**:
- ROUND_ROBIN: Eşit dağılım istediğinde (default)
- LEAST_CONNECTIONS: Servislerde farklı işlem süreleri varsa
- WEIGHTED_ROUND_ROBIN: Farklı kapasiteli sunucular varsa
- IP_HASH: Session persistence istediğinde
- RANDOM: Basit dağılım yeterli ise

**Test**:
```bash
# Load test ile dağılımı kontrol et
./scripts/load-test.sh http://localhost:8080/kisakes/actuator/health 100 10

# Admin API'den istatistikleri gör
curl http://localhost:8080/admin/status | jq
```

#### b) Health Check Configuration
```yaml
loadbalancer:
  health-check-enabled: true
  health-check-interval: 5000    # 5 saniye
  health-check-timeout: 2000     # 2 saniye
```

**Değiştirme Senaryoları**:
- Yavaş servisler: timeout'u artır
- Hızlı fail-over: interval'i azalt
- Devre dışı bırakma: enabled: false

**Test**:
```bash
# Bir servisi kapat ve load balancer'ın tepkisini izle
docker stop kisakes-app-2
./scripts/health-check.sh
# 5 saniye sonra load balancer bu instance'ı devre dışı bırakır
```

#### c) Circuit Breaker
```yaml
loadbalancer:
  circuit-breaker:
    enabled: true
    failure-threshold: 5           # 5 başarısız istek sonrası aç
    success-threshold: 2           # 2 başarılı istek sonrası kapat
    timeout-seconds: 60            # Açık kalma süresi
    reset-timeout-seconds: 300     # Sıfırlama süresi
```

**Kullanım Senaryoları**:
- Bir servis sürekli fail ediyorsa otomatik devre dışı bırakır
- Cascade failure'ı önler
- Servis recover olunca otomatik tekrar devreye alır

**Test**:
```bash
# Circuit breaker'ı test et
# 1. Bir servisi yavaşlat veya hata döndürmesini sağla
# 2. Sürekli istek gönder
for i in {1..10}; do
  curl http://localhost:8080/kisakes/api/endpoint
done

# 3. Circuit breaker state'ini kontrol et
curl http://localhost:8080/admin/circuit-breaker-status | jq
```

#### d) Rate Limiting
```yaml
loadbalancer:
  rate-limit:
    enabled: false  # Production'da true yap
```

**Aktif Etme**:
```yaml
loadbalancer:
  rate-limit:
    enabled: true
    requests-per-second: 100
    burst-capacity: 200
```

**Test**:
```bash
# Rate limit test
./scripts/load-test.sh http://localhost:8080/dummy-service/hello 1000 100
# 100 req/s'den fazla gelirse 429 Too Many Requests dönecek
```

#### e) Upstream (Backend) Servisleri
```yaml
loadbalancer:
  services:
    kisakes:
      algorithm: ROUND_ROBIN
      upstreams:
        - url: http://kisakes-app-1:8081
          weight: 1                  # Weighted algoritma için
          max-connections: 100       # Connection pool
        - url: http://kisakes-app-2:8082
          weight: 2                  # Bu instance 2x daha fazla trafik alır
          max-connections: 100
```

**Yeni Upstream Ekleme**:
```yaml
- url: http://kisakes-app-3:8085
  weight: 1
  max-connections: 150
```

**Test Süreci**:
1. `docker-compose.yml`'de yeni instance ekle
2. `application.yml`'de upstream ekle
3. Rebuild: `docker-compose build load-balancer`
4. Restart: `docker-compose restart load-balancer`
5. Test: `./scripts/health-check.sh`

#### f) SSL/TLS Configuration
```yaml
loadbalancer:
  ssl:
    enabled: false  # Production'da true
```

**SSL Aktif Etme**:
```yaml
loadbalancer:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_PASSWORD}
    key-store-type: PKCS12
```

**Test**:
```bash
# HTTPS test
curl -k https://localhost:8443/actuator/health
```

#### g) Sticky Sessions
```yaml
loadbalancer:
  sticky-session:
    enabled: false
```

**Ne Zaman Kullanılır**:
- Session-based uygulamalar
- WebSocket connections
- Upload/download işlemleri

**Aktif Etme**:
```yaml
loadbalancer:
  sticky-session:
    enabled: true
    cookie-name: LB_SESSION
    cookie-max-age: 3600
```

---

### 3. Kisakes Service Configuration

**Dosya**: `services/kisakes/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: kisakes
  
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/kisakes}
    username: ${DATABASE_USERNAME:user}
    password: ${DATABASE_PASSWORD:password}
  
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
  
  liquibase:
    enabled: ${SPRING_LIQUIBASE_ENABLED:true}
    change-log: classpath:db/changelog/db.changelog-master.yaml

server:
  port: ${SERVER_PORT:8080}
```

**Environment-Specific Overrides**:

**application-docker.yml**:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres-db:5432/kisakes
```

**Değişiklik Senaryoları**:

1. **Database Değiştirme**:
```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql-db:3306/kisakes
    driver-class-name: com.mysql.cj.jdbc.Driver
```
**Test**: 
- `docker-compose restart kisakes-app-1 kisakes-app-2`
- Log kontrolü: `docker logs kisakes-app-1`

2. **Redis Şifre Ekleme**:
```yaml
spring:
  data:
    redis:
      password: ${REDIS_PASSWORD}
```
**Test**: 
- Redis'e manuel bağlan: `redis-cli -h localhost -p 6379 -a mypassword`

3. **Liquibase Disable Etme** (2. instance'da):
```yaml
spring:
  liquibase:
    enabled: false
```

---

### 4. Database Migration (Liquibase)

**Dosya**: `services/kisakes/src/main/resources/db/changelog/db.changelog-master.yaml`

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/changes/001-create-urls-table.yaml
  - include:
      file: db/changelog/changes/002-create-short-code-lookup-table.yaml
```

**Yeni Migration Ekleme**:
1. Yeni dosya oluştur: `003-add-new-column.yaml`
```yaml
databaseChangeLog:
  - changeSet:
      id: 003-add-new-column
      author: yourname
      changes:
        - addColumn:
            tableName: urls
            columns:
              - column:
                  name: new_column
                  type: varchar(255)
```

2. Master'a ekle:
```yaml
  - include:
      file: db/changelog/changes/003-add-new-column.yaml
```

3. Test:
```bash
# Restart ile migration çalışır
docker-compose restart kisakes-app-1

# Database'i kontrol et
docker exec -it postgres-db psql -U admin -d kisakes
\d urls
```

---

### 5. Monitoring Configurations

#### Prometheus (`infrastructure/monitoring/prometheus/prometheus.yml`)

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'load-balancer'
    static_configs:
      - targets: ['load-balancer:8080']
    metrics_path: '/actuator/prometheus'
  
  - job_name: 'kisakes'
    static_configs:
      - targets: ['kisakes-app-1:8081', 'kisakes-app-2:8082']
    metrics_path: '/actuator/prometheus'
```

**Yeni Servis Ekleme**:
```yaml
  - job_name: 'my-new-service'
    static_configs:
      - targets: ['my-service-1:8085', 'my-service-2:8086']
    metrics_path: '/actuator/prometheus'
```

**Test**:
```bash
# Prometheus targets'ı kontrol et
open http://localhost:9090/targets
```

#### Loki (`infrastructure/monitoring/loki/loki-config.yml`)

Log aggregation konfigürasyonu. Genellikle değiştirilmez.

#### Promtail (`infrastructure/monitoring/promtail/promtail-config.yml`)

Docker container loglarını toplar. Default yapılandırma yeterlidir.

---

## 🔧 Load Balancer Detaylı Analiz

### Java Sınıfları ve Görevleri

#### 1. **LoadBalancerApplication.java**
- **Amaç**: Spring Boot ana uygulama sınıfı
- **Değiştirme**: Gerekli değil
- **Test**: `mvn spring-boot:run` ile lokal çalıştır

#### 2. **ProxyController.java**
- **Amaç**: Gelen istekleri backend servislerine yönlendirir
- **Endpoint**: `/**` (catch-all)
- **Mantık**:
  1. Request'i yakalar
  2. Service registry'den backend seçer
  3. Load balancing stratejisi uygular
  4. Circuit breaker kontrolü yapar
  5. Backend'e forward eder
  6. Response'u döner

**Değiştirme Senaryosu - Custom Header Ekleme**:
```java
@RestController
public class ProxyController {
    
    @RequestMapping("/**")
    public ResponseEntity<?> proxyRequest(HttpServletRequest request) {
        // Custom header ekle
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Forwarded-By", "Spectrum-LB");
        
        // ... existing code ...
    }
}
```

**Test**:
```bash
curl -v http://localhost:8080/kisakes/actuator/health
# Header'ı göreceksin: X-Forwarded-By: Spectrum-LB
```

#### 3. **AdminController.java**
- **Amaç**: Admin API'leri (monitoring, management)
- **Endpoints**:
  - `GET /admin/status` - Servis durumları
  - `GET /admin/features` - Aktif özellikler
  - `POST /admin/circuit-breaker/reset` - Circuit breaker sıfırla
  - `POST /admin/servers/{service}/add` - Dinamik backend ekleme
  - `DELETE /admin/servers/{service}/{url}` - Backend kaldırma

**Kullanım**:
```bash
# Durum kontrolü
curl http://localhost:8080/admin/status | jq

# Dinamik backend ekleme
curl -X POST http://localhost:8080/admin/servers/kisakes/add \
  -H "Content-Type: application/json" \
  -d '{"url":"http://kisakes-app-3:8085", "weight":1, "maxConnections":100}'

# Circuit breaker sıfırlama
curl -X POST http://localhost:8080/admin/circuit-breaker/reset
```

#### 4. **ServiceRegistry.java**
- **Amaç**: Backend servis listesini yönetir
- **Fonksiyonlar**:
  - Backend listesi tutma
  - Health status takibi
  - Dinamik ekleme/çıkarma

**Değiştirme - Service Discovery Entegrasyonu**:
```java
@Component
public class ServiceRegistry {
    
    // Consul/Eureka entegrasyonu için
    public void discoverServices() {
        // Consul API'den servisleri çek
        // Registry'e ekle
    }
}
```

#### 5. **HealthChecker.java**
- **Amaç**: Backend servislerinin health check'ini yapar
- **Mantık**:
  - Her N saniyede backend'lere health check yapar
  - Unhealthy olanları devre dışı bırakır
  - Recover olanları tekrar devreye alır

**Değiştirme - Custom Health Check**:
```java
@Component
public class HealthChecker {
    
    @Scheduled(fixedDelayString = "${loadbalancer.health-check-interval}")
    public void checkHealth() {
        for (Server server : servers) {
            boolean healthy = customHealthCheck(server);
            server.setHealthy(healthy);
        }
    }
    
    private boolean customHealthCheck(Server server) {
        // Custom logic: database bağlantısı, disk alanı vb.
        return checkDatabase(server) && checkDiskSpace(server);
    }
}
```

#### 6. **Load Balancing Strategies**

##### a) **RoundRobinStrategy.java**
- Sırayla dağıtım
- En basit ve yaygın

##### b) **LeastConnectionsStrategy.java**
- En az aktif bağlantısı olan servise yönlendirir
- Farklı işlem süreli istekler için ideal

##### c) **WeightedRoundRobinStrategy.java**
- Ağırlığa göre dağıtım
- Farklı kapasiteli sunucular için

##### d) **IpHashStrategy.java**
- Client IP'sine göre consistent hashing
- Session persistence sağlar

##### e) **RandomStrategy.java**
- Random dağıtım

**Yeni Strateji Ekleme**:
```java
@Component
public class CustomStrategy implements LoadBalancingStrategy {
    
    @Override
    public Server selectServer(List<Server> servers, HttpServletRequest request) {
        // Custom logic
        // Örnek: Header'a göre routing
        String priority = request.getHeader("X-Priority");
        if ("high".equals(priority)) {
            return getHighPriorityServer(servers);
        }
        return getDefaultServer(servers);
    }
}
```

**Kullanım**:
```yaml
loadbalancer:
  algorithm: CUSTOM
```

```java
@Component
public class LoadBalancingStrategyFactory {
    
    public LoadBalancingStrategy getStrategy(String algorithm) {
        return switch (algorithm) {
            case "ROUND_ROBIN" -> new RoundRobinStrategy();
            case "CUSTOM" -> new CustomStrategy();
            // ...
        };
    }
}
```

#### 7. **CircuitBreaker.java**
- **Amaç**: Cascade failure önleme
- **States**: CLOSED, OPEN, HALF_OPEN
- **Mantık**:
  - CLOSED: Normal çalışma
  - OPEN: Threshold aşıldı, istekler hemen fail
  - HALF_OPEN: Test aşaması, başarılı olursa CLOSED'a döner

**Değiştirme - Custom Failure Detection**:
```java
public class CircuitBreaker {
    
    public void recordFailure() {
        failureCount++;
        
        // Custom: belirli hata kodları için farklı davran
        if (is5xxError()) {
            failureCount += 2; // 5xx hataları daha ağır bas
        }
        
        if (failureCount >= failureThreshold) {
            state = State.OPEN;
        }
    }
}
```

#### 8. **RateLimiter.java**
- **Amaç**: Rate limiting (DDoS koruması)
- **Algoritma**: Token Bucket veya Sliding Window

**Token Bucket Mantığı**:
```
Bucket Capacity: 100 token
Refill Rate: 10 token/second

İstek geldiğinde:
- 1 token tüket
- Token yoksa 429 döner
- Her saniye bucket'a 10 token eklenir
```

**Değiştirme - Per-User Rate Limiting**:
```java
public class RateLimiter {
    
    private Map<String, TokenBucket> userBuckets = new ConcurrentHashMap<>();
    
    public boolean allowRequest(HttpServletRequest request) {
        String userId = request.getHeader("X-User-ID");
        TokenBucket bucket = userBuckets.computeIfAbsent(userId, 
            k -> new TokenBucket(100, 10));
        
        return bucket.tryConsume();
    }
}
```

#### 9. **StickySessionManager.java**
- **Amaç**: Session persistence
- **Mantık**: Cookie veya session ID ile aynı backend'e yönlendirir

#### 10. **LoadBalancerMetrics.java**
- **Amaç**: Micrometer metrics (Prometheus integration)
- **Metrics**:
  - `lb_requests_total` - Toplam istek sayısı
  - `lb_requests_failed` - Başarısız istek sayısı
  - `lb_active_connections` - Aktif bağlantı sayısı
  - `lb_response_time` - Response süreleri

**Custom Metric Ekleme**:
```java
@Component
public class LoadBalancerMetrics {
    
    private final Counter customMetric;
    
    public LoadBalancerMetrics(MeterRegistry registry) {
        this.customMetric = Counter.builder("lb_custom_metric")
            .description("Custom description")
            .register(registry);
    }
    
    public void recordCustomEvent() {
        customMetric.increment();
    }
}
```

**Grafana'da görüntüleme**:
```promql
rate(lb_custom_metric_total[5m])
```

---

## 🧪 Test ve Çalıştırma

### İlk Kurulum

```bash
# 1. Repository'yi clone'la
git clone <repo-url>
cd spectrum-platform

# 2. Setup script'i çalıştır
chmod +x scripts/*.sh
./scripts/setup.sh

# 3. Servisleri başlat
./scripts/start.sh

# 4. Health check (30-60 saniye bekle)
./scripts/health-check.sh
```

### Load Balancer Test Senaryoları

#### Test 1: Basic Routing

```bash
# Load balancer üzerinden kisakes servisine istek
curl http://localhost:8080/kisakes/actuator/health

# Response:
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"}
  }
}
```

#### Test 2: Round Robin Doğrulama

```bash
# 10 istek gönder ve hangi instance'ın cevap verdiğini gör
for i in {1..10}; do
  curl -s http://localhost:8080/kisakes/actuator/info | jq -r '.app.instance'
done

# Çıktı (Round Robin):
# kisakes-app-1
# kisakes-app-2
# kisakes-app-1
# kisakes-app-2
# ...
```

#### Test 3: Load Balancing Strategy Değiştirme

```bash
# 1. Config değiştir
vim infrastructure/load-balancer/src/main/resources/application.yml
# algorithm: LEAST_CONNECTIONS yap

# 2. Rebuild
docker-compose build load-balancer

# 3. Restart
docker-compose restart load-balancer

# 4. Test
for i in {1..20}; do
  curl -s http://localhost:8080/dummy-service/slow-endpoint &
done
wait

# 5. İstatistikleri kontrol et
curl http://localhost:8080/admin/status | jq
```

#### Test 4: Circuit Breaker

```bash
# 1. Bir servisi kapat
docker stop kisakes-app-1

# 2. İstekler gönder (circuit breaker açılana kadar)
for i in {1..10}; do
  curl http://localhost:8080/kisakes/actuator/health
  sleep 1
done

# 3. Circuit breaker state'ini kontrol et
curl http://localhost:8080/admin/circuit-breaker-status | jq

# 4. Servisi tekrar başlat
docker start kisakes-app-1

# 5. Circuit breaker kapanmasını bekle (success threshold kadar başarılı istek)
for i in {1..5}; do
  curl http://localhost:8080/kisakes/actuator/health
  sleep 2
done
```

#### Test 5: Health Check

```bash
# 1. Bir servisi unhealthy yap (örnek: database bağlantısını kes)
docker pause postgres-db

# 2. Health checker'ın tespit etmesini bekle (5-10 saniye)
sleep 10

# 3. Load balancer status kontrol et
curl http://localhost:8080/admin/status | jq

# Çıktı:
# {
#   "kisakes": {
#     "totalServers": 2,
#     "healthyCount": 0,  # İkisi de unhealthy
#     "servers": [...]
#   }
# }

# 4. Database'i tekrar başlat
docker unpause postgres-db

# 5. Recovery'yi izle
watch -n 1 'curl -s http://localhost:8080/admin/status | jq'
```

#### Test 6: Rate Limiting

```bash
# 1. Rate limiting aktif et
vim infrastructure/load-balancer/src/main/resources/application.yml
# rate-limit.enabled: true yap
# rate-limit.requests-per-second: 10

# 2. Rebuild ve restart
docker-compose build load-balancer
docker-compose restart load-balancer

# 3. Yüksek trafik gönder
./scripts/load-test.sh http://localhost:8080/dummy-service/hello 100 20

# 4. 429 Too Many Requests response'larını gör
```

#### Test 7: Weighted Round Robin

```bash
# 1. Ağırlıkları değiştir
vim infrastructure/load-balancer/src/main/resources/application.yml
# kisakes-app-1: weight: 1
# kisakes-app-2: weight: 3

# 2. Rebuild ve restart
docker-compose build load-balancer
docker-compose restart load-balancer

# 3. Test (app-2 3x daha fazla istek almalı)
for i in {1..40}; do
  curl -s http://localhost:8080/kisakes/actuator/info | jq -r '.app.instance'
done | sort | uniq -c

# Çıktı:
# 10 kisakes-app-1
# 30 kisakes-app-2
```

#### Test 8: Sticky Sessions

```bash
# 1. Sticky session aktif et
vim infrastructure/load-balancer/src/main/resources/application.yml
# sticky-session.enabled: true

# 2. Rebuild ve restart
docker-compose build load-balancer
docker-compose restart load-balancer

# 3. Cookie ile test
# İlk istek - cookie al
curl -c cookies.txt http://localhost:8080/kisakes/actuator/info

# Sonraki istekler - aynı instance'a gitmeli
for i in {1..10}; do
  curl -b cookies.txt -s http://localhost:8080/kisakes/actuator/info | jq -r '.app.instance'
done

# Hepsi aynı instance döner
```

#### Test 9: Load Test (Performance)

```bash
# Apache Bench ile
sudo apt-get install apache2-utils

# 1000 istek, 50 concurrent
ab -n 1000 -c 50 http://localhost:8080/dummy-service/hello

# Sonuçlar:
# - Requests per second
# - Time per request
# - Transfer rate
# - Percentile response times

# Script ile
./scripts/load-test.sh http://localhost:8080/kisakes/actuator/health 5000 100
```

#### Test 10: Monitoring Integration

```bash
# 1. Prometheus'ta metrics kontrol et
open http://localhost:9090/graph

# 2. Queries:
# - rate(http_server_requests_seconds_count[5m])  # Request rate
# - http_server_requests_seconds_max              # Max response time
# - lb_active_connections                         # Active connections

# 3. Grafana dashboard
open http://localhost:3000
# Login: admin / admin123
# Dashboards > Load Balancer Dashboard

# 4. Loki'de logları gör
# Grafana > Explore > Loki
# Query: {service="load-balancer"}
```

---

## 🔄 Değişiklik Senaryoları

### Senaryo 1: Yeni Mikroservis Ekleme

**Adımlar**:

1. **Servis Oluştur**:
```bash
cd services
mkdir my-new-service
cd my-new-service
```

2. **pom.xml**:
```xml
<parent>
    <groupId>com.degerli</groupId>
    <artifactId>spectrum-platform</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../../pom.xml</relativePath>
</parent>

<artifactId>my-new-service</artifactId>
<name>My New Service</name>
```

3. **Dockerfile**:
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

4. **docker-compose.yml'e ekle**:
```yaml
  my-new-service-1:
    build:
      context: .
      dockerfile: services/my-new-service/Dockerfile
    container_name: my-new-service-1
    networks:
      - microservices-net
    expose:
      - "8085"
    environment:
      - SERVER_PORT=8085
      - SPRING_PROFILES_ACTIVE=docker
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8085/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5
```

5. **Load Balancer'a ekle** (`infrastructure/load-balancer/src/main/resources/application.yml`):
```yaml
loadbalancer:
  services:
    my-new-service:
      algorithm: ROUND_ROBIN
      upstreams:
        - url: http://my-new-service-1:8085
          weight: 1
          max-connections: 100
```

6. **Parent pom.xml'e module ekle**:
```xml
<modules>
    <module>services/my-new-service</module>
</modules>
```

7. **Build ve Deploy**:
```bash
mvn clean package
docker-compose build
docker-compose up -d
./scripts/health-check.sh
```

8. **Test**:
```bash
curl http://localhost:8080/my-new-service/actuator/health
```

### Senaryo 2: Load Balancing Algoritması Değiştirme

**Durum**: Servislerinizde farklı işlem süreleri var, LEAST_CONNECTIONS kullanmak istiyorsun.

**Adımlar**:

1. **Config Değiştir**:
```bash
vim infrastructure/load-balancer/src/main/resources/application.yml
```

```yaml
loadbalancer:
  services:
    kisakes:
      algorithm: LEAST_CONNECTIONS  # ROUND_ROBIN'den değiştir
```

2. **Rebuild (sadece load-balancer)**:
```bash
docker-compose build load-balancer
```

3. **Restart**:
```bash
docker-compose restart load-balancer
```

4. **Test**:
```bash
# Yavaş endpoint'e eşzamanlı istekler gönder
for i in {1..10}; do
  curl http://localhost:8080/kisakes/slow-endpoint &
done
wait

# Bağlantı sayılarını kontrol et
curl http://localhost:8080/admin/status | jq '.kisakes.servers[] | {url, activeConnections}'
```

5. **Monitor**:
```bash
# Prometheus'ta monitoring
open http://localhost:9090
# Query: lb_active_connections{service="kisakes"}
```

### Senaryo 3: Circuit Breaker Threshold Değiştirme

**Durum**: Servisler çok hızlı circuit breaker'a giriyor, threshold'u artırmak istiyorsun.

**Adımlar**:

1. **Config Değiştir**:
```yaml
loadbalancer:
  circuit-breaker:
    enabled: true
    failure-threshold: 10        # 5'ten 10'a çıkar
    success-threshold: 3         # 2'den 3'e çıkar
    timeout-seconds: 120         # 60'tan 120'ye çıkar
```

2. **Rebuild ve Restart**:
```bash
docker-compose build load-balancer
docker-compose restart load-balancer
```

3. **Test**:
```bash
# Bir servisi kapat
docker stop kisakes-app-1

# 10 istek gönder (threshold kadar)
for i in {1..10}; do
  curl http://localhost:8080/kisakes/actuator/health
  echo "Request $i"
  sleep 1
done

# Circuit breaker açılmalı
curl http://localhost:8080/admin/circuit-breaker-status | jq
```

### Senaryo 4: Rate Limiting Aktif Etme

**Durum**: Production'a geçiyorsun, DDoS koruması istiyorsun.

**Adımlar**:

1. **Config**:
```yaml
loadbalancer:
  rate-limit:
    enabled: true
    requests-per-second: 100
    burst-capacity: 200
```

2. **Rebuild ve Restart**:
```bash
docker-compose build load-balancer
docker-compose restart load-balancer
```

3. **Test**:
```bash
# Yüksek trafik gönder
./scripts/load-test.sh http://localhost:8080/dummy-service/hello 1000 100

# 429 response'larını gör
```

4. **Per-User Rate Limiting (Custom)**:
```java
// RateLimiter.java'da değişiklik
public boolean allowRequest(HttpServletRequest request) {
    String userId = request.getHeader("X-User-ID");
    if (userId == null) userId = "anonymous";
    
    TokenBucket bucket = userBuckets.computeIfAbsent(userId, 
        k -> new TokenBucket(100, 10));
    
    return bucket.tryConsume();
}
```

### Senaryo 5: Database Migration Ekleme

**Durum**: `urls` tablosuna yeni bir kolon eklemek istiyorsun.

**Adımlar**:

1. **Yeni Liquibase Changeset**:
```bash
vim services/kisakes/src/main/resources/db/changelog/changes/003-add-expiry-date.yaml
```

```yaml
databaseChangeLog:
  - changeSet:
      id: 003-add-expiry-date
      author: yourname
      changes:
        - addColumn:
            tableName: urls
            columns:
              - column:
                  name: expiry_date
                  type: timestamp
                  constraints:
                    nullable: true
```

2. **Master'a Ekle**:
```yaml
# db.changelog-master.yaml
  - include:
      file: db/changelog/changes/003-add-expiry-date.yaml
```

3. **Entity Güncelle**:
```java
@Entity
public class Url {
    // ... existing fields ...
    
    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;
}
```

4. **Rebuild ve Restart**:
```bash
mvn clean package
docker-compose build kisakes-app-1 kisakes-app-2
docker-compose restart kisakes-app-1

# NOT: kisakes-app-2'de Liquibase disabled, restart gerekmez
```

5. **Verify**:
```bash
docker exec -it postgres-db psql -U admin -d kisakes
\d urls
# expiry_date kolonunu göreceksin
```

### Senaryo 6: Prometheus Scrape Interval Değiştirme

**Durum**: Daha sık metric toplamak istiyorsun.

**Adımlar**:

1. **Config Değiştir**:
```bash
vim infrastructure/monitoring/prometheus/prometheus.yml
```

```yaml
global:
  scrape_interval: 5s      # 15s'den 5s'ye düşür
  evaluation_interval: 5s
```

2. **Restart**:
```bash
docker-compose restart prometheus
```

3. **Verify**:
```bash
# Prometheus UI'da Targets'ı kontrol et
open http://localhost:9090/targets
# Last Scrape kolonu - 5 saniyede bir update olmalı
```

### Senaryo 7: Grafana Dashboard Ekleme

**Adımlar**:

1. **Dashboard JSON Oluştur**:
```bash
vim infrastructure/monitoring/grafana/dashboards/load-balancer-dashboard.json
```

2. **Provisioning Config**:
```bash
vim infrastructure/monitoring/grafana/dashboards/dashboard.yml
```

```yaml
apiVersion: 1

providers:
  - name: 'Load Balancer'
    folder: 'Custom'
    type: file
    options:
      path: /etc/grafana/provisioning/dashboards
```

3. **Restart Grafana**:
```bash
docker-compose restart grafana
```

4. **Dashboard Import**:
- Grafana UI'da dashboards'a git
- Otomatik yüklenmiş olmalı

### Senaryo 8: SSL/TLS Ekleme

**Adımlar**:

1. **Keystore Oluştur**:
```bash
keytool -genkeypair -alias spectrum-lb -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -validity 3650 \
  -storepass changeit
```

2. **Keystore'u Kopyala**:
```bash
cp keystore.p12 infrastructure/load-balancer/src/main/resources/
```

3. **Config**:
```yaml
# application.yml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12

loadbalancer:
  ssl:
    enabled: true
```

4. **docker-compose.yml Port Mapping**:
```yaml
load-balancer:
  ports:
    - "8080:8080"
    - "8443:8443"  # HTTPS
```

5. **Rebuild ve Restart**:
```bash
docker-compose build load-balancer
docker-compose restart load-balancer
```

6. **Test**:
```bash
curl -k https://localhost:8443/actuator/health
```

### Senaryo 9: Blue-Green Deployment

**Durum**: Zero-downtime deployment yapmak istiyorsun.

**Adımlar**:

1. **Yeni Version Deploy Et (Green)**:
```bash
# Yeni version'ı build et
mvn clean package -DskipTests

# Sadece bir instance'ı güncelle (Green)
docker-compose build kisakes-app-2
docker-compose stop kisakes-app-2
docker-compose up -d kisakes-app-2
```

2. **Health Check**:
```bash
# Green instance'ın healthy olmasını bekle
watch -n 1 'curl -s http://kisakes-app-2:8082/actuator/health'
```

3. **Load Balancer'da Green'i Aktif Et**:
```bash
# Admin API kullanarak
curl -X POST http://localhost:8080/admin/servers/kisakes/enable \
  -d "url=http://kisakes-app-2:8082"
```

4. **Blue'yu Güncelle**:
```bash
docker-compose build kisakes-app-1
docker-compose stop kisakes-app-1
docker-compose up -d kisakes-app-1
```

5. **Verify**:
```bash
./scripts/health-check.sh
curl http://localhost:8080/admin/status | jq
```

### Senaryo 10: Connection Pool Boyutu Değiştirme

**Durum**: Yüksek trafikte connection pool yetersiz.

**Adımlar**:

1. **Load Balancer Config**:
```yaml
loadbalancer:
  services:
    kisakes:
      upstreams:
        - url: http://kisakes-app-1:8081
          max-connections: 200  # 100'den 200'e çıkar
```

2. **Backend HikariCP Config** (kisakes service):
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50  # 20'den 50'ye çıkar
      minimum-idle: 10
      connection-timeout: 30000
```

3. **Rebuild ve Restart**:
```bash
# Load balancer
docker-compose build load-balancer
docker-compose restart load-balancer

# Kisakes
docker-compose build kisakes-app-1 kisakes-app-2
docker-compose restart kisakes-app-1 kisakes-app-2
```

4. **Test**:
```bash
# Yüksek concurrent requests
./scripts/load-test.sh http://localhost:8080/kisakes/api/endpoint 10000 100
```

---

## 🔍 Troubleshooting

### Problem 1: Load Balancer Başlamıyor

**Semptom**:
```bash
docker logs load-balancer
# Error: Cannot connect to backend services
```

**Çözüm**:
```bash
# 1. Backend servislerin healthy olduğunu kontrol et
./scripts/health-check.sh

# 2. Network kontrolü
docker network inspect microservices-net

# 3. DNS çözümleme testi
docker exec load-balancer ping kisakes-app-1

# 4. Restart order
docker-compose stop
docker-compose up -d postgres-db redis-cache
sleep 10
docker-compose up -d kisakes-app-1 kisakes-app-2 dummy-service-1 dummy-service-2
sleep 20
docker-compose up -d load-balancer
```

### Problem 2: Circuit Breaker Sürekli Açık

**Semptom**:
```bash
curl http://localhost:8080/kisakes/actuator/health
# 503 Service Unavailable - Circuit Breaker OPEN
```

**Çözüm**:
```bash
# 1. Backend servisleri kontrol et
docker ps | grep kisakes

# 2. Backend loglarını incele
docker logs kisakes-app-1
docker logs kisakes-app-2

# 3. Manuel circuit breaker reset
curl -X POST http://localhost:8080/admin/circuit-breaker/reset

# 4. Threshold'u geçici olarak artır
# application.yml: failure-threshold: 100
docker-compose restart load-balancer
```

### Problem 3: Health Check Fail

**Semptom**:
```bash
./scripts/health-check.sh
# ✗ kisakes-app-1 (unhealthy)
```

**Çözüm**:
```bash
# 1. Container status
docker ps -a | grep kisakes-app-1

# 2. Logs
docker logs kisakes-app-1 --tail 100

# 3. Health endpoint manuel test
docker exec kisakes-app-1 curl http://localhost:8081/actuator/health

# 4. Database bağlantısı
docker exec -it postgres-db psql -U admin -d kisakes -c "SELECT 1;"

# 5. Redis bağlantısı
docker exec redis-cache redis-cli ping

# 6. Network
docker exec kisakes-app-1 ping postgres-db
```

### Problem 4: Yüksek Latency

**Semptom**:
```bash
./scripts/load-test.sh
# Average response time: 5000ms (çok yüksek)
```

**Diagnosis**:
```bash
# 1. Prometheus'ta response time
open http://localhost:9090
# Query: http_server_requests_seconds_max{job="load-balancer"}

# 2. Load balancer metrics
curl http://localhost:8080/actuator/metrics/http.server.requests | jq

# 3. Database slow queries
docker exec postgres-db psql -U admin -d kisakes -c "
  SELECT query, mean_exec_time 
  FROM pg_stat_statements 
  ORDER BY mean_exec_time DESC 
  LIMIT 10;"

# 4. Connection pool
curl http://localhost:8080/admin/status | jq '.kisakes.servers[] | {url, activeConnections, maxConnections}'
```

**Çözümler**:
```bash
# A. Database indexler
docker exec -it postgres-db psql -U admin -d kisakes
CREATE INDEX idx_urls_short_code ON urls(short_code);

# B. Redis cache aktif et
# application.yml'de caching config

# C. Connection pool artır (yukarıda anlatıldı)

# D. Load balancing stratejisini değiştir
# LEAST_CONNECTIONS kullan
```

### Problem 5: 429 Too Many Requests (Rate Limiting)

**Semptom**:
```bash
curl http://localhost:8080/kisakes/api/endpoint
# 429 Too Many Requests
```

**Çözüm**:
```bash
# 1. Rate limit config kontrol
vim infrastructure/load-balancer/src/main/resources/application.yml
# rate-limit.enabled: false veya threshold'u artır

# 2. Geçici olarak disable et
# Admin API (eğer implement edilmişse)
curl -X POST http://localhost:8080/admin/rate-limit/disable

# 3. Restart
docker-compose restart load-balancer
```

### Problem 6: Database Connection Pool Exhausted

**Semptom**:
```bash
docker logs kisakes-app-1
# ERROR: HikariPool - Connection is not available
```

**Çözüm**:
```bash
# 1. Pool size artır
vim services/kisakes/src/main/resources/application-docker.yml
```

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 20
      connection-timeout: 60000
      idle-timeout: 600000
      max-lifetime: 1800000
```

```bash
# 2. Rebuild
docker-compose build kisakes-app-1 kisakes-app-2
docker-compose restart kisakes-app-1 kisakes-app-2

# 3. PostgreSQL max_connections artır
docker exec -it postgres-db bash
echo "max_connections = 200" >> /var/lib/postgresql/data/postgresql.conf
docker-compose restart postgres-db
```

### Problem 7: Memory Leak

**Semptom**:
```bash
docker stats
# load-balancer: 2GB+ memory kullanımı
```

**Diagnosis**:
```bash
# 1. Heap dump al
docker exec load-balancer jcmd 1 GC.heap_dump /tmp/heap.hprof
docker cp load-balancer:/tmp/heap.hprof ./heap.hprof

# 2. VisualVM veya Eclipse MAT ile analiz et

# 3. Thread dump
docker exec load-balancer jstack 1 > threads.txt
```

**Çözümler**:
```bash
# A. JVM heap size artır
# Dockerfile'da
ENV JAVA_OPTS="-Xms512m -Xmx2048m"

# B. GC tuning
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# C. Memory leak varsa fix'le (kod değişikliği gerekebilir)
```

### Problem 8: Prometheus Metrics Toplanmıyor

**Semptom**:
```bash
open http://localhost:9090/targets
# All targets: DOWN
```

**Çözüm**:
```bash
# 1. Network connectivity
docker exec prometheus ping load-balancer
docker exec prometheus curl http://load-balancer:8080/actuator/prometheus

# 2. Actuator endpoints exposed mi?
docker exec load-balancer curl http://localhost:8080/actuator
# prometheus endpoint'i görmeli

# 3. Config kontrol
docker exec prometheus cat /etc/prometheus/prometheus.yml

# 4. Restart
docker-compose restart prometheus
```

### Problem 9: Grafana Dashboards Yok

**Semptom**:
```bash
# Grafana'da dashboard görünmüyor
```

**Çözüm**:
```bash
# 1. Provisioning klasörünü kontrol et
ls -la infrastructure/monitoring/grafana/dashboards/

# 2. Volume mount doğru mu?
docker inspect grafana | grep Mounts -A 20

# 3. Grafana logları
docker logs grafana

# 4. Manuel import
# Grafana UI > Dashboards > Import > Upload JSON
```

### Problem 10: Docker Compose Build Hatası

**Semptom**:
```bash
docker-compose build
# ERROR: Cannot locate specified Dockerfile
```

**Çözüm**:
```bash
# 1. Context ve dockerfile path kontrol
cat docker-compose.yml | grep -A 5 "build:"

# 2. Dockerfile var mı?
ls -la services/kisakes/Dockerfile
ls -la infrastructure/load-balancer/Dockerfile

# 3. Maven build önce çalışmalı
mvn clean package -DskipTests

# 4. Cache temizle
docker-compose build --no-cache
```

---

## 📊 Monitoring ve Observability

### Prometheus Queries

**Request Rate**:
```promql
rate(http_server_requests_seconds_count{job="load-balancer"}[5m])
```

**Average Response Time**:
```promql
rate(http_server_requests_seconds_sum[5m]) 
/ 
rate(http_server_requests_seconds_count[5m])
```

**Error Rate**:
```promql
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
```

**Active Connections**:
```promql
lb_active_connections{service="kisakes"}
```

**Circuit Breaker State**:
```promql
lb_circuit_breaker_state{service="kisakes"}
```

### Loki Queries

**Load Balancer Errors**:
```logql
{service="load-balancer"} |= "ERROR"
```

**Slow Requests**:
```logql
{service="load-balancer"} | json | duration > 1000
```

**Circuit Breaker Events**:
```logql
{service="load-balancer"} |= "Circuit Breaker" |= "OPEN"
```

---

## 🚀 Production Checklist

### Güvenlik

- [ ] Default şifreleri değiştir (PostgreSQL, Redis, Grafana)
- [ ] SSL/TLS aktif et
- [ ] Rate limiting aktif et
- [ ] Database şifre şifreleme (vault kullan)
- [ ] Secrets management (Docker secrets veya Kubernetes secrets)
- [ ] Network policies (firewall kuralları)
- [ ] Container security scanning (Trivy, Clair)

### Performance

- [ ] Connection pool'ları optimize et
- [ ] JVM heap size ayarla
- [ ] Database indexler oluştur
- [ ] Redis caching stratejisi belirle
- [ ] Load test yap ve bottleneck'leri tespit et
- [ ] Auto-scaling stratejisi belirle

### Monitoring

- [ ] Alert kuralları tanımla (Prometheus Alertmanager)
- [ ] Dashboard'lar hazırla
- [ ] Log retention policy belirle
- [ ] Metrics retention policy belirle
- [ ] On-call rotation belirle

### Deployment

- [ ] CI/CD pipeline kur
- [ ] Blue-green deployment stratejisi
- [ ] Rollback planı
- [ ] Database migration stratejisi
- [ ] Backup stratejisi
- [ ] Disaster recovery planı

### Documentation

- [ ] API documentation (Swagger/OpenAPI)
- [ ] Runbook'lar hazırla
- [ ] Architecture decision records (ADR)
- [ ] Onboarding dokümantasyonu

---

## 📚 Ek Kaynaklar

### Komut Cheatsheet

```bash
# Servis başlatma
./scripts/start.sh

# Servis durdurma
./scripts/stop.sh

# Health check
./scripts/health-check.sh

# Load test
./scripts/load-test.sh [URL] [REQUESTS] [CONCURRENCY]

# Logları görüntüleme
./scripts/logs.sh [SERVICE_NAME]

# Scale up/down
./scripts/scale.sh kisakes 3

# Restart
./scripts/restart.sh [SERVICE_NAME]

# Build
docker-compose build [SERVICE_NAME]

# Logs
docker-compose logs -f [SERVICE_NAME]

# Exec
docker exec -it [CONTAINER] bash

# Stats
docker stats

# Cleanup
./scripts/clean.sh
```

### API Endpoints

**Load Balancer**:
- `GET /actuator/health` - Health check
- `GET /actuator/metrics` - Metrics
- `GET /actuator/prometheus` - Prometheus metrics
- `GET /admin/status` - Service status
- `GET /admin/features` - Feature flags
- `POST /admin/circuit-breaker/reset` - Reset circuit breaker

**Kisakes Service**:
- `GET /kisakes/actuator/health` - Health check
- `GET /kisakes/actuator/metrics` - Metrics
- `GET /kisakes/api/urls` - URL listesi
- `POST /kisakes/api/urls` - Yeni URL oluştur

**Monitoring**:
- `http://localhost:9090` - Prometheus UI
- `http://localhost:3000` - Grafana UI
- `http://localhost:3100/ready` - Loki health

---

## 🎓 Sonuç

Bu dokümantasyon, Spectrum Platform'un tüm bileşenlerini, konfigürasyonlarını ve kullanım senaryolarını kapsamaktadır. 

**Önemli Notlar**:
1. Her değişiklikten sonra ilgili servisleri rebuild ve restart et
2. Test et - production'a göndermeden önce tüm senaryoları test et
3. Monitoring - değişikliklerin etkisini Prometheus ve Grafana'da izle
4. Dokümante et - yaptığın değişiklikleri kaydet

**Destek için**:
- GitHub Issues
- Documentation: `/docs`
- Examples: `/examples`

---

**Version**: 1.0.0  
**Last Updated**: 2024  
**Author**: Spectrum Platform Team