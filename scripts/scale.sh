#!/bin/bash
SERVICE=$1
REPLICAS=$2

if [ -z "$SERVICE" ] || [ -z "$REPLICAS" ]; then
    echo "Usage: ./scripts/scale.sh <service> <replicas>"
    echo "Example: ./scripts/scale.sh kisakes 4"
    exit 1
fi

echo "📊 Scaling $SERVICE to $REPLICAS instances..."
docker-compose up -d --scale "${SERVICE}-app=$REPLICAS"
echo "✅ Scaling complete"
