-- 1. Insert Users
INSERT INTO users (id, email, password, first_name, last_name, avatar_url, role)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'student1@university.edu', '$2a$10$DummyHashForPassword1', 'John', 'Doe', 'https://example.com/avatars/john.jpg', 'STUDENT'),
    ('11111111-2222-2222-2222-222222222222', 'student2@university.edu', '$2a$10$DummyHashForPassword2', 'Jane', 'Smith', 'https://example.com/avatars/jane.jpg', 'STUDENT'),
    ('22222222-1111-1111-1111-111111111111', 'dr.turing@university.edu', '$2a$10$DummyHashForPassword3', 'Alan', 'Turing', 'https://example.com/avatars/alan.jpg', 'TEACHER'),
    ('22222222-2222-2222-2222-222222222222', 'dr.lovelace@university.edu', '$2a$10$DummyHashForPassword4', 'Ada', 'Lovelace', 'https://example.com/avatars/ada.jpg', 'TEACHER');

-- 2. Insert Students
INSERT INTO students (user_id, student_id, enrollment_year)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'STU001', 2023),
    ('11111111-2222-2222-2222-222222222222', 'STU002', 2024);

-- 3. Insert Teachers
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
INSERT INTO chapters (chapter_number, title, description, course_id) OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Variables and Data Types', 'Introduction to variables, primitives, and type systems.', '33333333-1111-1111-1111-111111111111'),
    (2, 'Control Flow', 'If statements, loops, and branching logic.', '33333333-1111-1111-1111-111111111111'),
    (3, 'Arrays and Linked Lists', 'Linear data structures and their operations.', '33333333-2222-2222-2222-222222222222'),
    (4, 'Trees and Graphs', 'Non-linear data structures and traversals.', '33333333-2222-2222-2222-222222222222');

-- 8. Insert Group Learning Outcomes
INSERT INTO group_learning_outcomes (id, name, description, course_id)
VALUES
    ('aaaaaaaa-1111-1111-1111-111111111111', 'Programming Fundamentals', 'Core programming concepts including syntax and data types.', '33333333-1111-1111-1111-111111111111'),
    ('aaaaaaaa-2222-2222-2222-222222222222', 'Control Flow Concepts', 'Understanding iteration, conditionals, and branching.', '33333333-1111-1111-1111-111111111111'),
    ('aaaaaaaa-3333-3333-3333-333333333333', 'Linear Data Structures', 'Arrays, linked lists, and their time complexities.', '33333333-2222-2222-2222-222222222222');

-- 9. Insert Learning Outcomes
-- CS101 Learning Outcomes
INSERT INTO learning_outcomes (id, code, description, course_id, group_learning_outcome_id)
VALUES
    ('66666666-1111-1111-1111-111111111111', 'CLO1', 'Understand basic syntax and variable usage.', '33333333-1111-1111-1111-111111111111', 'aaaaaaaa-1111-1111-1111-111111111111'),
    ('66666666-2222-2222-2222-222222222222', 'CLO2', 'Declare and use different data types correctly.', '33333333-1111-1111-1111-111111111111', 'aaaaaaaa-1111-1111-1111-111111111111'),
    ('66666666-3333-3333-3333-333333333333', 'CLO3', 'Implement basic iteration and conditionals.', '33333333-1111-1111-1111-111111111111', 'aaaaaaaa-2222-2222-2222-222222222222'),
    ('66666666-4444-4444-4444-444444444444', 'CLO4', 'Use switch statements and ternary operators.', '33333333-1111-1111-1111-111111111111', 'aaaaaaaa-2222-2222-2222-222222222222'),
    ('66666666-5555-5555-5555-555555555555', 'CLO5', 'Debug simple programs using systematic approaches.', '33333333-1111-1111-1111-111111111111', NULL);

-- CS201 Learning Outcomes
INSERT INTO learning_outcomes (id, code, description, course_id, group_learning_outcome_id)
VALUES
    ('66666666-6666-6666-6666-666666666666', 'CLO6', 'Implement and use arrays efficiently.', '33333333-2222-2222-2222-222222222222', 'aaaaaaaa-3333-3333-3333-333333333333'),
    ('66666666-7777-7777-7777-777777777777', 'CLO7', 'Understand linked list operations and trade-offs.', '33333333-2222-2222-2222-222222222222', 'aaaaaaaa-3333-3333-3333-333333333333'),
    ('66666666-8888-8888-8888-888888888888', 'CLO8', 'Analyze time complexity of basic operations.', '33333333-2222-2222-2222-222222222222', NULL),
    ('66666666-9999-9999-9999-999999999999', 'CLO9', 'Implement tree traversal algorithms.', '33333333-2222-2222-2222-222222222222', NULL);

-- 10. Insert Chapter-Learning Outcome Relationships
-- Ch1 (Variables): CLO1, CLO2, CLO5
INSERT INTO chapter_learning_outcomes (chapter_number, course_id, learning_outcome_id)
VALUES
    (1, '33333333-1111-1111-1111-111111111111', '66666666-1111-1111-1111-111111111111'),
    (1, '33333333-1111-1111-1111-111111111111', '66666666-2222-2222-2222-222222222222'),
    (1, '33333333-1111-1111-1111-111111111111', '66666666-5555-5555-5555-555555555555');

-- Ch2 (Control Flow): CLO3, CLO4, CLO5
INSERT INTO chapter_learning_outcomes (chapter_number, course_id, learning_outcome_id)
VALUES
    (2, '33333333-1111-1111-1111-111111111111', '66666666-3333-3333-3333-333333333333'),
    (2, '33333333-1111-1111-1111-111111111111', '66666666-4444-4444-4444-444444444444'),
    (2, '33333333-1111-1111-1111-111111111111', '66666666-5555-5555-5555-555555555555');

-- Ch3 (Arrays & Linked Lists): CLO6, CLO7, CLO8
INSERT INTO chapter_learning_outcomes (chapter_number, course_id, learning_outcome_id)
VALUES
    (3, '33333333-2222-2222-2222-222222222222', '66666666-6666-6666-6666-666666666666'),
    (3, '33333333-2222-2222-2222-222222222222', '66666666-7777-7777-7777-777777777777'),
    (3, '33333333-2222-2222-2222-222222222222', '66666666-8888-8888-8888-888888888888');

-- Ch4 (Trees & Graphs): CLO8, CLO9
INSERT INTO chapter_learning_outcomes (chapter_number, course_id, learning_outcome_id)
VALUES
    (4, '33333333-2222-2222-2222-222222222222', '66666666-8888-8888-8888-888888888888'),
    (4, '33333333-2222-2222-2222-222222222222', '66666666-9999-9999-9999-999999999999');

-- 11. Insert AI Settings
INSERT INTO ai_settings (id, generation_prompt, difficulty_level, focus_on_weak_areas, number_of_questions, setting_type, align_to_clo, include_answer_key, mode)
VALUES
    ('77777777-1111-1111-1111-111111111111', 'Generate a quiz on basic java syntax.', 'MEDIUM', true, 10, 'QUIZ_GENERATION', true, true, 'STRICT');

-- 12. Insert Quizzes
-- Quiz 1: CS101 Chapter 1 Quiz (teacher-created), chapters=[1], LOs=[CLO1, CLO2] (subset of Ch1: CLO1,CLO2,CLO5)
-- Quiz 2: CS101 Midterm Practice (student-personalized), chapters=[1,2], LOs=[CLO1, CLO3, CLO4] (subset of Ch1∪Ch2)
-- Quiz 3: CS201 Arrays Quiz (teacher-created), chapters=[3], LOs=[CLO6, CLO7, CLO8] (= Ch3 LOs)
-- Quiz 4: CS201 Trees Quiz (teacher-created), chapters=[4], LOs=[CLO8, CLO9] (= Ch4 LOs)
INSERT INTO quizzes (id, quiz_title, time_limit_minutes, pass_score, ai_settings_id, course_id, student_id, teacher_id, active)
VALUES
    ('88888888-1111-1111-1111-111111111111', 'CS101 Chapter 1 Quiz', 30, 70.0, '77777777-1111-1111-1111-111111111111', '33333333-1111-1111-1111-111111111111', NULL, '22222222-1111-1111-1111-111111111111', true),
    ('88888888-2222-2222-2222-222222222222', 'CS101 Midterm Practice', 60, 50.0, '77777777-1111-1111-1111-111111111111', '33333333-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', NULL, true),
    ('88888888-3333-3333-3333-333333333333', 'CS201 Arrays Quiz', 45, 65.0, '77777777-1111-1111-1111-111111111111', '33333333-2222-2222-2222-222222222222', NULL, '22222222-2222-2222-2222-222222222222', true),
    ('88888888-4444-4444-4444-444444444444', 'CS201 Trees Quiz', 40, 60.0, '77777777-1111-1111-1111-111111111111', '33333333-2222-2222-2222-222222222222', NULL, '22222222-2222-2222-2222-222222222222', true);

-- 13. Insert Questions
-- Quiz 1 questions (LOs must be in {CLO1, CLO2})
INSERT INTO questions (id, content, type, points, quiz_id)
VALUES
    ('99999999-1111-1111-1111-111111111111', 'What keyword is used to declare an integer variable in Java?', 'MULTIPLE_CHOICE', 10.0, '88888888-1111-1111-1111-111111111111'),
    ('99999999-2222-2222-2222-222222222222', 'Boolean is a primitive data type in Java.', 'TRUE_FALSE', 5.0, '88888888-1111-1111-1111-111111111111'),
    ('99999999-3333-3333-3333-333333333333', 'Explain the difference between int and Integer in Java. When would you use each?', 'ESSAY', 15.0, '88888888-1111-1111-1111-111111111111');

-- Quiz 2 questions (LOs must be in {CLO1, CLO3, CLO4})
INSERT INTO questions (id, content, type, points, quiz_id)
VALUES
    ('99999999-4444-4444-4444-444444444444', 'What is the output of: int x = 5; System.out.println(x++);', 'MULTIPLE_CHOICE', 10.0, '88888888-2222-2222-2222-222222222222'),
    ('99999999-5555-5555-5555-555555555555', 'Which loop structure guarantees at least one iteration?', 'MULTIPLE_CHOICE', 10.0, '88888888-2222-2222-2222-222222222222'),
    ('99999999-6666-6666-6666-666666666666', 'A switch statement in Java can only use integer values as cases.', 'TRUE_FALSE', 5.0, '88888888-2222-2222-2222-222222222222');

-- Quiz 3 questions (LOs must be in {CLO6, CLO7, CLO8})
INSERT INTO questions (id, content, type, points, quiz_id)
VALUES
    ('99999999-7777-7777-7777-777777777777', 'Arrays provide O(1) random access time.', 'TRUE_FALSE', 5.0, '88888888-3333-3333-3333-333333333333'),
    ('99999999-8888-8888-8888-888888888888', 'What is the time complexity of inserting an element at the head of a singly linked list?', 'MULTIPLE_CHOICE', 10.0, '88888888-3333-3333-3333-333333333333'),
    ('99999999-9999-9999-9999-999999999999', 'Explain Big-O notation and provide examples for O(1), O(n), and O(n^2) operations.', 'ESSAY', 20.0, '88888888-3333-3333-3333-333333333333');

-- Quiz 4 questions (LOs must be in {CLO8, CLO9})
INSERT INTO questions (id, content, type, points, quiz_id)
VALUES
    ('99999999-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Which tree traversal method visits the root node first?', 'MULTIPLE_CHOICE', 10.0, '88888888-4444-4444-4444-444444444444'),
    ('99999999-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Binary search on a sorted array has O(log n) time complexity.', 'TRUE_FALSE', 5.0, '88888888-4444-4444-4444-444444444444');

-- 14. Insert Answers (for MULTIPLE_CHOICE and TRUE_FALSE questions; ESSAY questions have no answers)
-- Q1: What keyword declares an integer?
INSERT INTO answers (content, is_correct, explanation, question_id)
VALUES
    ('int', true, 'The int keyword declares a 32-bit integer primitive.', '99999999-1111-1111-1111-111111111111'),
    ('integer', false, 'integer is not a keyword; Integer is a wrapper class.', '99999999-1111-1111-1111-111111111111'),
    ('number', false, 'number is not a valid Java keyword.', '99999999-1111-1111-1111-111111111111'),
    ('var', false, 'var is for type inference, not specifically for integers.', '99999999-1111-1111-1111-111111111111');

-- Q2: Boolean is a primitive data type
INSERT INTO answers (content, is_correct, explanation, question_id)
VALUES
    ('True', false, 'boolean (lowercase) is primitive; Boolean (uppercase) is a wrapper class.', '99999999-2222-2222-2222-222222222222'),
    ('False', true, 'Boolean is a wrapper class. boolean (lowercase) is the primitive type.', '99999999-2222-2222-2222-222222222222');

-- Q4: Output of x++
INSERT INTO answers (content, is_correct, explanation, question_id)
VALUES
    ('5', true, 'Post-increment returns the value before incrementing.', '99999999-4444-4444-4444-444444444444'),
    ('6', false, 'Pre-increment (++x) would output 6, not post-increment.', '99999999-4444-4444-4444-444444444444'),
    ('Compilation Error', false, 'This is valid Java syntax.', '99999999-4444-4444-4444-444444444444');

-- Q5: Which loop guarantees at least one iteration?
INSERT INTO answers (content, is_correct, explanation, question_id)
VALUES
    ('do-while', true, 'The do-while loop executes the body first, then checks the condition.', '99999999-5555-5555-5555-555555555555'),
    ('for', false, 'A for loop may never execute if the condition is initially false.', '99999999-5555-5555-5555-555555555555'),
    ('while', false, 'A while loop may never execute if the condition is initially false.', '99999999-5555-5555-5555-555555555555');

-- Q6: Switch only uses integers
INSERT INTO answers (content, is_correct, explanation, question_id)
VALUES
    ('True', false, 'Switch also supports String, char, byte, short, and enum types.', '99999999-6666-6666-6666-666666666666'),
    ('False', true, 'Java switch supports int, String, char, byte, short, and enum types.', '99999999-6666-6666-6666-666666666666');

-- Q7: Arrays have O(1) access
INSERT INTO answers (content, is_correct, explanation, question_id)
VALUES
    ('True', true, 'Arrays use contiguous memory, allowing direct index-based access in O(1).', '99999999-7777-7777-7777-777777777777'),
    ('False', false, 'Arrays do provide O(1) random access via index calculation.', '99999999-7777-7777-7777-777777777777');

-- Q8: Time complexity of linked list head insert
INSERT INTO answers (content, is_correct, explanation, question_id)
VALUES
    ('O(1)', true, 'Inserting at the head only requires updating the head pointer.', '99999999-8888-8888-8888-888888888888'),
    ('O(n)', false, 'O(n) applies to inserting at the tail without a tail pointer.', '99999999-8888-8888-8888-888888888888'),
    ('O(log n)', false, 'Linked list operations are not logarithmic.', '99999999-8888-8888-8888-888888888888');

-- Q10: Which traversal visits root first?
INSERT INTO answers (content, is_correct, explanation, question_id)
VALUES
    ('Pre-order', true, 'Pre-order visits root, then left subtree, then right subtree.', '99999999-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    ('In-order', false, 'In-order visits left subtree, then root, then right subtree.', '99999999-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    ('Post-order', false, 'Post-order visits left, right, then root last.', '99999999-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    ('Level-order', false, 'Level-order visits nodes level by level, but pre-order visits root first in DFS.', '99999999-aaaa-aaaa-aaaa-aaaaaaaaaaaa');

-- Q11: Binary search is O(log n)
INSERT INTO answers (content, is_correct, explanation, question_id)
VALUES
    ('True', true, 'Binary search halves the search space each step, giving O(log n).', '99999999-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
    ('False', false, 'Binary search is indeed O(log n) on a sorted array.', '99999999-bbbb-bbbb-bbbb-bbbbbbbbbbbb');

-- 15. Insert Attempts
INSERT INTO attempts (id, student_id, quiz_id, score, start_time, end_time, status)
VALUES
    ('bbbbbbbb-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', '88888888-1111-1111-1111-111111111111', 85.0, '2024-09-15 14:00:00', '2024-09-15 14:25:00', 'COMPLETED');

-- 16. Insert Quiz-Chapter Relationships
INSERT INTO quiz_chapters (quiz_id, chapter_number, chapter_course_id)
VALUES
    ('88888888-1111-1111-1111-111111111111', 1, '33333333-1111-1111-1111-111111111111'),
    ('88888888-2222-2222-2222-222222222222', 1, '33333333-1111-1111-1111-111111111111'),
    ('88888888-2222-2222-2222-222222222222', 2, '33333333-1111-1111-1111-111111111111'),
    ('88888888-3333-3333-3333-333333333333', 3, '33333333-2222-2222-2222-222222222222'),
    ('88888888-4444-4444-4444-444444444444', 4, '33333333-2222-2222-2222-222222222222');

-- 17. Insert Quiz-Learning Outcome Relationships
-- Quiz 1 LOs: CLO1, CLO2 (from Ch1: CLO1, CLO2, CLO5)
INSERT INTO quiz_learning_outcomes (quiz_id, learning_outcome_id)
VALUES
    ('88888888-1111-1111-1111-111111111111', '66666666-1111-1111-1111-111111111111'),
    ('88888888-1111-1111-1111-111111111111', '66666666-2222-2222-2222-222222222222');

-- Quiz 2 LOs: CLO1, CLO3, CLO4 (from Ch1∪Ch2: CLO1,CLO2,CLO3,CLO4,CLO5)
INSERT INTO quiz_learning_outcomes (quiz_id, learning_outcome_id)
VALUES
    ('88888888-2222-2222-2222-222222222222', '66666666-1111-1111-1111-111111111111'),
    ('88888888-2222-2222-2222-222222222222', '66666666-3333-3333-3333-333333333333'),
    ('88888888-2222-2222-2222-222222222222', '66666666-4444-4444-4444-444444444444');

-- Quiz 3 LOs: CLO6, CLO7, CLO8 (from Ch3: CLO6, CLO7, CLO8)
INSERT INTO quiz_learning_outcomes (quiz_id, learning_outcome_id)
VALUES
    ('88888888-3333-3333-3333-333333333333', '66666666-6666-6666-6666-666666666666'),
    ('88888888-3333-3333-3333-333333333333', '66666666-7777-7777-7777-777777777777'),
    ('88888888-3333-3333-3333-333333333333', '66666666-8888-8888-8888-888888888888');

-- Quiz 4 LOs: CLO8, CLO9 (from Ch4: CLO8, CLO9)
INSERT INTO quiz_learning_outcomes (quiz_id, learning_outcome_id)
VALUES
    ('88888888-4444-4444-4444-444444444444', '66666666-8888-8888-8888-888888888888'),
    ('88888888-4444-4444-4444-444444444444', '66666666-9999-9999-9999-999999999999');

-- 18. Insert Question-Learning Outcome Relationships
-- Each question's LO must exist in its quiz's LOs
INSERT INTO question_learning_outcomes (question_id, learning_outcome_id)
VALUES
    ('99999999-1111-1111-1111-111111111111', '66666666-1111-1111-1111-111111111111'),
    ('99999999-2222-2222-2222-222222222222', '66666666-2222-2222-2222-222222222222'),
    ('99999999-3333-3333-3333-333333333333', '66666666-1111-1111-1111-111111111111'),
    ('99999999-4444-4444-4444-444444444444', '66666666-1111-1111-1111-111111111111'),
    ('99999999-5555-5555-5555-555555555555', '66666666-3333-3333-3333-333333333333'),
    ('99999999-6666-6666-6666-666666666666', '66666666-4444-4444-4444-444444444444'),
    ('99999999-7777-7777-7777-777777777777', '66666666-6666-6666-6666-666666666666'),
    ('99999999-8888-8888-8888-888888888888', '66666666-7777-7777-7777-777777777777'),
    ('99999999-9999-9999-9999-999999999999', '66666666-8888-8888-8888-888888888888'),
    ('99999999-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '66666666-9999-9999-9999-999999999999'),
    ('99999999-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '66666666-8888-8888-8888-888888888888');

-- 19. Insert Student Learning Outcomes
INSERT INTO student_learning_outcomes (student_id, learning_outcome_id)
VALUES
    ('11111111-1111-1111-1111-111111111111', '66666666-1111-1111-1111-111111111111'),
    ('11111111-1111-1111-1111-111111111111', '66666666-2222-2222-2222-222222222222');