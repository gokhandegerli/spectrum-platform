#!/bin/bash
URL=${1:-"http://localhost:8080/dummy-service/hello"}
REQUESTS=${2:-1000}
CONCURRENCY=${3:-10}

echo "🚀 Load Testing Spectrum Platform"
echo "   URL: $URL"
echo "   Requests: $REQUESTS"
echo "   Concurrency: $CONCURRENCY"
echo ""

if command -v ab &> /dev/null; then
    ab -n $REQUESTS -c $CONCURRENCY "$URL"
else
    echo "⚠️  Apache Bench (ab) not found"
    echo "   Install: sudo apt-get install apache2-utils"
    echo ""
    echo "   Using curl fallback..."
    start_time=$(date +%s)
    for i in $(seq 1 $REQUESTS); do
        curl -s "$URL" > /dev/null &
        if [ $((i % CONCURRENCY)) -eq 0 ]; then
            wait
        fi
    done
    wait
    end_time=$(date +%s)
    duration=$((end_time - start_time))
    echo "✅ Completed $REQUESTS requests in $duration seconds"
    echo "   Rate: $((REQUESTS / duration)) req/s"
fi
