# ==========================================================
# Dummy Service - Dockerfile
# ==========================================================

# ----------------------------------------------------------
# Stage 1: Builder
# ----------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine as builder
WORKDIR /app

# 1. Maven Wrapper ve Ana POM
COPY .mvn/ .mvn
COPY mvnw .
COPY pom.xml .

# 2. Servis POM'u
COPY services/dummy-service/pom.xml services/dummy-service/

# 3. Bağımlılıkları indir
WORKDIR /app/services/dummy-service
RUN ../../mvnw dependency:go-offline -B

# 4. Kaynak kodları kopyala
COPY services/dummy-service/src src

# 5. Build al
RUN ../../mvnw package -DskipTests

# ----------------------------------------------------------
# Stage 2: Runtime
# ----------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Spectrum Platform"
LABEL service="dummy-service"

RUN addgroup -S spring && adduser -S spring -G spring
RUN apk add --no-cache curl

WORKDIR /app

# JAR dosyasını kopyala
COPY --from=builder /app/services/dummy-service/target/dummy-service-*.jar app.jar

RUN chown -R spring:spring /app
USER spring:spring

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]