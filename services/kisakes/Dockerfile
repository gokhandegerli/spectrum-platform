# ============================================
# Kisakes Service - Dockerfile for Spectrum Platform
# Multi-stage build for optimal image size
# ============================================

# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS builder

LABEL maintainer="Spectrum Platform"
LABEL description="Kisakes Service - Build Stage"

WORKDIR /build

# Copy Maven wrapper and pom.xml first (for layer caching)
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

# Download dependencies (cached if pom.xml unchanged)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application (skip tests for faster builds)
RUN ./mvnw clean package -DskipTests -B

# Verify JAR was created
RUN ls -la target/

# ============================================
# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Spectrum Platform"
LABEL description="Kisakes Service - Production Image"
LABEL version="1.0.0"

# Add non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Install curl for health checks
RUN apk add --no-cache curl

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Change ownership
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose port (will be set via env var)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1

# JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]