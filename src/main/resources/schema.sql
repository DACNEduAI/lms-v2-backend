-- Students table (flattened from users + students)
CREATE TABLE IF NOT EXISTS students (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    avatar_url VARCHAR(255),
    role VARCHAR(50),
    enrollment_year BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Teachers table (flattened from users + teachers)
CREATE TABLE IF NOT EXISTS teachers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    avatar_url VARCHAR(255),
    role VARCHAR(50),
    employee_id VARCHAR(255),
    phone_number VARCHAR(255),
    department VARCHAR(255),
    specialization VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Courses table
CREATE TABLE IF NOT EXISTS courses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    course_code VARCHAR(255) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT true,
    cover_image VARCHAR(255),
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Junction table for many-to-many: courses <-> teachers
CREATE TABLE IF NOT EXISTS course_teachers (
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    teacher_id UUID NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    PRIMARY KEY (course_id, teacher_id)
);

-- Junction table for many-to-many: courses <-> students
CREATE TABLE IF NOT EXISTS course_students (
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    PRIMARY KEY (course_id, student_id)
);

-- Chapters table (weak entity dependent on Course)
CREATE TABLE IF NOT EXISTS chapters (
    chapter_number BIGINT GENERATED ALWAYS AS IDENTITY,
    title VARCHAR(255),
    description TEXT,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (chapter_number, course_id)
);

-- Materials table
CREATE TABLE IF NOT EXISTS materials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255),
    type VARCHAR(50) CHECK (type IN ('PDF', 'VIDEO', 'LINK')), -- Added CHECK constraint here
    source_url VARCHAR(255),
    is_downloadable BOOLEAN DEFAULT false,
    chapter_number BIGINT,
    chapter_course_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (chapter_number, chapter_course_id) REFERENCES chapters(chapter_number, course_id) ON DELETE SET NULL
);

-- Junction table for many-to-many: courses <-> materials
CREATE TABLE IF NOT EXISTS course_materials (
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    material_id UUID NOT NULL REFERENCES materials(id) ON DELETE CASCADE,
    PRIMARY KEY (course_id, material_id)
);

-- Learning outcomes table
CREATE TABLE IF NOT EXISTS learning_outcomes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(255),
    description TEXT,
    course_id UUID REFERENCES courses(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Enrollments table
CREATE TABLE IF NOT EXISTS enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID REFERENCES students(id),
    course_id UUID REFERENCES courses(id),
    enrolled_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- AI Settings table (single table for both student and teacher settings)
CREATE TABLE IF NOT EXISTS ai_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    generation_prompt TEXT,
    difficulty_level VARCHAR(50),
    focus_on_weak_areas BOOLEAN DEFAULT false,
    number_of_questions INTEGER,
    setting_type VARCHAR(50),
    align_to_clo BOOLEAN,
    include_answer_key BOOLEAN,
    mode VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Quizzes table
CREATE TABLE IF NOT EXISTS quizzes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_title VARCHAR(255) NOT NULL,
    time_limit_minutes INTEGER,
    pass_score DOUBLE PRECISION,
    ai_settings_id UUID REFERENCES ai_settings(id),
    course_id UUID NOT NULL REFERENCES courses(id),
    student_id UUID REFERENCES students(id),
    teacher_id UUID REFERENCES teachers(id),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Questions table
CREATE TABLE IF NOT EXISTS questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT,
    type VARCHAR(50),
    points DOUBLE PRECISION,
    quiz_id UUID REFERENCES quizzes(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Answers table (weak entity dependent on Question)
CREATE TABLE IF NOT EXISTS answers (
    answer_number BIGINT GENERATED ALWAYS AS IDENTITY,
    content VARCHAR(255),
    is_correct BOOLEAN DEFAULT false,
    explanation TEXT,
    question_id UUID NOT NULL REFERENCES questions(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (answer_number, question_id)
);

-- Attempts table
CREATE TABLE IF NOT EXISTS attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID REFERENCES students(id),
    quiz_id UUID REFERENCES quizzes(id),
    score DOUBLE PRECISION,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Join tables for many-to-many relationships
CREATE TABLE IF NOT EXISTS quiz_chapters (
    quiz_id UUID REFERENCES quizzes(id),
    chapter_number BIGINT,
    chapter_course_id UUID,
    PRIMARY KEY (quiz_id, chapter_number, chapter_course_id),
    FOREIGN KEY (chapter_number, chapter_course_id) REFERENCES chapters(chapter_number, course_id)
);

CREATE TABLE IF NOT EXISTS quiz_learning_outcomes (
    quiz_id UUID REFERENCES quizzes(id),
    learning_outcome_id UUID REFERENCES learning_outcomes(id),
    PRIMARY KEY (quiz_id, learning_outcome_id)
);

CREATE TABLE IF NOT EXISTS question_learning_outcomes (
    question_id UUID REFERENCES questions(id),
    learning_outcome_id UUID REFERENCES learning_outcomes(id),
    PRIMARY KEY (question_id, learning_outcome_id)
);

CREATE TABLE IF NOT EXISTS student_learning_outcomes (
    student_id UUID REFERENCES students(id),
    learning_outcome_id UUID REFERENCES learning_outcomes(id),
    PRIMARY KEY (student_id, learning_outcome_id)
);
