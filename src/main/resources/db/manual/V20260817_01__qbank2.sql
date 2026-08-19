BEGIN;

CREATE TABLE IF NOT EXISTS edu.question_bank_2_code_counters (
    course_id UUID PRIMARY KEY REFERENCES edu.courses(id) ON DELETE CASCADE,
    next_value INTEGER NOT NULL CHECK(next_value > 0)
);

CREATE TABLE IF NOT EXISTS edu.question_bank_2_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES edu.courses(id) ON DELETE CASCADE,
    code VARCHAR(64) NOT NULL,
    code_sequence INTEGER NOT NULL CHECK(code_sequence > 0),
    name VARCHAR(255) NOT NULL CHECK(btrim(name)<>''),
    description TEXT NOT NULL CHECK(btrim(description)<>''),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(course_id, code),
    UNIQUE(course_id, code_sequence)
);

CREATE TABLE IF NOT EXISTS edu.question_bank_2_examples (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES edu.question_bank_2_groups(id) ON DELETE CASCADE,
    content TEXT NOT NULL CHECK(btrim(content)<>''),
    sequence INTEGER NOT NULL CHECK(sequence > 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(group_id, sequence)
);

CREATE INDEX IF NOT EXISTS question_bank_2_groups_course_idx ON edu.question_bank_2_groups(course_id, code_sequence);
CREATE INDEX IF NOT EXISTS question_bank_2_examples_group_idx ON edu.question_bank_2_examples(group_id, sequence);

COMMIT;
