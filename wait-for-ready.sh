#!/bin/bash

PMD_URL="http://localhost:4000"
GRAFANA_URL="http://localhost:3000"
INTERVAL=5
MAX_WAIT=300  # 5 minutes timeout
elapsed=0

echo ""
echo "========================================="
echo "  Waiting for services to be ready..."
echo "========================================="
echo ""

while true; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "$PMD_URL")

  if [ "$STATUS" -eq 200 ] 2>/dev/null || [ "$STATUS" -eq 404 ] 2>/dev/null; then
    echo ""
    echo "========================================="
    echo "     PMD Backend is UP!"
    echo ""
    echo "     Your services are ready:"
    echo "     • PMD Backend  -> $PMD_URL"
    echo "     • Grafana      -> $GRAFANA_URL"
    echo "     • Frontend     -> http://localhost:80"
    echo "     • Mongo API    -> http://localhost:4001"
    echo "========================================="
    echo ""
    exit 0
  fi

  if [ "$elapsed" -ge "$MAX_WAIT" ]; then
    echo ""
    echo "========================================="
    echo "     Timed out after ${MAX_WAIT}s."
    echo "     PMD backend did not respond at $PMD_URL"
    echo "     Try: docker compose logs g7-pmd"
    echo "========================================="
    echo ""
    exit 1
  fi

  echo "  PMD not ready yet (HTTP $STATUS)... retrying in ${INTERVAL}s  [${elapsed}s elapsed]"
  sleep "$INTERVAL"
  elapsed=$((elapsed + INTERVAL))
done