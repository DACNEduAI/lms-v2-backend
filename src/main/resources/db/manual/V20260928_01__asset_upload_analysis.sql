BEGIN;

ALTER TABLE edu.asset_generation_jobs
    DROP CONSTRAINT IF EXISTS ck_asset_generation_mode;
ALTER TABLE edu.asset_generation_jobs
    ADD CONSTRAINT ck_asset_generation_mode
    CHECK (generation_mode IN ('INITIAL', 'REGENERATE', 'UPLOAD_ANALYSIS'));

COMMIT;
