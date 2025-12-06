#!/bin/bash
echo "🧹 Cleaning Spectrum Platform..."
echo "⚠️  This will remove all containers, volumes, and networks!"
read -p "Are you sure? (yes/no): " -r
if [[ $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    docker-compose down -v --remove-orphans
    docker system prune -f
    echo "✅ Cleanup complete"
else
    echo "❌ Cleanup cancelled"
fi
