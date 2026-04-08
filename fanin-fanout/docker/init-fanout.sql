-- Fan-out metrics table for Grafana (snapshot + time series)
CREATE TABLE IF NOT EXISTS fan_out_metrics (
    id          SERIAL PRIMARY KEY,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    class_name  VARCHAR(1024) NOT NULL,
    scope       VARCHAR(64) NOT NULL DEFAULT 'class',
    fan_out     INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_fan_out_recorded_at ON fan_out_metrics (recorded_at);
CREATE INDEX IF NOT EXISTS idx_fan_out_class ON fan_out_metrics (class_name, recorded_at);

-- Fan-in metrics table for Grafana (snapshot + time series)
CREATE TABLE IF NOT EXISTS fan_in_metrics (
    id          SERIAL PRIMARY KEY,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    class_name  VARCHAR(1024) NOT NULL,
    scope       VARCHAR(64) NOT NULL DEFAULT 'class',
    fan_in      INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_fan_in_recorded_at ON fan_in_metrics (recorded_at);
CREATE INDEX IF NOT EXISTS idx_fan_in_class ON fan_in_metrics (class_name, recorded_at);

-- Afferent/Efferent metrics table for Grafana (snapshot + time series)
CREATE TABLE IF NOT EXISTS afferent_efferent_results (
    id          SERIAL PRIMARY KEY,
    run_id      INTEGER NOT NULL REFERENCES analysis_runs(run_id),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    class_name  VARCHAR(1024) NOT NULL,
    afferent    INTEGER NOT NULL,
    efferent    INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ae_recorded_at ON afferent_efferent_results (recorded_at);
CREATE INDEX IF NOT EXISTS idx_ae_class ON afferent_efferent_results (class_name, recorded_at);
CREATE INDEX IF NOT EXISTS idx_ae_run_id ON afferent_efferent_results (run_id);
