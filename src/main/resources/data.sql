-- 1. Insert Users (shared data for both students and teachers)
INSERT INTO users (id, email, password, first_name, last_name, avatar_url, role)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'student1@university.edu', '$2a$10$DummyHashForPassword1', 'John', 'Doe', 'https://example.com/avatars/john.jpg', 'STUDENT'),
    ('11111111-2222-2222-2222-222222222222', 'student2@university.edu', '$2a$10$DummyHashForPassword2', 'Jane', 'Smith', 'https://example.com/avatars/jane.jpg', 'STUDENT'),
    ('22222222-1111-1111-1111-111111111111', 'dr.turing@university.edu', '$2a$10$DummyHashForPassword3', 'Alan', 'Turing', 'https://example.com/avatars/alan.jpg', 'TEACHER'),
    ('22222222-2222-2222-2222-222222222222', 'dr.lovelace@university.edu', '$2a$10$DummyHashForPassword4', 'Ada', 'Lovelace', 'https://example.com/avatars/ada.jpg', 'TEACHER');

-- 2. Insert Students (role-specific data)
INSERT INTO students (user_id, student_id, enrollment_year)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'STU001', 2023),
    ('11111111-2222-2222-2222-222222222222', 'STU002', 2024);

-- 3. Insert Teachers (role-specific data)
INSERT INTO teachers (user_id, employee_id, phone_number, department, specialization)
VALUES
    ('22222222-1111-1111-1111-111111111111', 'EMP001', '555-0101', 'Computer Science', 'Artificial Intelligence'),
    ('22222222-2222-2222-2222-222222222222', 'EMP002', '555-0102', 'Mathematics', 'Algorithms');

-- 4. Insert Courses
INSERT INTO courses (id, course_name, description, course_code, active, cover_image, start_date, end_date)
VALUES
    ('33333333-1111-1111-1111-111111111111', 'Introduction to Computer Science', 'A foundational course in CS.', 'CS101', true, 'https://example.com/covers/cs101.jpg', '2024-09-01 08:00:00', '2024-12-15 17:00:00'),
    ('33333333-2222-2222-2222-222222222222', 'Data Structures and Algorithms', 'Advanced programming concepts.', 'CS201', true, 'https://example.com/covers/cs201.jpg', '2025-01-15 08:00:00', '2025-05-30 17:00:00');

-- 5. Insert Course-Teacher & Course-Student Relationships
INSERT INTO course_teachers (course_id, teacher_id)
VALUES
    ('33333333-1111-1111-1111-111111111111', '22222222-1111-1111-1111-111111111111'),
    ('33333333-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222');

INSERT INTO course_students (course_id, student_id)
VALUES
    ('33333333-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111'),
    ('33333333-1111-1111-1111-111111111111', '11111111-2222-2222-2222-222222222222'),
    ('33333333-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111');

-- 6. Insert Enrollments
INSERT INTO enrollments (id, student_id, course_id, enrolled_date)
VALUES
    ('44444444-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', '33333333-1111-1111-1111-111111111111', '2024-08-20 10:00:00'),
    ('44444444-2222-2222-2222-222222222222', '11111111-2222-2222-2222-222222222222', '33333333-1111-1111-1111-111111111111', '2024-08-21 11:30:00');

-- 7. Insert Chapters
-- FIXED: Since chapter_number is "BIGINT UNIQUE", it requires globally distinct numbers.
INSERT INTO chapters (chapter_number, title, description, course_id) OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Variables and Data Types', 'Intro to basics.', '33333333-1111-1111-1111-111111111111'),
    (2, 'Control Flow', 'If statements and loops.', '33333333-1111-1111-1111-111111111111'),
    (3, 'Arrays and Linked Lists', 'Basic structures.', '33333333-2222-2222-2222-222222222222');

-- 8. Insert Materials
-- FIXED: Material table only contains 'course_id', removed 'chapter_number' and 'chapter_course_id' from this insert.
-- INSERT INTO materials (id, title, type, file_key, is_downloadable, status, course_id)
-- VALUES
--     ('55555555-1111-1111-1111-111111111111', 'Syllabus PDF', 'PDF', 'courses/33333333-1111-1111-1111-111111111111/syllabus.pdf', true, 'UPLOADED', '33333333-1111-1111-1111-111111111111'),
--     ('55555555-2222-2222-2222-222222222222', 'Intro Lecture', 'VIDEO', 'courses/33333333-1111-1111-1111-111111111111/intro-lecture.mp4', false, 'UPLOADED', '33333333-1111-1111-1111-111111111111'),
--     ('55555555-3333-3333-3333-333333333333', 'Java Documentation', 'LINK', 'https://docs.oracle.com/java', false, 'UPLOADED', '33333333-1111-1111-1111-111111111111');

-- 9. Insert Chapter-Materials Junction
-- FIXED: The correct table in the schema is chapter_materials
-- INSERT INTO chapter_materials (material_id, chapter_number, course_id)
-- VALUES
--     ('55555555-1111-1111-1111-111111111111', 1, '33333333-1111-1111-1111-111111111111'),
--     ('55555555-2222-2222-2222-222222222222', 1, '33333333-1111-1111-1111-111111111111'),
--     ('55555555-3333-3333-3333-333333333333', 2, '33333333-1111-1111-1111-111111111111');

-- 10. Insert Learning Outcomes
INSERT INTO learning_outcomes (id, code, description, course_id)
VALUES
    ('66666666-1111-1111-1111-111111111111', 'CLO1', 'Understand basic syntax and variable usage.', '33333333-1111-1111-1111-111111111111'),
    ('66666666-2222-2222-2222-222222222222', 'CLO2', 'Implement basic iteration and conditionals.', '33333333-1111-1111-1111-111111111111');

-- 11. Insert AI Settings
INSERT INTO ai_settings (id, generation_prompt, difficulty_level, focus_on_weak_areas, number_of_questions, setting_type, align_to_clo, include_answer_key, mode)
VALUES
    ('77777777-1111-1111-1111-111111111111', 'Generate a quiz on basic java syntax.', 'MEDIUM', true, 10, 'QUIZ_GENERATION', true, true, 'STRICT');

-- 12. Insert Quizzes
INSERT INTO quizzes (id, quiz_title, time_limit_minutes, pass_score, ai_settings_id, course_id, student_id, teacher_id, active)
VALUES
    ('88888888-1111-1111-1111-111111111111', 'CS101 Chapter 1 Quiz', 30, 70.0, '77777777-1111-1111-1111-111111111111', '33333333-1111-1111-1111-111111111111', null, '22222222-1111-1111-1111-111111111111', true),
    ('88888888-2222-2222-2222-222222222222', 'Midterm Practice (Personalized)', 60, 50.0, '77777777-1111-1111-1111-111111111111', '33333333-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', null, true);

-- 13. Insert Questions
INSERT INTO questions (id, content, type, points, quiz_id)
VALUES
    ('99999999-1111-1111-1111-111111111111', 'What keyword is used to declare an integer in Java?', 'MULTIPLE_CHOICE', 10.0, '88888888-1111-1111-1111-111111111111'),
    ('99999999-2222-2222-2222-222222222222', 'Boolean values can only be true or false.', 'TRUE_FALSE', 5.0, '88888888-1111-1111-1111-111111111111');

-- 14. Insert Answers
INSERT INTO answers (content, is_correct, explanation, question_id)
VALUES
    ('int', true, 'The int keyword declares an integer primitive.', '99999999-1111-1111-1111-111111111111'),
    ('integer', false, 'integer is a wrapper class, int is the primitive.', '99999999-1111-1111-1111-111111111111'),
    ('True', true, 'Booleans represent binary logic states.', '99999999-2222-2222-2222-222222222222');

-- 15. Insert Attempts
INSERT INTO attempts (id, student_id, quiz_id, score, start_time, end_time, status)
VALUES
    ('bbbbbbbb-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', '88888888-1111-1111-1111-111111111111', 85.0, '2024-09-15 14:00:00', '2024-09-15 14:25:00', 'COMPLETED');

-- 16. Insert Many-to-Many Linking Data
INSERT INTO quiz_chapters (quiz_id, chapter_number, chapter_course_id)
VALUES
    ('88888888-1111-1111-1111-111111111111', 1, '33333333-1111-1111-1111-111111111111');

INSERT INTO quiz_learning_outcomes (quiz_id, learning_outcome_id)
VALUES
    ('88888888-1111-1111-1111-111111111111', '66666666-1111-1111-1111-111111111111');

-- 17. Insert Question Learning Outcomes
INSERT INTO question_learning_outcomes (question_id, learning_outcome_id)
VALUES
    ('99999999-1111-1111-1111-111111111111', '66666666-1111-1111-1111-111111111111');

-- 18. Insert Student Learning Outcomes
INSERT INTO student_learning_outcomes (student_id, learning_outcome_id)
VALUES
    ('11111111-1111-1111-1111-111111111111', '66666666-1111-1111-1111-111111111111');