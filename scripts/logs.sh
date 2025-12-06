#!/bin/bash
SERVICE=$1

if [ -z "$SERVICE" ]; then
    echo "📝 Showing all logs (Ctrl+C to exit)..."
    docker-compose logs -f
else
    echo "📝 Showing logs for $SERVICE..."
    docker-compose logs -f "$SERVICE"
fi
