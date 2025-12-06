#!/bin/bash

# =============================================================================
# Spectrum Platform - Build All Services
# This script builds all modules in the correct order
# =============================================================================

set -e  # Exit on error

echo "🌈 =========================================="
echo "   Spectrum Platform Build Script"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Check prerequisites
echo -e "${BLUE}[1/5] Checking prerequisites...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}✗ Java is not installed${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Java found: $(java -version 2>&1 | head -n 1)${NC}"

if ! command -v mvn &> /dev/null && [ ! -f "./mvnw" ]; then
    echo -e "${RED}✗ Maven is not installed and mvnw not found${NC}"
    exit 1
fi

# Determine maven command
if [ -f "./mvnw" ]; then
    MVN="./mvnw"
else
    MVN="mvn"
fi
echo -e "${GREEN}✓ Maven found${NC}"
echo ""

# Clean previous builds
echo -e "${BLUE}[2/5] Cleaning previous builds...${NC}"
$MVN clean -q
echo -e "${GREEN}✓ Clean complete${NC}"
echo ""

# Build parent POM (install to local repo)
echo -e "${BLUE}[3/5] Building parent POM...${NC}"
$MVN install -N -DskipTests -q
echo -e "${GREEN}✓ Parent POM installed to local repository${NC}"
echo ""

# Build all modules
echo -e "${BLUE}[4/5] Building all modules...${NC}"
echo ""

echo -e "${YELLOW}Building Load Balancer...${NC}"
cd infrastructure/load-balancer
../../mvnw clean package -DskipTests
echo -e "${GREEN}✓ Load Balancer built successfully${NC}"
cd ../..
echo ""

echo -e "${YELLOW}Building Kisakes Service...${NC}"
cd services/kisakes
../../mvnw clean package -DskipTests
echo -e "${GREEN}✓ Kisakes built successfully${NC}"
cd ../..
echo ""

echo -e "${YELLOW}Building Dummy Service...${NC}"
cd services/dummy-service
../../mvnw clean package -DskipTests
echo -e "${GREEN}✓ Dummy Service built successfully${NC}"
cd ../..
echo ""

# Verify JARs
echo -e "${BLUE}[5/5] Verifying JAR files...${NC}"

check_jar() {
    local path=$1
    local name=$2

    if ls $path/*.jar 1> /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} $name: $(ls $path/*.jar | xargs basename)"
    else
        echo -e "${RED}✗${NC} $name: JAR not found at $path"
        return 1
    fi
}

check_jar "infrastructure/load-balancer/target" "Load Balancer"
check_jar "services/kisakes/target" "Kisakes"
check_jar "services/dummy-service/target" "Dummy Service"

echo ""
echo -e "${GREEN}=========================================="
echo "   ✅ Build completed successfully!"
echo "==========================================${NC}"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo "  1. Run: docker-compose build"
echo "  2. Run: docker-compose up -d"
echo "  3. Run: ./scripts/health-check.sh"
echo ""