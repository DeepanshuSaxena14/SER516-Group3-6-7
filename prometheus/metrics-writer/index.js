import pkg from "pg";
const { Pool } = pkg;

// ── Configuration ────────────────────────────────────────────────────────────
const PROMETHEUS_URL = process.env.PROMETHEUS_URL || "http://prometheus:9090";
const WRITE_INTERVAL_MS = Number(process.env.WRITE_INTERVAL_MS) || 15000;

const pool = new Pool({
  connectionString: process.env.SUPABASE_DB_URL,
  ssl: { rejectUnauthorized: false },
});

// ── Metrics to collect ────────────────────────────────────────────────────────
// Each entry: { metric: PromQL expression, name: human label, job: service tag }
// The writer runs every WRITE_INTERVAL_MS and inserts one row per entry.
const METRICS = [
  // --- Node.js event loop lag ---
  { expr: 'nodejs_eventloop_lag_p50_seconds{job="middleware"}',  service: "middleware",  metric: "eventloop_lag_p50_seconds" },
  { expr: 'nodejs_eventloop_lag_p95_seconds{job="middleware"}',  service: "middleware",  metric: "eventloop_lag_p95_seconds" },
  { expr: 'nodejs_eventloop_lag_p99_seconds{job="middleware"}',  service: "middleware",  metric: "eventloop_lag_p99_seconds" },
  { expr: 'nodejs_eventloop_lag_p50_seconds{job="g7-pmd"}',      service: "g7-pmd",      metric: "eventloop_lag_p50_seconds" },
  { expr: 'nodejs_eventloop_lag_p95_seconds{job="g7-pmd"}',      service: "g7-pmd",      metric: "eventloop_lag_p95_seconds" },
  { expr: 'nodejs_eventloop_lag_p99_seconds{job="g7-pmd"}',      service: "g7-pmd",      metric: "eventloop_lag_p99_seconds" },

  // --- Node.js heap ---
  { expr: 'nodejs_heap_size_used_bytes{job="middleware"}',        service: "middleware",  metric: "heap_used_bytes" },
  { expr: 'nodejs_heap_size_used_bytes{job="g7-pmd"}',           service: "g7-pmd",      metric: "heap_used_bytes" },

  // --- CPU ---
  // Node.js prom-client uses process_cpu_seconds_total (counter) → rate() works
  { expr: 'rate(process_cpu_seconds_total{job="middleware"}[1m])', service: "middleware", metric: "cpu_usage_rate" },
  { expr: 'rate(process_cpu_seconds_total{job="g7-pmd"}[1m])',     service: "g7-pmd",     metric: "cpu_usage_rate" },
  // Java Micrometer uses process_cpu_usage (gauge, 0–1) → read directly, no rate()
  { expr: 'process_cpu_usage{job="g6-metrics"}',                   service: "g6-metrics", metric: "cpu_usage_rate" },

  // --- JVM heap (g6-metrics only) ---
  { expr: 'sum(jvm_memory_used_bytes{job="g6-metrics",area="heap"})', service: "g6-metrics", metric: "jvm_heap_used_bytes" },
  { expr: 'sum(jvm_memory_max_bytes{job="g6-metrics",area="heap"})',  service: "g6-metrics", metric: "jvm_heap_max_bytes" },

  // --- HTTP request rate (g6-metrics / Micrometer) ---
  {
    expr: 'sum(rate(http_server_requests_seconds_count{job="g6-metrics"}[1m]))',
    service: "g6-metrics",
    metric: "http_request_rate",
  },
  {
    expr: 'sum(rate(http_server_requests_seconds_count{job="g6-metrics",status=~"4..|5.."}[1m]))',
    service: "g6-metrics",
    metric: "http_error_rate",
  },

  // --- /analyze latency percentiles (g6-metrics) ---
  {
    expr: 'histogram_quantile(0.50, sum by(le)(rate(http_server_requests_seconds_bucket{job="g6-metrics",uri=~".*/analyze.*"}[1m])))',
    service: "g6-metrics",
    metric: "analyze_latency_p50_seconds",
  },
  {
    expr: 'histogram_quantile(0.95, sum by(le)(rate(http_server_requests_seconds_bucket{job="g6-metrics",uri=~".*/analyze.*"}[1m])))',
    service: "g6-metrics",
    metric: "analyze_latency_p95_seconds",
  },
  {
    expr: 'histogram_quantile(0.99, sum by(le)(rate(http_server_requests_seconds_bucket{job="g6-metrics",uri=~".*/analyze.*"}[1m])))',
    service: "g6-metrics",
    metric: "analyze_latency_p99_seconds",
  },
];

// ── Prometheus instant query ─────────────────────────────────────────────────
async function queryPrometheus(expr) {
  const url = `${PROMETHEUS_URL}/api/v1/query?query=${encodeURIComponent(expr)}`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Prometheus HTTP ${res.status} for: ${expr}`);
  const body = await res.json();
  if (body.status !== "success") throw new Error(`Prometheus error: ${body.error}`);
  // Return the first result value, or null if no data yet
  const results = body.data?.result ?? [];
  if (results.length === 0) return null;
  const raw = results[0].value?.[1];
  const value = parseFloat(raw);
  return isNaN(value) ? null : value;
}

// ── Write one batch to Supabase ───────────────────────────────────────────────
async function writeBatch() {
  const now = new Date();
  const rows = [];

  await Promise.all(
    METRICS.map(async ({ expr, service, metric }) => {
      try {
        const value = await queryPrometheus(expr);
        if (value !== null) {
          rows.push({ service, metric, value, scraped_at: now });
        }
      } catch (err) {
        console.warn(`[metrics-writer] skip ${metric} (${service}): ${err.message}`);
      }
    })
  );

  if (rows.length === 0) return;

  const client = await pool.connect();
  try {
    await client.query("BEGIN");
    for (const row of rows) {
      await client.query(
        `INSERT INTO observability_metrics (service, metric, value, scraped_at)
         VALUES ($1, $2, $3, $4)`,
        [row.service, row.metric, row.value, row.scraped_at]
      );
    }
    await client.query("COMMIT");
    console.log(`[metrics-writer] wrote ${rows.length} rows at ${now.toISOString()}`);
  } catch (err) {
    await client.query("ROLLBACK");
    console.error("[metrics-writer] DB write failed:", err.message);
  } finally {
    client.release();
  }
}

// ── Startup ───────────────────────────────────────────────────────────────────
console.log(`[metrics-writer] starting — Prometheus: ${PROMETHEUS_URL}, interval: ${WRITE_INTERVAL_MS}ms`);

// Initial write, then repeat
writeBatch();
setInterval(writeBatch, WRITE_INTERVAL_MS);
