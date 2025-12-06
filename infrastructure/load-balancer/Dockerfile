FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Maven wrapper ve pom.xml'i kopyala
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Dependencies'i indir (cache için)
RUN ./mvnw dependency:go-offline -B

# Kaynak kodları kopyala
COPY src src

# Build
RUN ./mvnw package -DskipTests

# Runtime image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# JAR'ı kopyala
COPY --from=builder /app/target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]