-- Main table to track each analysis run
CREATE TABLE IF NOT EXISTS analysis_runs (
    run_id         SERIAL PRIMARY KEY,
    repo_url       TEXT NOT NULL,
    branch         VARCHAR(255) DEFAULT 'main',
    commit_hash    VARCHAR(40),
    status         VARCHAR(20) NOT NULL DEFAULT 'running'
                   CHECK (status IN ('running', 'completed', 'failed')),
    error_message  TEXT,
    started_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at   TIMESTAMP WITH TIME ZONE
);

-- Fan-out metrics table for Grafana (snapshot + time series)
CREATE TABLE IF NOT EXISTS fan_out_metrics (
    id          SERIAL PRIMARY KEY,
    run_id      INTEGER REFERENCES analysis_runs(run_id),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    class_name  VARCHAR(1024) NOT NULL,
    scope       VARCHAR(64) NOT NULL DEFAULT 'class',
    fan_out     INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_fan_out_recorded_at ON fan_out_metrics (recorded_at);
CREATE INDEX IF NOT EXISTS idx_fan_out_class ON fan_out_metrics (class_name, recorded_at);
CREATE INDEX IF NOT EXISTS idx_fan_out_run_id ON fan_out_metrics (run_id);

-- Fan-in metrics table for Grafana (snapshot + time series)
CREATE TABLE IF NOT EXISTS fan_in_metrics (
    id          SERIAL PRIMARY KEY,
    run_id      INTEGER REFERENCES analysis_runs(run_id),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    class_name  VARCHAR(1024) NOT NULL,
    scope       VARCHAR(64) NOT NULL DEFAULT 'class',
    fan_in      INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_fan_in_recorded_at ON fan_in_metrics (recorded_at);
CREATE INDEX IF NOT EXISTS idx_fan_in_class ON fan_in_metrics (class_name, recorded_at);
CREATE INDEX IF NOT EXISTS idx_fan_in_run_id ON fan_in_metrics (run_id);

-- Afferent/Efferent metrics table for Grafana (snapshot + time series)
CREATE TABLE IF NOT EXISTS afferent_efferent_result (
    id          SERIAL PRIMARY KEY,
    run_id      INTEGER REFERENCES analysis_runs(run_id),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    class_name  VARCHAR(1024) NOT NULL,
    afferent    INTEGER NOT NULL,
    efferent    INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ae_recorded_at ON afferent_efferent_result (recorded_at);
CREATE INDEX IF NOT EXISTS idx_ae_class ON afferent_efferent_result (class_name, recorded_at);
CREATE INDEX IF NOT EXISTS idx_ae_run_id ON afferent_efferent_result (run_id);

-- PMD Defects metrics
CREATE TABLE IF NOT EXISTS pmd_metrics (
    id          SERIAL PRIMARY KEY,
    run_id      INTEGER REFERENCES analysis_runs(run_id),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    file_name   VARCHAR(1024) NOT NULL,
    rule        VARCHAR(1024) NOT NULL,
    priority    INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pmd_run_id ON pmd_metrics (run_id);
CREATE INDEX IF NOT EXISTS idx_pmd_file ON pmd_metrics (file_name, recorded_at);
