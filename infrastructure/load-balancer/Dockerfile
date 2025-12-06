# ==========================================================
# Load Balancer - Dockerfile for Spectrum Platform
# Multi-stage build for optimal image size
# ==========================================================

# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

LABEL maintainer="Spectrum Platform"
LABEL stage="builder"

WORKDIR /build

# Copy Maven wrapper and pom.xml first (for layer caching)
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests -B

# ==========================================================
# Runtime stage
# ==========================================================
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Spectrum Platform"
LABEL description="Load Balancer - Production Image"
LABEL version="1.0.0"

# Add a non-root user for enhanced security
RUN addgroup -S spring && adduser -S spring -G spring

# Install curl for health checks
RUN apk add --no-cache curl

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /build/target/load-balancer-*.jar app.jar

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

# Set default JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]