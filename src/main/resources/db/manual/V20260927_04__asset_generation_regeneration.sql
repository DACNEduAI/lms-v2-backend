BEGIN;

ALTER TABLE edu.asset_generation_jobs
    ADD COLUMN IF NOT EXISTS generation_mode VARCHAR(32) NOT NULL DEFAULT 'INITIAL',
    ADD COLUMN IF NOT EXISTS output_storage_key VARCHAR(1024);

ALTER TABLE edu.asset_generation_jobs
    DROP CONSTRAINT IF EXISTS ck_asset_generation_mode;
ALTER TABLE edu.asset_generation_jobs
    ADD CONSTRAINT ck_asset_generation_mode CHECK (generation_mode IN ('INITIAL', 'REGENERATE'));

UPDATE edu.asset_generation_jobs g
SET output_storage_key = i.storage_key
FROM edu.image_assets i
WHERE g.asset_id = i.asset_id
  AND g.output_storage_key IS NULL;

ALTER TABLE edu.asset_generation_jobs
    ALTER COLUMN output_storage_key SET NOT NULL;

COMMIT;
