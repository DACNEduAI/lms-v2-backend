-- 1. Insert Users
INSERT INTO users (id, email, password, first_name, last_name, avatar_url, role)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'martin.fowler@university.edu', '123456', 'Martin', 'Fowler', 'https://example.com/avatars/mf.jpg', 'TEACHER'),
    ('22222222-2222-2222-2222-222222222222', 'andrew.ng@university.edu', '123456', 'Andrew', 'Ng', 'https://example.com/avatars/an.jpg', 'TEACHER'),
    ('33333333-3333-3333-3333-333333333333', 'kelsey.hightower@university.edu', '123456', 'Kelsey', 'Hightower', 'https://example.com/avatars/kh.jpg', 'TEACHER'),
    ('44444444-4444-4444-4444-444444444444', 'alice.student@university.edu', '123456', 'Alice', 'Chen', 'https://example.com/avatars/ac.jpg', 'STUDENT'),
    ('55555555-5555-5555-5555-555555555555', 'bob.student@university.edu', '123456', 'Bob', 'Martin', 'https://example.com/avatars/bm.jpg', 'STUDENT'),
    ('66666666-6666-6666-6666-666666666666', 'carol.student@university.edu', '123456', 'Carol', 'Danvers', 'https://example.com/avatars/cd.jpg', 'STUDENT');

-- 2. Insert Students (Using the valid user IDs you just inserted)
INSERT INTO students (user_id, student_id, enrollment_year)
VALUES
    ('44444444-4444-4444-4444-444444444444', 'STU-2024-001', 2024),
    ('55555555-5555-5555-5555-555555555555', 'STU-2024-002', 2024),
    ('66666666-6666-6666-6666-666666666666', 'STU-2025-001', 2025);

-- 3. Insert Teachers
INSERT INTO teachers (user_id, employee_id, phone_number, department, specialization)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'EMP-ARCH-01', '555-0199', 'Software Engineering', 'System Architecture & DDD'),
    ('22222222-2222-2222-2222-222222222222', 'EMP-AI-01', '555-0200', 'Computer Science', 'Machine Learning & AI'),
    ('33333333-3333-3333-3333-333333333333', 'EMP-DEV-01', '555-0201', 'Information Technology', 'DevOps & Containerization');

-- 4. Insert Courses (using valid hex 'c...')
INSERT INTO courses (id, course_name, description, course_code, active, cover_image, start_date, end_date)
VALUES
    ('c1111111-1111-1111-1111-111111111111', 'Advanced Software Architecture', 'Exploring Domain-Driven Design, Hexagonal Architecture, and Microservices.', 'SE401', true, 'https://example.com/covers/se401.jpg', TO_TIMESTAMP('01-09-2025 08:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('15-12-2025 17:00:00', 'DD-MM-YYYY HH24:MI:SS')),
    ('c2222222-2222-2222-2222-222222222222', 'Applied Artificial Intelligence', 'Practical AI implementation including RAG, Vector Databases, Perceptrons, and SVMs.', 'CS502', true, 'https://example.com/covers/cs502.jpg', TO_TIMESTAMP('15-01-2026 08:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('30-05-2026 17:00:00', 'DD-MM-YYYY HH24:MI:SS')),
    ('c3333333-3333-3333-3333-333333333333', 'Modern DevOps Practices', 'Mastering Docker, LocalStack, and GitHub Actions for CI/CD pipelines.', 'IT305', true, 'https://example.com/covers/it305.jpg', TO_TIMESTAMP('01-09-2025 08:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('15-12-2025 17:00:00', 'DD-MM-YYYY HH24:MI:SS'));

-- 5. Insert Enrollments
INSERT INTO enrollments (course_id, user_id, enrolled_date)
VALUES
    -- Teachers assigned
    ('c1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', TO_TIMESTAMP('15-08-2025 09:00:00', 'DD-MM-YYYY HH24:MI:SS')),
    ('c2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', TO_TIMESTAMP('10-01-2026 09:00:00', 'DD-MM-YYYY HH24:MI:SS')),
    ('c3333333-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', TO_TIMESTAMP('20-08-2025 09:00:00', 'DD-MM-YYYY HH24:MI:SS')),
    -- Students
    ('c1111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', TO_TIMESTAMP('20-08-2025 10:00:00', 'DD-MM-YYYY HH24:MI:SS')),
    ('c1111111-1111-1111-1111-111111111111', '55555555-5555-5555-5555-555555555555', TO_TIMESTAMP('21-08-2025 11:30:00', 'DD-MM-YYYY HH24:MI:SS')),
    ('c2222222-2222-2222-2222-222222222222', '55555555-5555-5555-5555-555555555555', TO_TIMESTAMP('12-01-2026 08:30:00', 'DD-MM-YYYY HH24:MI:SS')),
    ('c2222222-2222-2222-2222-222222222222', '66666666-6666-6666-6666-666666666666', TO_TIMESTAMP('13-01-2026 14:15:00', 'DD-MM-YYYY HH24:MI:SS')),
    ('c3333333-3333-3333-3333-333333333333', '44444444-4444-4444-4444-444444444444', TO_TIMESTAMP('22-08-2025 09:15:00', 'DD-MM-YYYY HH24:MI:SS')),
    ('c3333333-3333-3333-3333-333333333333', '66666666-6666-6666-6666-666666666666', TO_TIMESTAMP('23-08-2025 10:00:00', 'DD-MM-YYYY HH24:MI:SS'));

-- 6. Insert Chapters
INSERT INTO chapters (chapter_number, title, description, course_id) OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Clean Architecture Foundations', 'Isolating domain logic from frameworks and delivery mechanisms.', 'c1111111-1111-1111-1111-111111111111'),
    (2, 'Tactical Domain-Driven Design', 'Deep dive into Entities, Value Objects, and Aggregate Roots.', 'c1111111-1111-1111-1111-111111111111'),
    (3, 'Strategic Design and Context Mapping', 'Defining Bounded Contexts and integrating external systems.', 'c1111111-1111-1111-1111-111111111111'),
    (1, 'Foundational ML Algorithms', 'Understanding the math behind Perceptrons and Support Vector Machines (SVM).', 'c2222222-2222-2222-2222-222222222222'),
    (2, 'Vector Databases and Embeddings', 'High-dimensional vector spaces and similarity search mechanisms.', 'c2222222-2222-2222-2222-222222222222'),
    (3, 'Building Agents and RAG Pipelines', 'Integrating LangChain Agents with external knowledge bases.', 'c2222222-2222-2222-2222-222222222222'),
    (1, 'Containerization with Docker', 'Creating Dockerfiles, managing images, and composing multi-container apps.', 'c3333333-3333-3333-3333-333333333333'),
    (2, 'CI/CD Automation', 'Building robust deployment pipelines using GitHub Actions.', 'c3333333-3333-3333-3333-333333333333');

-- 7. Insert Group Learning Outcomes (using valid hex '7...')
INSERT INTO group_learning_outcomes (id, name, description, course_id)
VALUES
    ('71111111-1111-1111-1111-111111111111', 'Structural Patterns', 'Mastery of backend application layering.', 'c1111111-1111-1111-1111-111111111111'),
    ('72222222-2222-2222-2222-222222222222', 'Applied AI Integration', 'Designing applications that utilize ML models and LLMs.', 'c2222222-2222-2222-2222-222222222222'),
    ('73333333-3333-3333-3333-333333333333', 'Infrastructure as Code', 'Managing environments through reproducible configuration.', 'c3333333-3333-3333-3333-333333333333');

-- 8. Insert Learning Outcomes (using valid hex '8...')
INSERT INTO learning_outcomes (id, code, description, course_id, group_learning_outcome_id)
VALUES
    ('81111111-1111-1111-1111-111111111111', 'SE401-1', 'Implement dependency inversion in a Spring Boot application.', 'c1111111-1111-1111-1111-111111111111', '71111111-1111-1111-1111-111111111111'),
    ('82222222-2222-2222-2222-222222222222', 'SE401-2', 'Design transactional boundaries using DDD Aggregate Roots.', 'c1111111-1111-1111-1111-111111111111', '71111111-1111-1111-1111-111111111111'),
    ('83333333-3333-3333-3333-333333333333', 'SE401-3', 'Identify Bounded Contexts within a complex monolithic system.', 'c1111111-1111-1111-1111-111111111111', '71111111-1111-1111-1111-111111111111'),
    ('84444444-4444-4444-4444-444444444444', 'CS502-1', 'Differentiate between linear and non-linear classification using SVMs.', 'c2222222-2222-2222-2222-222222222222', '72222222-2222-2222-2222-222222222222'),
    ('85555555-5555-5555-5555-555555555555', 'CS502-2', 'Construct a Retrieval-Augmented Generation workflow using LangChain.', 'c2222222-2222-2222-2222-222222222222', '72222222-2222-2222-2222-222222222222'),
    ('86666666-6666-6666-6666-666666666666', 'IT305-1', 'Containerize a full-stack application using Docker Compose.', 'c3333333-3333-3333-3333-333333333333', '73333333-3333-3333-3333-333333333333'),
    ('87777777-7777-7777-7777-777777777777', 'IT305-2', 'Automate testing and deployment using GitHub Actions workflows.', 'c3333333-3333-3333-3333-333333333333', '73333333-3333-3333-3333-333333333333');

-- 9. Insert Chapter-Learning Outcome Relationships
INSERT INTO chapter_learning_outcomes (chapter_number, course_id, learning_outcome_id)
VALUES
    (1, 'c1111111-1111-1111-1111-111111111111', '81111111-1111-1111-1111-111111111111'),
    (2, 'c1111111-1111-1111-1111-111111111111', '82222222-2222-2222-2222-222222222222'),
    (3, 'c1111111-1111-1111-1111-111111111111', '83333333-3333-3333-3333-333333333333'),
    (1, 'c2222222-2222-2222-2222-222222222222', '84444444-4444-4444-4444-444444444444'),
    (3, 'c2222222-2222-2222-2222-222222222222', '85555555-5555-5555-5555-555555555555'),
    (1, 'c3333333-3333-3333-3333-333333333333', '86666666-6666-6666-6666-666666666666'),
    (2, 'c3333333-3333-3333-3333-333333333333', '87777777-7777-7777-7777-777777777777');

-- 10. Insert AI Settings (using valid hex 'a...')
INSERT INTO ai_settings (id, generation_prompt, difficulty_level, focus_on_weak_areas, number_of_questions, setting_type, align_to_clo, include_answer_key, mode)
VALUES
    ('a1111111-1111-1111-1111-111111111111', 'Generate architecture scenario questions.', 'HARD', true, 5, 'QUIZ_GENERATION', true, true, 'STRICT'),
    ('a2222222-2222-2222-2222-222222222222', 'Create conceptual ML questions.', 'MEDIUM', false, 10, 'QUIZ_GENERATION', true, true, 'RELAXED'),
    ('a3333333-3333-3333-3333-333333333333', 'Generate CI/CD troubleshooting scenarios.', 'HARD', true, 8, 'QUIZ_GENERATION', true, true, 'STRICT');

-- 11. Insert Quizzes (using valid hex '9...')
INSERT INTO quizzes (id, quiz_title, time_limit_minutes, pass_score, start_time, due_time, attempt_limited, ai_settings_id, course_id, teacher_id, display)
VALUES
    ('91111111-1111-1111-1111-111111111111', 'Arch Midterm: Clean Arch & DDD', 60, 70.0, TO_TIMESTAMP('15-10-2025 09:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('15-10-2025 11:00:00', 'DD-MM-YYYY HH24:MI:SS'), 1, 'a1111111-1111-1111-1111-111111111111', 'c1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', true),
    ('92222222-2222-2222-2222-222222222222', 'Arch Final: System Boundaries', 120, 75.0, TO_TIMESTAMP('10-12-2025 13:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('10-12-2025 15:00:00', 'DD-MM-YYYY HH24:MI:SS'), 1, 'a1111111-1111-1111-1111-111111111111', 'c1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', true),
    ('93333333-3333-3333-3333-333333333333', 'ML Foundations Check', 30, 60.0, TO_TIMESTAMP('15-02-2026 10:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('20-02-2026 23:59:00', 'DD-MM-YYYY HH24:MI:SS'), 2, 'a2222222-2222-2222-2222-222222222222', 'c2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', true),
    ('94444444-4444-4444-4444-444444444444', 'Applied RAG & Agents', 45, 80.0, TO_TIMESTAMP('10-04-2026 09:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('15-04-2026 23:59:00', 'DD-MM-YYYY HH24:MI:SS'), 3, 'a2222222-2222-2222-2222-222222222222', 'c2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', true),
    ('95555555-5555-5555-5555-555555555555', 'Docker & CI/CD Practical', 90, 85.0, TO_TIMESTAMP('05-11-2025 14:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('05-11-2025 17:00:00', 'DD-MM-YYYY HH24:MI:SS'), 1, 'a3333333-3333-3333-3333-333333333333', 'c3333333-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', true);

-- 12. Insert Questions (using valid hex 'd...')
INSERT INTO questions (id, content, type, points, quiz_id)
VALUES
    ('d1111111-1111-1111-1111-111111111111', 'In Clean Architecture, entities can depend on Use Case orchestrators.', 'TRUE_FALSE', 5.0, '91111111-1111-1111-1111-111111111111'),
    ('d2222222-2222-2222-2222-222222222222', 'Which DDD concept ensures transactional consistency?', 'MULTIPLE_CHOICE', 10.0, '91111111-1111-1111-1111-111111111111'),
    ('d3333333-3333-3333-3333-333333333333', 'Explain how Ports and Adapters isolate the core domain from a PostgreSQL database.', 'ESSAY', 20.0, '91111111-1111-1111-1111-111111111111'),
    ('d4444444-4444-4444-4444-444444444444', 'An Anti-Corruption Layer (ACL) is used to translate models between two bounded contexts.', 'TRUE_FALSE', 10.0, '92222222-2222-2222-2222-222222222222'),
    ('d5555555-5555-5555-5555-555555555555', 'A single-layer perceptron can perfectly classify XOR data.', 'TRUE_FALSE', 5.0, '93333333-3333-3333-3333-333333333333'),
    ('d6666666-6666-6666-6666-666666666666', 'What technique does an SVM use to handle non-linearly separable data?', 'MULTIPLE_CHOICE', 10.0, '93333333-3333-3333-3333-333333333333'),
    ('d7777777-7777-7777-7777-777777777777', 'Which LangChain component is responsible for dynamically deciding the next action step?', 'MULTIPLE_CHOICE', 10.0, '94444444-4444-4444-4444-444444444444'),
    ('d8888888-8888-8888-8888-888888888888', 'In a RAG pipeline, semantic search is performed over the raw text strings rather than vector embeddings.', 'TRUE_FALSE', 5.0, '94444444-4444-4444-4444-444444444444'),
    ('d9999999-9999-9999-9999-999999999999', 'What instruction in a Dockerfile defines the base image?', 'MULTIPLE_CHOICE', 5.0, '95555555-5555-5555-5555-555555555555'),
    ('daaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'A GitHub Action Workflow must be placed in the `.github/workflows` directory.', 'TRUE_FALSE', 5.0, '95555555-5555-5555-5555-555555555555'),
    ('dbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Describe the steps to configure a multi-stage Docker build to optimize the final image size of a Spring Boot application.', 'ESSAY', 25.0, '95555555-5555-5555-5555-555555555555');

-- 13. Insert Answers
INSERT INTO answers (content, is_correct, explanation, question_id)
VALUES
    ('True', false, 'Dependencies must point inward. Entities are at the center and depend on nothing.', 'd1111111-1111-1111-1111-111111111111'),
    ('False', true, 'The Dependency Rule dictates that core entities cannot depend on outer layers like Use Cases.', 'd1111111-1111-1111-1111-111111111111'),
    ('Value Object', false, 'Value objects describe characteristics but do not manage boundaries.', 'd2222222-2222-2222-2222-222222222222'),
    ('Aggregate Root', true, 'Aggregates enforce invariants and serve as transactional boundaries.', 'd2222222-2222-2222-2222-222222222222'),
    ('Domain Service', false, 'Services encapsulate logic that doesn''t naturally fit an entity.', 'd2222222-2222-2222-2222-222222222222'),
    ('True', true, 'ACL acts as a buffer/translator to prevent a downstream context from being polluted by upstream models.', 'd4444444-4444-4444-4444-444444444444'),
    ('False', false, 'This is exactly the purpose of an Anti-Corruption Layer.', 'd4444444-4444-4444-4444-444444444444'),
    ('True', false, 'XOR is not linearly separable; a single perceptron cannot solve it.', 'd5555555-5555-5555-5555-555555555555'),
    ('False', true, 'XOR requires a multi-layer network (MLP) due to its non-linear separability.', 'd5555555-5555-5555-5555-555555555555'),
    ('Gradient Descent', false, 'Gradient descent is an optimization algorithm, not specific to non-linear mapping.', 'd6666666-6666-6666-6666-666666666666'),
    ('The Kernel Trick', true, 'The Kernel Trick implicitly maps inputs into high-dimensional feature spaces.', 'd6666666-6666-6666-6666-666666666666'),
    ('Sigmoid Activation', false, 'Common in Neural Networks, but not the primary mechanism of standard SVMs.', 'd6666666-6666-6666-6666-666666666666'),
    ('Memory', false, 'Memory persists state between conversational turns.', 'd7777777-7777-7777-7777-777777777777'),
    ('Agent', true, 'Agents use LLMs as reasoning engines to determine which tools to call and in what order.', 'd7777777-7777-7777-7777-777777777777'),
    ('Chain', false, 'Chains are hardcoded sequences of calls, not dynamic reasoning loops.', 'd7777777-7777-7777-7777-777777777777'),
    ('True', false, 'Semantic search relies on comparing mathematical vector representations, not raw strings.', 'd8888888-8888-8888-8888-888888888888'),
    ('False', true, 'RAG retrieves context by comparing the query''s embedding vector against the database vectors.', 'd8888888-8888-8888-8888-888888888888'),
    ('FROM', true, 'FROM initializes a new build stage and sets the Base Image.', 'd9999999-9999-9999-9999-999999999999'),
    ('BASE', false, 'There is no BASE instruction in Dockerfiles.', 'd9999999-9999-9999-9999-999999999999'),
    ('ENV', false, 'ENV sets environment variables.', 'd9999999-9999-9999-9999-999999999999'),
    ('True', true, 'GitHub automatically detects and runs YAML workflows placed exactly in this directory.', 'daaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    ('False', false, 'Workflows placed outside `.github/workflows` will not be recognized by GitHub Actions.', 'daaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa');

-- 14. Insert Attempts (using valid hex 'e...')
INSERT INTO attempts (id, student_id, quiz_id, score, start_time, end_time, status)
VALUES
    ('e1111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', '91111111-1111-1111-1111-111111111111', 85.0, TO_TIMESTAMP('15-10-2025 09:10:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('15-10-2025 10:05:00', 'DD-MM-YYYY HH24:MI:SS'), 'COMPLETED'),
    ('e2222222-2222-2222-2222-222222222222', '55555555-5555-5555-5555-555555555555', '93333333-3333-3333-3333-333333333333', 90.0, TO_TIMESTAMP('16-02-2026 14:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('16-02-2026 14:28:00', 'DD-MM-YYYY HH24:MI:SS'), 'COMPLETED'),
    ('e3333333-3333-3333-3333-333333333333', '66666666-6666-6666-6666-666666666666', '95555555-5555-5555-5555-555555555555', NULL, TO_TIMESTAMP('05-11-2025 14:15:00', 'DD-MM-YYYY HH24:MI:SS'), NULL, 'IN_PROGRESS');

-- 15. Insert Junction Tables
INSERT INTO quiz_chapters (quiz_id, chapter_number, chapter_course_id)
VALUES
    ('91111111-1111-1111-1111-111111111111', 1, 'c1111111-1111-1111-1111-111111111111'),
    ('91111111-1111-1111-1111-111111111111', 2, 'c1111111-1111-1111-1111-111111111111'),
    ('92222222-2222-2222-2222-222222222222', 3, 'c1111111-1111-1111-1111-111111111111'),
    ('93333333-3333-3333-3333-333333333333', 1, 'c2222222-2222-2222-2222-222222222222'),
    ('94444444-4444-4444-4444-444444444444', 3, 'c2222222-2222-2222-2222-222222222222'),
    ('95555555-5555-5555-5555-555555555555', 1, 'c3333333-3333-3333-3333-333333333333'),
    ('95555555-5555-5555-5555-555555555555', 2, 'c3333333-3333-3333-3333-333333333333');

INSERT INTO quiz_learning_outcomes (quiz_id, learning_outcome_id)
VALUES
    ('91111111-1111-1111-1111-111111111111', '81111111-1111-1111-1111-111111111111'),
    ('91111111-1111-1111-1111-111111111111', '82222222-2222-2222-2222-222222222222'),
    ('92222222-2222-2222-2222-222222222222', '83333333-3333-3333-3333-333333333333'),
    ('93333333-3333-3333-3333-333333333333', '84444444-4444-4444-4444-444444444444'),
    ('94444444-4444-4444-4444-444444444444', '85555555-5555-5555-5555-555555555555'),
    ('95555555-5555-5555-5555-555555555555', '86666666-6666-6666-6666-666666666666'),
    ('95555555-5555-5555-5555-555555555555', '87777777-7777-7777-7777-777777777777');

INSERT INTO question_learning_outcomes (question_id, learning_outcome_id)
VALUES
    ('d1111111-1111-1111-1111-111111111111', '81111111-1111-1111-1111-111111111111'),
    ('d2222222-2222-2222-2222-222222222222', '82222222-2222-2222-2222-222222222222'),
    ('d3333333-3333-3333-3333-333333333333', '81111111-1111-1111-1111-111111111111'),
    ('d4444444-4444-4444-4444-444444444444', '83333333-3333-3333-3333-333333333333'),
    ('d5555555-5555-5555-5555-555555555555', '84444444-4444-4444-4444-444444444444'),
    ('d6666666-6666-6666-6666-666666666666', '84444444-4444-4444-4444-444444444444'),
    ('d7777777-7777-7777-7777-777777777777', '85555555-5555-5555-5555-555555555555'),
    ('d8888888-8888-8888-8888-888888888888', '85555555-5555-5555-5555-555555555555'),
    ('d9999999-9999-9999-9999-999999999999', '86666666-6666-6666-6666-666666666666'),
    ('daaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '87777777-7777-7777-7777-777777777777'),
    ('dbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '86666666-6666-6666-6666-666666666666');

INSERT INTO student_learning_outcomes (student_id, learning_outcome_id)
VALUES
    ('44444444-4444-4444-4444-444444444444', '81111111-1111-1111-1111-111111111111'),
    ('44444444-4444-4444-4444-444444444444', '82222222-2222-2222-2222-222222222222'),
    ('55555555-5555-5555-5555-555555555555', '84444444-4444-4444-4444-444444444444');