#!/bin/bash

# Start all services
set -e

echo "================================"
echo "Starting Microservices Platform"
echo "================================"
echo ""

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check if .env exists
if [ ! -f .env ]; then
    echo -e "${YELLOW}⚠ .env file not found. Running setup...${NC}"
    ./scripts/setup.sh
fi

echo -e "${BLUE}[1/4] Pulling latest images...${NC}"
docker-compose pull

echo -e "${BLUE}[2/4] Building services...${NC}"
docker-compose build

echo -e "${BLUE}[3/4] Starting services...${NC}"
docker-compose up -d

echo -e "${BLUE}[4/4] Waiting for services to be healthy...${NC}"
echo "This may take a few minutes..."

# Wait for services
max_attempts=60
attempt=0

while [ $attempt -lt $max_attempts ]; do
    attempt=$((attempt + 1))

    # Check if all services are healthy
    healthy=$(docker-compose ps | grep -c "healthy" || true)
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
    echo "Run: docker-compose ps"
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
echo "  View logs:      docker-compose logs -f"
echo "  View status:    docker-compose ps"
echo "  Stop services:  ./scripts/stop.sh"
echo "  Health check:   ./scripts/health-check.sh"
echo ""