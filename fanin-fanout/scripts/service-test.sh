#!/usr/bin/env bash

curl_code() {
  local url=$1
  curl -s -o /dev/null -w "%{http_code}" "$url" || echo "000"
}

RESPONSE=$(curl_code "http://localhost:8080/metrics/fanout?path=/input/Simple-Java-Calculator/src")
if [[ "$RESPONSE" != "200" ]]; then
  echo "Service test FAILED — expected 200 from metrics endpoint, got $RESPONSE"
  exit 1
fi

RESPONSE=$(curl_code "http://localhost:3000/api/health")
if [[ "$RESPONSE" != "200" ]]; then
  echo "Service test FAILED — expected 200 from Grafana health, got $RESPONSE"
  exit 1
fi

OUT_DIR=${OUT_DIR:-"out"}
if [[ ! -f "$OUT_DIR/fanout.json" || ! -f "$OUT_DIR/fanout.csv" ]]; then
  echo "Service test FAILED — expected output files fanout.json and fanout.csv in $OUT_DIR"
  exit 1
fi

DASHBOARD_FILE=${DASHBOARD_FILE:-"grafana/provisioning/dashboards/fan-in-fan-out.json"}
if [[ ! -f "$DASHBOARD_FILE" ]]; then
  echo "Service test FAILED — expected Grafana dashboard definition at $DASHBOARD_FILE"
  exit 1
fi

echo "Service test PASSED"

