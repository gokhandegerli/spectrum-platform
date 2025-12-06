#!/bin/bash

# Health check all services

echo "================================"
echo "Microservices Platform Health Check"
echo "================================"
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

check_endpoint() {
    local name=$1
    local url=$2

    if curl -sf "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} $name"
        return 0
    else
        echo -e "${RED}✗${NC} $name"
        return 1
    fi
}

check_docker_container() {
    local name=$1
    local status=$(docker inspect -f '{{.State.Health.Status}}' "$name" 2>/dev/null || echo "not found")

    if [ "$status" = "healthy" ]; then
        echo -e "${GREEN}✓${NC} $name (healthy)"
        return 0
    elif [ "$status" = "not found" ]; then
        echo -e "${RED}✗${NC} $name (not found)"
        return 1
    else
        echo -e "${YELLOW}⚠${NC} $name ($status)"
        return 1
    fi
}

echo -e "${BLUE}Docker Containers:${NC}"
check_docker_container "postgres-db"
check_docker_container "redis-cache"
check_docker_container "kisakes-app-1"
check_docker_container "kisakes-app-2"
check_docker_container "dummy-service-1"
check_docker_container "dummy-service-2"
check_docker_container "load-balancer"
check_docker_container "loki"
check_docker_container "prometheus"
check_docker_container "grafana"

echo ""
echo -e "${BLUE}HTTP Endpoints:${NC}"
check_endpoint "Load Balancer" "http://localhost:8080/actuator/health"
check_endpoint "Load Balancer Admin" "http://localhost:8080/admin/status"
check_endpoint "Kisakes (via LB)" "http://localhost:8080/kisakes/actuator/health"
check_endpoint "Dummy Service (via LB)" "http://localhost:8080/dummy-service/actuator/health"
check_endpoint "Grafana" "http://localhost:3000/api/health"
check_endpoint "Prometheus" "http://localhost:9090/-/healthy"
check_endpoint "Loki" "http://localhost:3100/ready"

echo ""
echo -e "${BLUE}Database Connections:${NC}"
if docker exec postgres-db pg_isready -U admin > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} PostgreSQL"
else
    echo -e "${RED}✗${NC} PostgreSQL"
fi

if docker exec redis-cache redis-cli ping > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Redis"
else
    echo -e "${RED}✗${NC} Redis"
fi

echo ""
echo -e "${BLUE}Load Balancer Status:${NC}"
curl -s http://localhost:8080/admin/features | jq -r 'to_entries[] | "\(.key): \(.value.enabled // "N/A")"' 2>/dev/null || echo "Unable to fetch features"

echo ""
echo -e "${BLUE}Service Statistics:${NC}"
curl -s http://localhost:8080/admin/status | jq -r 'to_entries[] | "\(.key): \(.value.healthyCount)/\(.value.totalServers) healthy"' 2>/dev/null || echo "Unable to fetch statistics"

echo ""
echo "================================"
echo "Health Check Complete"
echo "================================"