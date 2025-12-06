#!/bin/bash

# Setup script for Microservices Platform
# This script prepares the environment for first-time setup

set -e

echo "================================"
echo "Microservices Platform Setup"
echo "================================"
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Check prerequisites
echo -e "${BLUE}[1/7] Checking prerequisites...${NC}"

if ! command -v docker &> /dev/null; then
    echo -e "${RED}✗ Docker is not installed${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Docker found: $(docker --version)${NC}"

if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}✗ Docker Compose is not installed${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Docker Compose found: $(docker-compose --version)${NC}"

# Create .env file if not exists
echo -e "${BLUE}[2/7] Setting up environment variables...${NC}"
if [ ! -f .env ]; then
    cp .env.example .env
    echo -e "${GREEN}✓ .env file created${NC}"
    echo -e "${YELLOW}⚠ Please edit .env file with your configuration${NC}"
else
    echo -e "${YELLOW}⚠ .env file already exists${NC}"
fi

# Create necessary directories
echo -e "${BLUE}[3/7] Creating directories...${NC}"
mkdir -p infrastructure/monitoring/grafana/dashboards
mkdir -p infrastructure/monitoring/grafana/datasources
mkdir -p infrastructure/monitoring/loki
mkdir -p infrastructure/monitoring/promtail
mkdir -p infrastructure/monitoring/prometheus
mkdir -p infrastructure/databases/postgres/init-scripts
mkdir -p logs
echo -e "${GREEN}✓ Directories created${NC}"

# Create init script for multiple databases
echo -e "${BLUE}[4/7] Creating database init script...${NC}"
cat > infrastructure/databases/postgres/init-scripts/01-init-databases.sh << 'EOF'
#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE kisakes;
    CREATE DATABASE dummydb;

    GRANT ALL PRIVILEGES ON DATABASE kisakes TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE dummydb TO $POSTGRES_USER;
EOSQL
EOF
chmod +x infrastructure/databases/postgres/init-scripts/01-init-databases.sh
echo -e "${GREEN}✓ Database init script created${NC}"

# Create Loki config
echo -e "${BLUE}[5/7] Creating Loki configuration...${NC}"
cat > infrastructure/monitoring/loki/loki-config.yml << 'EOF'
auth_enabled: false

server:
  http_listen_port: 3100

ingester:
  lifecycler:
    ring:
      kvstore:
        store: inmemory
      replication_factor: 1
  chunk_idle_period: 5m
  chunk_retain_period: 30s

schema_config:
  configs:
    - from: 2020-10-24
      store: boltdb
      object_store: filesystem
      schema: v11
      index:
        prefix: index_
        period: 168h

storage_config:
  boltdb:
    directory: /loki/index
  filesystem:
    directory: /loki/chunks

limits_config:
  enforce_metric_name: false
  reject_old_samples: true
  reject_old_samples_max_age: 168h

chunk_store_config:
  max_look_back_period: 0s

table_manager:
  retention_deletes_enabled: false
  retention_period: 0s
EOF
echo -e "${GREEN}✓ Loki config created${NC}"

# Create Promtail config
echo -e "${BLUE}[6/7] Creating Promtail configuration...${NC}"
cat > infrastructure/monitoring/promtail/promtail-config.yml << 'EOF'
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  - job_name: docker
    docker_sd_configs:
      - host: unix:///var/run/docker.sock
        refresh_interval: 5s
    relabel_configs:
      - source_labels: ['__meta_docker_container_name']
        regex: '/(.*)'
        target_label: 'container'
      - source_labels: ['__meta_docker_container_log_stream']
        target_label: 'logstream'
      - source_labels: ['__meta_docker_container_label_service']
        target_label: 'service'
    pipeline_stages:
      - json:
          expressions:
            level: level
            message: message
      - labels:
          level:
EOF
echo -e "${GREEN}✓ Promtail config created${NC}"

# Create Grafana datasource
echo -e "${BLUE}[7/7] Creating Grafana datasources...${NC}"
cat > infrastructure/monitoring/grafana/datasources/datasources.yml << 'EOF'
apiVersion: 1

datasources:
  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    isDefault: true
    jsonData:
      maxLines: 1000

  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: false
    jsonData:
      timeInterval: 5s
EOF
echo -e "${GREEN}✓ Grafana datasources created${NC}"

echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Setup completed successfully!${NC}"
echo -e "${GREEN}================================${NC}"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Edit .env file with your configuration"
echo "2. Run: ./scripts/start.sh"
echo "3. Access services:"
echo "   - Load Balancer: http://localhost:8080"
echo "   - Grafana: http://localhost:3000 (admin/admin123)"
echo "   - Prometheus: http://localhost:9090"
echo ""