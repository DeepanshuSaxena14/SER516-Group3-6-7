CREATE TABLE IF NOT EXISTS fan_out_metrics (
    id          SERIAL PRIMARY KEY,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    class_name  VARCHAR(1024) NOT NULL,
    scope       VARCHAR(64) NOT NULL DEFAULT 'class',
    fan_out     INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_fan_out_recorded_at ON fan_out_metrics (recorded_at);
CREATE INDEX IF NOT EXISTS idx_fan_out_class ON fan_out_metrics (class_name, recorded_at);

CREATE TABLE IF NOT EXISTS fan_in_metrics (
    id          SERIAL PRIMARY KEY,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    class_name  VARCHAR(1024) NOT NULL,
    scope       VARCHAR(64) NOT NULL DEFAULT 'class',
    fan_in      INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_fan_in_recorded_at  ON fan_in_metrics (recorded_at);
CREATE INDEX IF NOT EXISTS idx_fan_in_class        ON fan_in_metrics (class_name, recorded_at);
CREATE INDEX IF NOT EXISTS idx_fan_in_scope        ON fan_in_metrics (scope, recorded_at);
CREATE INDEX IF NOT EXISTS idx_fan_out_scope       ON fan_out_metrics (scope, recorded_at);