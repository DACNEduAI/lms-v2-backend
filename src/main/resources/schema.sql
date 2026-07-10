CREATE SCHEMA IF NOT EXISTS edu;

-- Set the default schema for the rest of this script!
SET search_path TO edu;

-- Drop existing tables to start fresh on every application boot
DROP TABLE IF EXISTS
    users,
    students,
    teachers,
    courses,
    chapters,
    materials,
    chapter_materials,
    chapter_learning_outcomes,
    learning_outcomes,
    group_learning_outcomes,
    enrollments,
    ai_settings,
    quizzes,
    questions,
    question_images,
    answers,
    attempts,
    quiz_generation_jobs,
    quiz_generation_job_learning_outcomes,
    quiz_chapters,
    quiz_learning_outcomes,
    question_learning_outcomes,
    student_learning_outcomes,
    activity_logs,
    course_announcements,
    event_publication,
    ai_course_chat_messages,
    ai_chat_sessions,
    notifications,
    user_chat_messages,
    exam_matrices,
    exam_matrix_rows,
    CASCADE;

-- Below is the actual schema of the code

CREATE TABLE IF NOT EXISTS users
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name  VARCHAR(255),
    avatar_url VARCHAR(255),
    role       VARCHAR(50),
    created_at TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS students
(
-- Define the column, make it the Primary Key, and reference the users table
    user_id         UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    student_id      VARCHAR(255) NOT NULL UNIQUE,
    enrollment_year BIGINT       NOT NULL

);

CREATE TABLE IF NOT EXISTS teachers
(
-- Define the column, make it the Primary Key, and reference the users table
    user_id        UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    employee_id    VARCHAR(255) NOT NULL UNIQUE,
    phone_number   VARCHAR(255),
    department     VARCHAR(255),
    specialization VARCHAR(255)
);

-- Courses table
CREATE TABLE IF NOT EXISTS courses
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    course_name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    course_code VARCHAR(255) NOT NULL UNIQUE,
    active      BOOLEAN      NOT NULL DEFAULT true,
    cover_image VARCHAR(255),
    start_date  TIMESTAMP    NOT NULL,
    end_date    TIMESTAMP    NOT NULL,
    created_at  TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP             DEFAULT CURRENT_TIMESTAMP
);

-- Chapters table
CREATE TABLE IF NOT EXISTS chapters
(
    chapter_id  UUID      DEFAULT gen_random_uuid() UNIQUE,
    title       VARCHAR(255),
    description TEXT,
    course_id   UUID NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (chapter_id)
);

-- Materials table
CREATE TABLE IF NOT EXISTS materials
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(255),
    type            VARCHAR(50) CHECK (type IN ('PDF', 'VIDEO', 'LINK')), -- Added CHECK constraint here
    file_key        VARCHAR(255) UNIQUE,
    is_downloadable BOOLEAN          DEFAULT false,
    status          VARCHAR(50)      DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'UPLOADED', 'FAILED')),
    course_id       UUID,
    created_at      TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS chapter_materials
(
    chapter_id  UUID NOT NULL REFERENCES chapters (chapter_id) ON DELETE CASCADE,

    -- Reference to material table
    material_id UUID NOT NULL REFERENCES materials (id) ON DELETE CASCADE,

    PRIMARY KEY (material_id, chapter_id)
);


-- Group Learning Outcomes table (weak entity dependent on Course)
CREATE TABLE IF NOT EXISTS group_learning_outcomes
(
    id          UUID UNIQUE DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    course_id   UUID         NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    created_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, course_id)
);

-- Learning outcomes table (weak entity dependent on Course)
CREATE TABLE IF NOT EXISTS learning_outcomes
(
    id                        UUID UNIQUE DEFAULT gen_random_uuid(),
    code                      VARCHAR(255),
    description               TEXT,
    course_id                 UUID NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    group_learning_outcome_id UUID,
    created_at                TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, course_id),
    FOREIGN KEY (group_learning_outcome_id, course_id) REFERENCES group_learning_outcomes (id, course_id) ON DELETE SET NULL (group_learning_outcome_id)
);

-- Junction table for many-to-many: chapters <-> learning_outcomes
CREATE TABLE IF NOT EXISTS chapter_learning_outcomes
(
    chapter_id          UUID NOT NULL REFERENCES chapters (chapter_id) ON DELETE CASCADE,
    learning_outcome_id UUID NOT NULL REFERENCES learning_outcomes (id) ON DELETE CASCADE,
    PRIMARY KEY (chapter_id, learning_outcome_id)
);

-- Exam Matrices
CREATE TABLE IF NOT EXISTS exam_matrices
(
    id          uuid                        not null default gen_random_uuid(),
    course_id   uuid                        not null,
    title       character varying           not null,
    description text                        null,
    created_at  timestamp without time zone null     default CURRENT_TIMESTAMP,
    updated_at  timestamp without time zone null     default CURRENT_TIMESTAMP,
    constraint exam_matrices_pkey primary key (id),
    constraint exam_matrices_course_id_fkey foreign KEY (course_id) references courses (id) on delete CASCADE
) TABLESPACE pg_default;

CREATE TABLE IF NOT EXISTS exam_matrix_rows
(
    id                  uuid              not null default gen_random_uuid(),
    matrix_id           uuid              not null,
    learning_outcome_id uuid              not null,
    chapter_id          uuid              null,
    topic               character varying null,
    understand_count    integer           not null default 0,
    apply_count         integer           not null default 0,
    apply_plus_count    integer           not null default 0,
    order_index         integer           not null,
    constraint exam_matrix_rows_pkey primary key (id),
    constraint exam_matrix_rows_chapter_id_fkey foreign KEY (chapter_id) references chapters (chapter_id) on delete set null,
    constraint exam_matrix_rows_learning_outcome_id_fkey foreign KEY (learning_outcome_id) references learning_outcomes (id) on delete CASCADE,
    constraint exam_matrix_rows_matrix_id_fkey foreign KEY (matrix_id) references exam_matrices (id) on delete CASCADE
) TABLESPACE pg_default;

-- Enrollments table
CREATE TABLE IF NOT EXISTS enrollments
(
    course_id     UUID NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    user_id       UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    enrolled_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (course_id, user_id)
);

-- AI Settings table (single table for both student and teacher settings)
CREATE TABLE IF NOT EXISTS ai_settings
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    generation_prompt   TEXT,
    difficulty_level    VARCHAR(50),
    focus_on_weak_areas BOOLEAN          DEFAULT false,
    number_of_questions INTEGER,
    setting_type        VARCHAR(50),
    align_to_clo        BOOLEAN,
    include_answer_key  BOOLEAN,
    mode                VARCHAR(50),
    created_at          TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

-- Quizzes table
CREATE TABLE IF NOT EXISTS quizzes
(
    id                 UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    quiz_title         VARCHAR(255) NOT NULL,
    unlimited          BOOLEAN      NOT NULL DEFAULT false,
    time_limit_minutes INTEGER,
    pass_score         DOUBLE PRECISION,
    start_time         TIMESTAMP,
    due_time           TIMESTAMP,
    attempt_limited    INTEGER,
    ai_settings_id     UUID REFERENCES ai_settings (id),
    course_id          UUID         NOT NULL REFERENCES courses (id),
    student_id         UUID REFERENCES students (user_id),
    teacher_id         UUID REFERENCES teachers (user_id),
    display            BOOLEAN      NOT NULL DEFAULT true,
    created_at         TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP             DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS quiz_generation_jobs
(
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id          UUID        NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    chapter_id         UUID        REFERENCES chapters (chapter_id) ON DELETE SET NULL,
    quiz_id            UUID        REFERENCES quizzes (id) ON DELETE SET NULL,
    requested_by       UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    requested_role     VARCHAR(50) NOT NULL,
    quiz_title         VARCHAR(255),
    question_count     INTEGER,
    question_type      VARCHAR(50),
    prompt             TEXT,
    use_options        BOOLEAN,
    unlimited          BOOLEAN,
    time_limit_minutes INTEGER,
    pass_score         DOUBLE PRECISION,
    start_time         TIMESTAMP,
    due_time           TIMESTAMP,
    attempt_limited    INTEGER,
    display            BOOLEAN,
    status             VARCHAR(30) NOT NULL,
    error_message      TEXT,
    created_at         TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS quiz_generation_job_learning_outcomes
(
    job_id              UUID REFERENCES quiz_generation_jobs (id) ON DELETE CASCADE,
    learning_outcome_id UUID REFERENCES learning_outcomes (id) ON DELETE CASCADE,
    PRIMARY KEY (job_id, learning_outcome_id)
);

CREATE INDEX IF NOT EXISTS idx_quiz_generation_jobs_status
    ON quiz_generation_jobs (status);

CREATE INDEX IF NOT EXISTS idx_quiz_generation_jobs_requested_by
    ON quiz_generation_jobs (requested_by);

-- Questions table
CREATE TABLE IF NOT EXISTS questions
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content    TEXT,
    type       VARCHAR(50),
    quiz_id    UUID REFERENCES quizzes (id) ON DELETE CASCADE,
    created_at TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS question_images
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID         NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    image_name  VARCHAR(255) NOT NULL,
    url         TEXT         NOT NULL,
    created_at  TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_question_images_question_id ON question_images (question_id);

-- Answers table (weak entity dependent on Question)
CREATE TABLE IF NOT EXISTS answers
(
    answer_number BIGINT GENERATED ALWAYS AS IDENTITY,
    content       VARCHAR(255),
    is_correct    BOOLEAN   DEFAULT false,
    explanation   TEXT,
    question_id   UUID NOT NULL REFERENCES questions (id),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (answer_number, question_id)
);

-- Attempts table
CREATE TABLE IF NOT EXISTS attempts
(
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id         UUID        NOT NULL REFERENCES students (user_id),
    quiz_id            UUID        NOT NULL REFERENCES quizzes (id),
    score              DOUBLE PRECISION,
    start_time         TIMESTAMP,
    due_time           TIMESTAMP,
    submitted_time     TIMESTAMP,
    time_spent_seconds INTEGER,
    status             VARCHAR(50) NOT NULL CHECK (status IN ('STARTED', 'FINISHED', 'ERROR')),
    error_reason       VARCHAR(255),
    created_at         TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_attempts_student_quiz_status ON attempts (student_id, quiz_id, status);
CREATE INDEX IF NOT EXISTS idx_attempts_due_time ON attempts (due_time);

-- Join tables for many-to-many relationships
CREATE TABLE IF NOT EXISTS quiz_chapters
(
    quiz_id    UUID REFERENCES quizzes (id),
    chapter_id UUID REFERENCES chapters (chapter_id),
    PRIMARY KEY (quiz_id, chapter_id)
);

CREATE TABLE IF NOT EXISTS quiz_learning_outcomes
(
    quiz_id             UUID REFERENCES quizzes (id) ON DELETE CASCADE,
    learning_outcome_id UUID REFERENCES learning_outcomes (id) ON DELETE CASCADE,
    PRIMARY KEY (quiz_id, learning_outcome_id)
);

CREATE TABLE IF NOT EXISTS question_learning_outcomes
(
    question_id         UUID REFERENCES questions (id) ON DELETE CASCADE,
    learning_outcome_id UUID REFERENCES learning_outcomes (id) ON DELETE CASCADE,
    PRIMARY KEY (question_id, learning_outcome_id)
);

CREATE TABLE IF NOT EXISTS student_learning_outcomes
(
    student_id          UUID REFERENCES students (user_id),
    learning_outcome_id UUID REFERENCES learning_outcomes (id),
    PRIMARY KEY (student_id, learning_outcome_id)
);

-- Activity Logs table for History module
CREATE TABLE IF NOT EXISTS activity_logs
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    action_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id   UUID        NOT NULL,
    entity_name VARCHAR(255),
    description TEXT,
    created_at  TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_activity_logs_user_time ON activity_logs (user_id, created_at DESC);

-- ==========================================
-- NOTIFICATION MODULE
-- ==========================================
CREATE TABLE IF NOT EXISTS notifications
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title      VARCHAR(255) NOT NULL,
    message    TEXT         NOT NULL,
    type       VARCHAR(50), -- 'SYSTEM', 'COURSE_UPDATE', 'DEADLINE_REMINDER'
    link_url   VARCHAR(255),
    is_read    BOOLEAN          DEFAULT false,
    created_at TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notif_user_read ON notifications (user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notif_created_at ON notifications (created_at DESC);

-- ==========================================
-- CHAT MODULE
-- ==========================================
CREATE TABLE IF NOT EXISTS user_chat_messages
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipient_id   UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    content        TEXT NOT NULL,
    status         VARCHAR(50)      DEFAULT 'SENT' CHECK (status IN ('SENT', 'DELIVERED', 'READ')),
    client_temp_id VARCHAR(255),
    created_at     TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_chat_messages_participants ON user_chat_messages (sender_id, recipient_id, created_at DESC);

-- ==========================================
-- COURSE ANNOUNCEMENT MODULE
-- ==========================================
CREATE TABLE IF NOT EXISTS course_announcements
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id  UUID         NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    title      VARCHAR(255) NOT NULL,
    content    TEXT         NOT NULL,
    teacher_id UUID         NOT NULL REFERENCES users (id),
    type       VARCHAR(50)      DEFAULT 'INFO',
    created_at TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_course_announcements_course_id ON course_announcements (course_id);

-- ==========================================
-- SPRING MODULITH EVENT PUBLICATION
-- ==========================================
CREATE TABLE IF NOT EXISTS event_publication
(
    id               UUID                     NOT NULL,
    completion_date  TIMESTAMP WITH TIME ZONE,
    event_type       VARCHAR(512)             NOT NULL,
    listener_id      VARCHAR(512)             NOT NULL,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
    serialized_event VARCHAR(4000)            NOT NULL,
    PRIMARY KEY (id)
);

-- ==========================================
-- AI CHAT SESSIONS (Phiên chat / Threads)
-- ==========================================
CREATE TABLE IF NOT EXISTS ai_chat_sessions
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    course_id  UUID NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    title      VARCHAR(255)     DEFAULT 'Đoạn chat mới',
    is_pinned  BOOLEAN          DEFAULT FALSE,
    created_at TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_chat_sessions_user_course ON ai_chat_sessions (user_id, course_id, updated_at DESC);

-- ==========================================
-- AI COURSE ASSISTANT CHAT MESSAGES
-- ==========================================
CREATE TABLE IF NOT EXISTS ai_course_chat_messages
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID        NOT NULL REFERENCES ai_chat_sessions (id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    course_id  UUID        NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL, -- 'user' or 'assistant'
    content    TEXT        NOT NULL,
    created_at TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_chat_messages_session ON ai_course_chat_messages (session_id, created_at ASC);