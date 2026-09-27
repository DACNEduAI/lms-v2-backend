BEGIN;

-- This release deliberately retires every grouped CCCT/Matrix source and its
-- generated Quiz3 descendants.  Do not run it on a database whose quiz history
-- must be retained.
CREATE TEMP TABLE retiring_matrices AS SELECT id FROM edu.exam_matrices;
CREATE TEMP TABLE retiring_assessments AS
  SELECT id FROM edu.quiz3_assessments WHERE matrix_id IN (SELECT id FROM retiring_matrices);
CREATE TEMP TABLE retiring_quizzes AS
  SELECT quiz_id FROM edu.quiz3_assessment_variants WHERE assessment_id IN (SELECT id FROM retiring_assessments) AND quiz_id IS NOT NULL;

DELETE FROM edu.quiz3_question_provenance WHERE generation_job_id IN (SELECT id FROM edu.quiz3_generation_jobs WHERE assessment_id IN (SELECT id FROM retiring_assessments));
DELETE FROM edu.quiz3_generation_jobs WHERE assessment_id IN (SELECT id FROM retiring_assessments) OR matrix_id IN (SELECT id FROM retiring_matrices);
DELETE FROM edu.quiz3_assessments WHERE id IN (SELECT id FROM retiring_assessments);
DELETE FROM edu.quizzes WHERE id IN (SELECT quiz_id FROM retiring_quizzes);
DELETE FROM edu.exam_matrices;
DELETE FROM edu.ccct_profiles;

DROP TABLE IF EXISTS edu.ccct_revision_items;
DROP TABLE IF EXISTS edu.ccct_revision_topics;
DROP TABLE IF EXISTS edu.ccct_revision_chapters;
DROP TABLE IF EXISTS edu.ccct_profile_revisions;
DROP TABLE IF EXISTS edu.ccct_items;
DROP TABLE IF EXISTS edu.ccct_topics;
DROP TABLE IF EXISTS edu.ccct_chapters;

CREATE TABLE edu.ccct_chapters (
  id UUID PRIMARY KEY, profile_id UUID NOT NULL REFERENCES edu.ccct_profiles(id) ON DELETE CASCADE,
  course_chapter_id UUID REFERENCES edu.chapters(chapter_id) ON DELETE SET NULL,
  chapter_code VARCHAR(64) NOT NULL CHECK (chapter_code ~ '^[a-z][A-Za-z0-9]*$'),
  title VARCHAR(255) NOT NULL CHECK (btrim(title)<>''), order_index INTEGER NOT NULL CHECK(order_index>=0),
  CONSTRAINT uq_ccct_chapter_order UNIQUE(profile_id,order_index), CONSTRAINT uq_ccct_chapter_code UNIQUE(profile_id,chapter_code));
CREATE TABLE edu.ccct_items (
  id UUID PRIMARY KEY, chapter_id UUID NOT NULL REFERENCES edu.ccct_chapters(id) ON DELETE CASCADE,
  content_code VARCHAR(128) NOT NULL CHECK(content_code ~ '^[a-z][A-Za-z0-9]*\.[a-z][A-Za-z0-9]*$'),
  content TEXT NOT NULL CHECK(btrim(content)<>''), order_index INTEGER NOT NULL CHECK(order_index>=0),
  CONSTRAINT uq_ccct_item_order UNIQUE(chapter_id,order_index), CONSTRAINT uq_ccct_item_code UNIQUE(chapter_id,content_code));
CREATE INDEX idx_ccct_chapters_profile ON edu.ccct_chapters(profile_id);
CREATE INDEX idx_ccct_items_chapter ON edu.ccct_items(chapter_id);

COMMIT;
