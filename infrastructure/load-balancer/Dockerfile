# ==========================================================
# Load Balancer - Dockerfile for Spectrum Platform
# Assumes the project has been built and JAR is available
# ==========================================================

# Use Java 21 JRE for the runtime environment
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Spectrum Platform"
LABEL description="Load Balancer - Production Image"

# Add a non-root user for enhanced security
RUN addgroup -S spring && adduser -S spring -G spring

# Install curl, which is required for the health check
RUN apk add --no-cache curl

WORKDIR /app

# Copy the pre-built JAR file from the 'target' directory into the container.
# The JAR file is expected to be created by 'mvn clean install' before building the image.
COPY target/load-balancer-*.jar app.jar

# Change the ownership of the application directory to the non-root user
RUN chown -R spring:spring /app

# Switch to the non-root user
USER spring:spring

# Expose the application port
EXPOSE 8080

# Health check to ensure the application is running correctly
HEALTHCHECK --interval=10s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Set default JVM options for running in a containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# The command to run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]