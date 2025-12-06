# ==========================================================
# Kisakes Service - Dockerfile
# ==========================================================

# ----------------------------------------------------------
# Stage 1: Builder
# ----------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine as builder
WORKDIR /app

# 1. Maven Wrapper ve Ana POM'u kopyala (Root Context)
COPY .mvn/ .mvn
COPY mvnw .
COPY pom.xml .

# 2. Servisin POM dosyasını kendi klasör yapısına kopyala
COPY services/kisakes/pom.xml services/kisakes/

# 3. Bağımlılıkları indir
# Çalışma dizinini servisin içine alıyoruz ki parent'ı ../../pom.xml olarak görebilsin
WORKDIR /app/services/kisakes
RUN ../../mvnw dependency:go-offline -B

# 4. Kaynak kodları kopyala
COPY services/kisakes/src src

# 5. Build al
RUN ../../mvnw package -DskipTests

# ----------------------------------------------------------
# Stage 2: Runtime
# ----------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Spectrum Platform"
LABEL service="kisakes"

# Güvenlik için kullanıcı oluştur
RUN addgroup -S spring && adduser -S spring -G spring

# Health check için curl
RUN apk add --no-cache curl

WORKDIR /app

# Builder aşamasından JAR dosyasını al
# DİKKAT: Yol, builder aşamasındaki WORKDIR yapısına göre ayarlandı
COPY --from=builder /app/services/kisakes/target/kisakes-*.jar app.jar

# İzinleri ayarla
RUN chown -R spring:spring /app

USER spring:spring

# Port
EXPOSE 8080

# Health Check
HEALTHCHECK --interval=10s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM Ayarları
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]