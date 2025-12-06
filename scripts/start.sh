#!/bin/bash

# Start all services
set -e

echo "================================="
echo "Starting Spectrum Platform"
echo "================================="
echo ""

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Check if .env exists
if [ ! -f .env ]; then
    echo -e "${YELLOW}⚠ .env file not found. Running setup...${NC}"
    ./scripts/setup.sh
fi

echo -e "${BLUE}[1/5] Pulling latest images...${NC}"
docker-compose pull

echo -e "${BLUE}[2/5] Building project JARs...${NC}"
# Use Maven Wrapper from project root
if [ -f "./mvnw" ]; then
    ./mvnw clean install --settings ./settings.xml -DskipTests
elif command -v mvn &> /dev/null; then
    mvn clean install -DskipTests
else
    echo -e "${RED}✗ Maven not found. Please install Maven or ensure mvnw is present.${NC}"
    exit 1
fi

echo -e "${BLUE}[3/5] Building Docker images...${NC}"
docker-compose build

echo -e "${BLUE}[4/5] Starting services...${NC}"
docker-compose up -d

echo -e "${BLUE}[5/5] Waiting for services to be healthy...${NC}"
echo "This may take a few minutes..."

# Wait for services
max_attempts=60
attempt=0

while [ $attempt -lt $max_attempts ]; do
    attempt=$((attempt + 1))

    # Check if all critical services are healthy
    healthy=$(docker-compose ps --filter "health=healthy" | grep -c "healthy" || true)
    total=$(docker-compose ps --services | wc -l)

    echo -ne "\rAttempt $attempt/$max_attempts - Healthy: $healthy/$total"

    if [ "$healthy" -ge 8 ]; then
        echo ""
        echo -e "${GREEN}✓ All critical services are healthy!${NC}"
        break
    fi

    sleep 5
done

if [ $attempt -eq $max_attempts ]; then
    echo ""
    echo -e "${YELLOW}⚠ Some services may not be healthy yet${NC}"
    echo "Check status: docker-compose ps"
    echo "View logs: docker-compose logs"
fi

echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Platform started successfully!${NC}"
echo -e "${GREEN}================================${NC}"
echo ""
echo -e "${BLUE}Service URLs:${NC}"
echo "  Load Balancer:  http://localhost:8080"
echo "  Admin API:      http://localhost:8080/admin/status"
echo "  Grafana:        http://localhost:3000 (admin/admin123)"
echo "  Prometheus:     http://localhost:9090"
echo ""
echo -e "${BLUE}Service Routes (via Load Balancer):${NC}"
echo "  Kisakes:        http://localhost:8080/kisakes/..."
echo "  Dummy Service:  http://localhost:8080/dummy-service/..."
echo ""
echo -e "${BLUE}Useful Commands:${NC}"
echo "  View logs:      docker-compose logs -f [service]"
echo "  View status:    docker-compose ps"
echo "  Stop services:  ./scripts/stop.sh"
echo "  Health check:   ./scripts/health-check.sh"
echo ""