ALTER TABLE edu.code_runner_configs
    ADD COLUMN IF NOT EXISTS wall_time_limit_ms INTEGER NOT NULL DEFAULT 5000
        CHECK (wall_time_limit_ms BETWEEN 100 AND 30000),
    ADD COLUMN IF NOT EXISTS memory_limit_kb INTEGER NOT NULL DEFAULT 131072
        CHECK (memory_limit_kb BETWEEN 16384 AND 1048576);
