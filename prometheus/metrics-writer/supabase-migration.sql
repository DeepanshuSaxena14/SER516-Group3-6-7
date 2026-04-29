-- Run this once in the Supabase SQL editor:
-- https://supabase.com/dashboard → your project → SQL Editor

-- ── Table ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS observability_metrics (
  id          BIGSERIAL    PRIMARY KEY,
  service     TEXT         NOT NULL,   -- 'g6-metrics' | 'middleware' | 'g7-pmd'
  metric      TEXT         NOT NULL,   -- e.g. 'eventloop_lag_p95_seconds'
  value       DOUBLE PRECISION NOT NULL,
  scraped_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── Index — speeds up all time-series queries in Grafana ─────────────────────
CREATE INDEX IF NOT EXISTS idx_obs_service_metric_time
  ON observability_metrics (service, metric, scraped_at DESC);

-- ── Optional: auto-purge rows older than 7 days ──────────────────────────────
-- Uncomment if you want to keep the table small automatically.
-- CREATE OR REPLACE FUNCTION purge_old_observability_metrics()
-- RETURNS void LANGUAGE sql AS $$
--   DELETE FROM observability_metrics WHERE scraped_at < NOW() - INTERVAL '7 days';
-- $$;
