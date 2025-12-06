# ==========================================================
# Load Balancer - Dockerfile
# ==========================================================

# ----------------------------------------------------------
# Stage 1: Builder
# ----------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine as builder
WORKDIR /app

# 1. Maven Wrapper ve Ana POM'u kopyala
COPY .mvn/ .mvn
COPY mvnw .
COPY pom.xml .

# 2. Servisin POM dosyasını KENDİ YERİNE kopyala
COPY infrastructure/load-balancer/pom.xml infrastructure/load-balancer/

# 3. Bağımlılıkları indir
WORKDIR /app/infrastructure/load-balancer
RUN ../../mvnw dependency:go-offline -B

# 4. Kaynak kodları kopyala
COPY infrastructure/load-balancer/src src

# 5. Build al
RUN ../../mvnw package -DskipTests

# ----------------------------------------------------------
# Stage 2: Runtime
# ----------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Spectrum Platform"
LABEL description="Load Balancer - Production Image"

# Add a non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Install curl for health checks
RUN apk add --no-cache curl

WORKDIR /app

# Copy the built JAR from builder stage
# DİKKAT: Yol builder'daki WORKDIR yapısına göre ayarlandı
COPY --from=builder /app/infrastructure/load-balancer/target/load-balancer-*.jar app.jar

# Change ownership
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose ports
EXPOSE 8080
EXPOSE 8443

# Health check
HEALTHCHECK --interval=10s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Run
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]