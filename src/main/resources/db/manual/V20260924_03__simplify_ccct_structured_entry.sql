-- CCCT now supports only direct Structured Entry and Markdown editing.
-- Historical PDF objects under ccct-ai/ are intentionally left untouched.

DROP TABLE IF EXISTS edu.ccct_ai_jobs;

ALTER TABLE edu.ccct_chapters
    DROP COLUMN IF EXISTS custom_content;

ALTER TABLE edu.ccct_items
    DROP COLUMN IF EXISTS coverage_status,
    DROP COLUMN IF EXISTS note;

UPDATE edu.ccct_profiles
SET last_mode = 'STRUCTURED'
WHERE last_mode IN ('PDF', 'CUSTOM');

UPDATE edu.ccct_profiles
SET markdown_content = regexp_replace(
    regexp_replace(markdown_content, E'\\s+—\\s+(Learn and exam|Learn, no exam|Do not learn)', '', 'g'),
    E'\\n\\s{2,}-\\s+(Note|Ghi chú):[^\\n]*', '', 'g');

ALTER TABLE edu.ccct_profiles
    DROP CONSTRAINT IF EXISTS ccct_profiles_last_mode_check;

ALTER TABLE edu.ccct_profiles
    ADD CONSTRAINT ccct_profiles_last_mode_check
    CHECK (last_mode IN ('STRUCTURED', 'MARKDOWN'));
