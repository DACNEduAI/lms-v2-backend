-- Set the default schema for the rest of this script!
SET search_path TO edu;

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
    ('11111111-1111-1111-1111-111111111111', 'EMP-DSA-01', '555-0199', 'Computer Science', 'Data Structures'),
    ('22222222-2222-2222-2222-222222222222', 'EMP-DSA-02', '555-0200', 'Computer Science', 'Algorithm Analysis'),
    ('33333333-3333-3333-3333-333333333333', 'EMP-DSA-03', '555-0201', 'Computer Science', 'Graph Algorithms');

-- 4. Insert Courses (using valid hex 'c...')
INSERT INTO courses (id, course_name, description, course_code, active, cover_image, start_date, end_date)
VALUES
    ('c1111111-1111-1111-1111-111111111111', 'Data Structures and Algorithm', 'Core data structures, algorithm design techniques, complexity analysis, sorting, searching, trees, graphs, hashing, and dynamic programming.', 'CS201', true, 'https://example.com/covers/cs201-dsa.jpg', TO_TIMESTAMP('01-09-2025 08:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('15-12-2025 17:00:00', 'DD-MM-YYYY HH24:MI:SS'));

-- 5. Insert Enrollments
INSERT INTO enrollments (course_id, user_id, enrolled_date)
VALUES
    -- Teachers assigned
    ('c1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', TO_TIMESTAMP('15-08-2025 09:00:00', 'DD-MM-YYYY HH24:MI:SS')),
    ('c1111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', TO_TIMESTAMP('15-08-2025 09:05:00', 'DD-MM-YYYY HH24:MI:SS')),
    ('c1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', TO_TIMESTAMP('15-08-2025 09:10:00', 'DD-MM-YYYY HH24:MI:SS')),
    -- Students
    ('c1111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', TO_TIMESTAMP('20-08-2025 10:00:00', 'DD-MM-YYYY HH24:MI:SS')),
    ('c1111111-1111-1111-1111-111111111111', '55555555-5555-5555-5555-555555555555', TO_TIMESTAMP('21-08-2025 11:30:00', 'DD-MM-YYYY HH24:MI:SS')),
    ('c1111111-1111-1111-1111-111111111111', '66666666-6666-6666-6666-666666666666', TO_TIMESTAMP('22-08-2025 09:15:00', 'DD-MM-YYYY HH24:MI:SS'));

-- 6. Insert Chapters
INSERT INTO chapters (chapter_id, title, description, course_id, created_at, updated_at)
VALUES
    ('5e055691-371a-4de5-b61a-f6d06712c36a', 'Độ phức tạp giải thuật', NULL, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 07:05:53.190081', '2026-07-19 07:20:21.950210'),
    ('8a01575f-a418-4107-b1b1-5eef75c5b72d', 'Giới thiệu môn Cấu trúc dữ liệu và giải thuật', NULL, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 07:05:41.769227', '2026-07-19 07:20:45.408590'),
    ('fe369f65-27f3-4314-9885-2099b0ca2fd8', 'Danh sách', NULL, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 07:06:23.214023', '2026-07-19 07:20:58.587976'),
    ('069331da-fc65-45f3-bb7f-e23f8f9a3a50', 'Cây nhị phân', NULL, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 07:21:29.051404', '2026-07-19 07:21:29.051404'),
    ('d8d35922-4538-4613-9fe8-5dcef719b520', 'Cấu trúc cây nâng cao', NULL, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 07:21:49.823482', '2026-07-19 07:21:49.823482'),
    ('69c3255e-fbd2-4eb7-9a78-a5147b0da42d', 'Heap', NULL, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 07:22:07.547732', '2026-07-19 07:22:07.547732'),
    ('6967be86-ae45-4e55-8faa-71318f50bbf6', 'Đồ thị', NULL, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 07:22:26.545267', '2026-07-19 07:22:26.545267'),
    ('c5d67446-27f5-4fcf-97cd-a6de6109114e', 'Ngăn xếp và Hàng đợi', NULL, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 07:19:51.782248', '2026-07-19 07:33:28.944068'),
    ('a8c06712-6aa5-4993-901b-77008af766d3', 'Giải thuật sắp xếp', NULL, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 07:22:20.266461', '2026-07-19 07:36:03.118431'),
    ('d1f07793-cfd2-450f-887a-abfc5b189b4b', 'Giải thuật tìm kiếm', NULL, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 07:22:38.409260', '2026-07-19 07:36:19.128899');

-- 7. Insert Group Learning Outcomes (using valid hex '7...')
INSERT INTO group_learning_outcomes (id, code, description, sort_order, course_id, created_at, updated_at)
VALUES
    ('71111111-1111-1111-1111-111111111111', 'L.O.1', 'Xác định được độ phức tạp của các giải thuật đơn giản (độ phức tạp đa thức trên vòng lặp lồng nhau - không đệ quy', 1, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 06:37:47.495347', '2026-07-19 07:14:25.820358'),
    ('72222222-2222-2222-2222-222222222222', 'L.O.2', 'Thao tác được các cấu trúc dữ liệu cơ bản như danh sách, cây, đồ thị.', 2, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 06:37:47.495347', '2026-07-19 07:14:25.852697'),
    ('73333333-3333-3333-3333-333333333333', 'L.O.3', 'Cài đặt được các giải thuật cơ bản như sắp xếp, tìm kiếm', 3, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 06:37:47.495347', '2026-07-19 07:14:25.881811');

-- 8. Insert Learning Outcomes (using valid hex '8...')
INSERT INTO learning_outcomes (id, prefix_number, description, sort_order, course_id, group_learning_outcome_id, created_at, updated_at)
VALUES
    ('81111111-1111-1111-1111-111111111111', 1, 'Nêu định nghĩa về độ phức tạp “Big O” của giải thuật.', 1, 'c1111111-1111-1111-1111-111111111111', '71111111-1111-1111-1111-111111111111', '2026-07-19 06:37:47.502246', '2026-07-19 07:14:25.919706'),
    ('82222222-2222-2222-2222-222222222222', 2, 'Xác định độ phức tạp của các giải thuật đơn giản có thời gian tính toán đa thức', 2, 'c1111111-1111-1111-1111-111111111111', '71111111-1111-1111-1111-111111111111', '2026-07-19 06:37:47.502246', '2026-07-19 07:14:25.919706'),
    ('84444444-4444-4444-4444-444444444444', 1, 'Thao tác được các cấu trúc dữ liệu cơ bản như danh sách, cây, đồ thị.', 1, 'c1111111-1111-1111-1111-111111111111', '72222222-2222-2222-2222-222222222222', '2026-07-19 06:37:47.502246', '2026-07-19 07:14:25.919706'),
    ('85555555-5555-5555-5555-555555555555', 2, 'Hiện thực được các phương thức cần thiết cho từng cấu trúc dữ liệu như: mảng, danh sách liên kết, chồng, hàng đợi, cây, đồ thị.', 2, 'c1111111-1111-1111-1111-111111111111', '72222222-2222-2222-2222-222222222222', '2026-07-19 06:37:47.502246', '2026-07-19 07:14:25.919706'),
    ('86666666-6666-6666-6666-666666666666', 1, 'Minh họa được hoạt động của các giải thuật tìm kiếm trên cấu trúc dữ liệu như: mảng, danh sách liên kết, chồng, hàng đợi, cây, đồ thị.', 1, 'c1111111-1111-1111-1111-111111111111', '73333333-3333-3333-3333-333333333333', '2026-07-19 06:37:47.502246', '2026-07-19 07:14:25.919706'),
    ('87777777-7777-7777-7777-777777777777', 2, 'Minh họa được từng bước hoạt động của các giải thuật sắp xếp trên một danh sách.', 2, 'c1111111-1111-1111-1111-111111111111', '73333333-3333-3333-3333-333333333333', '2026-07-19 06:37:47.502246', '2026-07-19 07:14:25.919706'),
    ('8854781e-efc8-4130-9451-3b0391a32419', 3, 'Hiện thực được các phương thức cần thiết và giải thuật đề xuất trên một cấu trúc dữ liệu cho sẵn để giải quyết bài toán', 3, 'c1111111-1111-1111-1111-111111111111', '73333333-3333-3333-3333-333333333333', '2026-07-19 07:06:38.928711', '2026-07-19 07:14:25.919706');

-- 9. Insert Chapter-Learning Outcome Relationships
INSERT INTO chapter_learning_outcomes (chapter_id, learning_outcome_id)
VALUES
    ('8a01575f-a418-4107-b1b1-5eef75c5b72d', '84444444-4444-4444-4444-444444444444'),
    ('5e055691-371a-4de5-b61a-f6d06712c36a', '81111111-1111-1111-1111-111111111111'),
    ('5e055691-371a-4de5-b61a-f6d06712c36a', '82222222-2222-2222-2222-222222222222'),
    ('fe369f65-27f3-4314-9885-2099b0ca2fd8', '84444444-4444-4444-4444-444444444444'),
    ('fe369f65-27f3-4314-9885-2099b0ca2fd8', '85555555-5555-5555-5555-555555555555'),
    ('fe369f65-27f3-4314-9885-2099b0ca2fd8', '8854781e-efc8-4130-9451-3b0391a32419'),
    ('c5d67446-27f5-4fcf-97cd-a6de6109114e', '84444444-4444-4444-4444-444444444444'),
    ('c5d67446-27f5-4fcf-97cd-a6de6109114e', '85555555-5555-5555-5555-555555555555'),
    ('c5d67446-27f5-4fcf-97cd-a6de6109114e', '8854781e-efc8-4130-9451-3b0391a32419'),
    ('069331da-fc65-45f3-bb7f-e23f8f9a3a50', '84444444-4444-4444-4444-444444444444'),
    ('069331da-fc65-45f3-bb7f-e23f8f9a3a50', '85555555-5555-5555-5555-555555555555'),
    ('069331da-fc65-45f3-bb7f-e23f8f9a3a50', '8854781e-efc8-4130-9451-3b0391a32419'),
    ('d8d35922-4538-4613-9fe8-5dcef719b520', '84444444-4444-4444-4444-444444444444'),
    ('d8d35922-4538-4613-9fe8-5dcef719b520', '85555555-5555-5555-5555-555555555555'),
    ('d8d35922-4538-4613-9fe8-5dcef719b520', '8854781e-efc8-4130-9451-3b0391a32419'),
    ('69c3255e-fbd2-4eb7-9a78-a5147b0da42d', '84444444-4444-4444-4444-444444444444'),
    ('69c3255e-fbd2-4eb7-9a78-a5147b0da42d', '85555555-5555-5555-5555-555555555555'),
    ('69c3255e-fbd2-4eb7-9a78-a5147b0da42d', '8854781e-efc8-4130-9451-3b0391a32419'),
    ('6967be86-ae45-4e55-8faa-71318f50bbf6', '84444444-4444-4444-4444-444444444444'),
    ('6967be86-ae45-4e55-8faa-71318f50bbf6', '85555555-5555-5555-5555-555555555555'),
    ('6967be86-ae45-4e55-8faa-71318f50bbf6', '86666666-6666-6666-6666-666666666666'),
    ('6967be86-ae45-4e55-8faa-71318f50bbf6', '8854781e-efc8-4130-9451-3b0391a32419'),
    ('a8c06712-6aa5-4993-901b-77008af766d3', '86666666-6666-6666-6666-666666666666'),
    ('a8c06712-6aa5-4993-901b-77008af766d3', '87777777-7777-7777-7777-777777777777'),
    ('a8c06712-6aa5-4993-901b-77008af766d3', '8854781e-efc8-4130-9451-3b0391a32419'),
    ('d1f07793-cfd2-450f-887a-abfc5b189b4b', '86666666-6666-6666-6666-666666666666'),
    ('d1f07793-cfd2-450f-887a-abfc5b189b4b', '87777777-7777-7777-7777-777777777777'),
    ('d1f07793-cfd2-450f-887a-abfc5b189b4b', '8854781e-efc8-4130-9451-3b0391a32419');

-- 10. Insert Exam Matrix
INSERT INTO exam_matrices (id, course_id, title, description, created_at, updated_at)
VALUES
    ('b1111111-1111-1111-1111-111111111111', 'c1111111-1111-1111-1111-111111111111', 'DSA Final Exam Matrix', 'Balanced final exam blueprint for Data Structures and Algorithm covering complexity analysis, core data structures, trees, graphs, greedy algorithms, and dynamic programming.', '2026-07-19 06:37:47.516609', '2026-07-19 07:44:49.120831');

-- 11. Insert Exam Matrix Rows
INSERT INTO exam_matrix_rows (id, matrix_id, chapter_id, topic, order_index)
VALUES
    ('8e513031-979a-49f1-9479-6cbfc325c97b', 'b1111111-1111-1111-1111-111111111111', '5e055691-371a-4de5-b61a-f6d06712c36a', 'Độ phức tạp giải thuật', 0),
    ('680a4f68-6203-41ea-bb53-d95152956b6d', 'b1111111-1111-1111-1111-111111111111', '5e055691-371a-4de5-b61a-f6d06712c36a', 'Độ phức tạp giải thuật', 1),
    ('a2cde69d-3faf-470b-a50a-dcf29b3e09e4', 'b1111111-1111-1111-1111-111111111111', 'fe369f65-27f3-4314-9885-2099b0ca2fd8', 'Danh sách', 2),
    ('ec938999-eead-47c2-b2f6-02d68c7865b9', 'b1111111-1111-1111-1111-111111111111', 'fe369f65-27f3-4314-9885-2099b0ca2fd8', 'Danh sách', 3),
    ('6ad52dd8-32ea-4f6f-a0c7-8545601f8031', 'b1111111-1111-1111-1111-111111111111', 'fe369f65-27f3-4314-9885-2099b0ca2fd8', 'Danh sách', 4),
    ('8ca0fdef-55cd-4dd8-bf61-5fbed2f1b8b4', 'b1111111-1111-1111-1111-111111111111', 'c5d67446-27f5-4fcf-97cd-a6de6109114e', 'Ngăn xếp và Hàng đợi', 5),
    ('7ea27159-f06a-4cb8-93b4-17a0d98279ab', 'b1111111-1111-1111-1111-111111111111', 'c5d67446-27f5-4fcf-97cd-a6de6109114e', 'Ngăn xếp và hàng đợi', 6),
    ('4078d7a9-6062-4f92-b84d-158ad29d90ed', 'b1111111-1111-1111-1111-111111111111', 'c5d67446-27f5-4fcf-97cd-a6de6109114e', 'Ngăn xếp và hàng đợi', 7),
    ('ec404589-3aeb-496c-ae2c-0d22ef6867de', 'b1111111-1111-1111-1111-111111111111', 'a8c06712-6aa5-4993-901b-77008af766d3', 'Giải thuật sắp xếp', 8),
    ('43f57a91-ce34-4d83-ba11-e7584bf57a88', 'b1111111-1111-1111-1111-111111111111', 'a8c06712-6aa5-4993-901b-77008af766d3', 'Giải thuật sắp xếp', 9),
    ('cb382f32-f447-4696-96fb-3c36be5b3c07', 'b1111111-1111-1111-1111-111111111111', 'a8c06712-6aa5-4993-901b-77008af766d3', 'Giải thuật sắp xếp', 10),
    ('cfde2627-dd69-4418-b687-9c279208a73d', 'b1111111-1111-1111-1111-111111111111', 'd1f07793-cfd2-450f-887a-abfc5b189b4b', 'Giải thuật tìm kiếm', 11),
    ('af0ce644-f164-414c-96c9-c8d604e14824', 'b1111111-1111-1111-1111-111111111111', 'd1f07793-cfd2-450f-887a-abfc5b189b4b', 'Giải thuật tìm kiếm', 12),
    ('6c213801-20e3-4a98-9c1f-7f58729d7acb', 'b1111111-1111-1111-1111-111111111111', 'd1f07793-cfd2-450f-887a-abfc5b189b4b', 'Giải thuật tìm kiếm', 13);

-- 11a. Insert Exam Matrix Row-Learning Outcome Relationships
INSERT INTO exam_matrix_row_learning_outcomes (exam_matrix_row_id, learning_outcome_id, is_selected)
VALUES
    ('8e513031-979a-49f1-9479-6cbfc325c97b', '81111111-1111-1111-1111-111111111111', TRUE),
    ('680a4f68-6203-41ea-bb53-d95152956b6d', '82222222-2222-2222-2222-222222222222', TRUE),
    ('a2cde69d-3faf-470b-a50a-dcf29b3e09e4', '84444444-4444-4444-4444-444444444444', TRUE),
    ('ec938999-eead-47c2-b2f6-02d68c7865b9', '85555555-5555-5555-5555-555555555555', TRUE),
    ('6ad52dd8-32ea-4f6f-a0c7-8545601f8031', '8854781e-efc8-4130-9451-3b0391a32419', TRUE),
    ('8ca0fdef-55cd-4dd8-bf61-5fbed2f1b8b4', '84444444-4444-4444-4444-444444444444', TRUE),
    ('7ea27159-f06a-4cb8-93b4-17a0d98279ab', '85555555-5555-5555-5555-555555555555', TRUE),
    ('4078d7a9-6062-4f92-b84d-158ad29d90ed', '8854781e-efc8-4130-9451-3b0391a32419', TRUE),
    ('ec404589-3aeb-496c-ae2c-0d22ef6867de', '86666666-6666-6666-6666-666666666666', TRUE),
    ('43f57a91-ce34-4d83-ba11-e7584bf57a88', '87777777-7777-7777-7777-777777777777', TRUE),
    ('cb382f32-f447-4696-96fb-3c36be5b3c07', '8854781e-efc8-4130-9451-3b0391a32419', TRUE),
    ('cfde2627-dd69-4418-b687-9c279208a73d', '86666666-6666-6666-6666-666666666666', TRUE),
    ('af0ce644-f164-414c-96c9-c8d604e14824', '87777777-7777-7777-7777-777777777777', TRUE),
    ('6c213801-20e3-4a98-9c1f-7f58729d7acb', '8854781e-efc8-4130-9451-3b0391a32419', TRUE);

INSERT INTO exam_matrix_row_learning_outcomes (exam_matrix_row_id, learning_outcome_id, is_selected)
SELECT matrix_row.id, chapter_outcome.learning_outcome_id, FALSE
FROM exam_matrix_rows matrix_row
JOIN chapter_learning_outcomes chapter_outcome
  ON chapter_outcome.chapter_id = matrix_row.chapter_id
ON CONFLICT (exam_matrix_row_id, learning_outcome_id) DO NOTHING;

-- 11b. Seed Custom Learning Levels for Existing Learning Outcomes
INSERT INTO custom_learning_levels (matrix_id, code, name, description, sort_order)
SELECT matrix.id, level.code, level.name, level.description, level.sort_order
FROM exam_matrices matrix
CROSS JOIN (
    VALUES
        ('CLL1', 'Understand', 'Legacy understand cognitive level', 1),
        ('CLL2', 'Apply', 'Legacy apply cognitive level', 2),
        ('CLL3', 'Apply Plus', 'Legacy advanced apply cognitive level', 3)
) AS level(code, name, description, sort_order);

INSERT INTO custom_learning_level_learning_outcomes
    (custom_learning_level_id, learning_outcome_id, is_selected)
SELECT custom_level.id, mapping.learning_outcome_id, TRUE
FROM (
    VALUES
        -- Understand: definition, foundational data-structure operation, and algorithm illustration.
        ('CLL1', '81111111-1111-1111-1111-111111111111'::uuid),
        ('CLL1', '84444444-4444-4444-4444-444444444444'::uuid),
        ('CLL1', '86666666-6666-6666-6666-666666666666'::uuid),
        -- Apply: complexity calculation, data-structure implementation, and sorting execution.
        ('CLL2', '82222222-2222-2222-2222-222222222222'::uuid),
        ('CLL2', '85555555-5555-5555-5555-555555555555'::uuid),
        ('CLL2', '87777777-7777-7777-7777-777777777777'::uuid),
        -- Apply Plus: advanced implementation for solving a supplied problem.
        ('CLL3', '8854781e-efc8-4130-9451-3b0391a32419'::uuid)
) AS mapping(level_code, learning_outcome_id)
JOIN learning_outcomes learning_outcome
  ON learning_outcome.id = mapping.learning_outcome_id
JOIN custom_learning_levels custom_level
  ON custom_level.matrix_id IN (
      SELECT matrix.id FROM exam_matrices matrix
      WHERE matrix.course_id = learning_outcome.course_id
  )
 AND custom_level.code = mapping.level_code;

INSERT INTO custom_learning_level_learning_outcomes
    (custom_learning_level_id, learning_outcome_id, is_selected)
SELECT custom_level.id, learning_outcome.id, FALSE
FROM custom_learning_levels custom_level
JOIN exam_matrices matrix ON matrix.id = custom_level.matrix_id
JOIN learning_outcomes learning_outcome ON learning_outcome.course_id = matrix.course_id
ON CONFLICT (custom_learning_level_id, learning_outcome_id) DO NOTHING;

-- 11c. Preserve Legacy Question Counts by Row and Custom Learning Level
INSERT INTO exam_matrix_row_learning_level_counts
    (exam_matrix_row_id, custom_learning_level_id, matrix_id, question_count)
SELECT legacy.exam_matrix_row_id,
       custom_level.id,
       matrix_row.matrix_id,
       level_count.question_count
FROM (
    VALUES
        ('8e513031-979a-49f1-9479-6cbfc325c97b'::uuid, '81111111-1111-1111-1111-111111111111'::uuid, 4, 0, 0),
        ('680a4f68-6203-41ea-bb53-d95152956b6d'::uuid, '82222222-2222-2222-2222-222222222222'::uuid, 0, 6, 0),
        ('a2cde69d-3faf-470b-a50a-dcf29b3e09e4'::uuid, '84444444-4444-4444-4444-444444444444'::uuid, 4, 0, 0),
        ('ec938999-eead-47c2-b2f6-02d68c7865b9'::uuid, '85555555-5555-5555-5555-555555555555'::uuid, 0, 4, 0),
        ('6ad52dd8-32ea-4f6f-a0c7-8545601f8031'::uuid, '8854781e-efc8-4130-9451-3b0391a32419'::uuid, 0, 0, 2),
        ('8ca0fdef-55cd-4dd8-bf61-5fbed2f1b8b4'::uuid, '84444444-4444-4444-4444-444444444444'::uuid, 3, 0, 0),
        ('7ea27159-f06a-4cb8-93b4-17a0d98279ab'::uuid, '85555555-5555-5555-5555-555555555555'::uuid, 0, 4, 0),
        ('4078d7a9-6062-4f92-b84d-158ad29d90ed'::uuid, '8854781e-efc8-4130-9451-3b0391a32419'::uuid, 0, 0, 1),
        ('ec404589-3aeb-496c-ae2c-0d22ef6867de'::uuid, '86666666-6666-6666-6666-666666666666'::uuid, 1, 0, 0),
        ('43f57a91-ce34-4d83-ba11-e7584bf57a88'::uuid, '87777777-7777-7777-7777-777777777777'::uuid, 0, 2, 0),
        ('cb382f32-f447-4696-96fb-3c36be5b3c07'::uuid, '8854781e-efc8-4130-9451-3b0391a32419'::uuid, 0, 0, 1),
        ('cfde2627-dd69-4418-b687-9c279208a73d'::uuid, '86666666-6666-6666-6666-666666666666'::uuid, 1, 0, 0),
        ('af0ce644-f164-414c-96c9-c8d604e14824'::uuid, '87777777-7777-7777-7777-777777777777'::uuid, 0, 2, 0),
        ('6c213801-20e3-4a98-9c1f-7f58729d7acb'::uuid, '8854781e-efc8-4130-9451-3b0391a32419'::uuid, 0, 0, 1)
) AS legacy(exam_matrix_row_id, learning_outcome_id, understand_count, apply_count, apply_plus_count)
CROSS JOIN LATERAL (
    VALUES
        ('Understand', legacy.understand_count),
        ('Apply', legacy.apply_count),
        ('Apply Plus', legacy.apply_plus_count)
) AS level_count(level_name, question_count)
JOIN exam_matrix_rows matrix_row ON matrix_row.id = legacy.exam_matrix_row_id
JOIN custom_learning_levels custom_level
  ON custom_level.matrix_id = matrix_row.matrix_id
 AND custom_level.name = level_count.level_name
WHERE level_count.question_count > 0;

-- 12. Insert Quizzes (using valid hex '9...')
INSERT INTO quizzes (id, quiz_title, unlimited, time_limit_minutes, pass_score, start_time, due_time, attempt_limited, course_id, teacher_id, display)
VALUES
    ('91111111-1111-1111-1111-111111111111', 'DSA Quiz 1: Complexity and Linear Structures', false, 60, 70.0, TO_TIMESTAMP('15-10-2025 09:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('15-10-2025 11:00:00', 'DD-MM-YYYY HH24:MI:SS'), 1, 'c1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', true),
    ('92222222-2222-2222-2222-222222222222', 'DSA Midterm: Sorting and Searching', false, 120, 75.0, TO_TIMESTAMP('10-11-2025 13:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('10-11-2025 15:00:00', 'DD-MM-YYYY HH24:MI:SS'), 1, 'c1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', true),
    ('93333333-3333-3333-3333-333333333333', 'DSA Quiz 2: Heaps and Priority Queues', false, 45, 70.0, TO_TIMESTAMP('20-11-2025 10:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('20-11-2025 11:00:00', 'DD-MM-YYYY HH24:MI:SS'), 2, 'c1111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', true),
    ('94444444-4444-4444-4444-444444444444', 'DSA Quiz 3: Trees and Graphs', false, 75, 80.0, TO_TIMESTAMP('01-12-2025 09:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('01-12-2025 10:30:00', 'DD-MM-YYYY HH24:MI:SS'), 2, 'c1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', true),
    ('95555555-5555-5555-5555-555555555555', 'DSA Final: Sorting, Searching, and Graphs', false, 120, 85.0, TO_TIMESTAMP('12-12-2025 14:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('12-12-2025 16:00:00', 'DD-MM-YYYY HH24:MI:SS'), 1, 'c1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', true);

-- 13. Insert Questions (using valid hex 'd...')
INSERT INTO questions (id, content, type, quiz_id)
VALUES
    ('d1111111-1111-1111-1111-111111111111', 'A binary search over a sorted array runs in O(log n) time.', 'TRUE_FALSE', '91111111-1111-1111-1111-111111111111'),
    ('d2222222-2222-2222-2222-222222222222', 'Which data structure provides LIFO access?', 'MULTIPLE_CHOICE', '91111111-1111-1111-1111-111111111111'),
    ('d3333333-3333-3333-3333-333333333333', 'Explain when you would choose a linked list over an array and include the trade-offs for insertion and random access.', 'ESSAY', '91111111-1111-1111-1111-111111111111'),
    ('d4444444-4444-4444-4444-444444444444', 'Merge sort uses a divide-and-conquer strategy and has O(n log n) worst-case time complexity.', 'TRUE_FALSE', '92222222-2222-2222-2222-222222222222'),
    ('d5555555-5555-5555-5555-555555555555', 'A binary heap is a complete binary tree that maintains the heap-order property.', 'TRUE_FALSE', '93333333-3333-3333-3333-333333333333'),
    ('d6666666-6666-6666-6666-666666666666', 'Which structure is commonly used to implement a priority queue?', 'MULTIPLE_CHOICE', '93333333-3333-3333-3333-333333333333'),
    ('d7777777-7777-7777-7777-777777777777', 'Which graph traversal finds shortest paths by edge count in an unweighted graph?', 'MULTIPLE_CHOICE', '94444444-4444-4444-4444-444444444444'),
    ('d8888888-8888-8888-8888-888888888888', 'In a binary search tree, an in-order traversal visits keys in sorted order.', 'TRUE_FALSE', '94444444-4444-4444-4444-444444444444'),
    ('d9999999-9999-9999-9999-999999999999', 'Which sorting algorithm guarantees O(n log n) worst-case time?', 'MULTIPLE_CHOICE', '95555555-5555-5555-5555-555555555555'),
    ('daaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Dijkstra''s algorithm can be used directly on graphs with negative edge weights.', 'TRUE_FALSE', '95555555-5555-5555-5555-555555555555'),
    ('dbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Describe how to implement breadth-first search for a graph, including the data structures used and its time complexity.', 'ESSAY', '95555555-5555-5555-5555-555555555555');

-- 14. Insert Answers
INSERT INTO answers (content, is_correct, explanation, question_id)
VALUES
    ('True', true, 'Binary search halves the remaining search interval each step, so it takes logarithmic time.', 'd1111111-1111-1111-1111-111111111111'),
    ('False', false, 'Linear search is O(n), but binary search on sorted data is O(log n).', 'd1111111-1111-1111-1111-111111111111'),
    ('Queue', false, 'A queue provides FIFO access: first in, first out.', 'd2222222-2222-2222-2222-222222222222'),
    ('Stack', true, 'A stack provides LIFO access: last in, first out.', 'd2222222-2222-2222-2222-222222222222'),
    ('Hash Table', false, 'A hash table maps keys to values and does not enforce LIFO behavior.', 'd2222222-2222-2222-2222-222222222222'),
    ('True', true, 'Merge sort recursively splits the input and merges sorted halves in O(n log n) time.', 'd4444444-4444-4444-4444-444444444444'),
    ('False', false, 'Merge sort has O(n log n) worst-case time, not quadratic worst-case behavior.', 'd4444444-4444-4444-4444-444444444444'),
    ('True', true, 'A binary heap combines the complete-tree shape property with min-heap or max-heap ordering.', 'd5555555-5555-5555-5555-555555555555'),
    ('False', false, 'Binary heaps must satisfy both the complete-tree shape property and the appropriate heap-order property.', 'd5555555-5555-5555-5555-555555555555'),
    ('Binary Heap', true, 'A binary heap supports efficient insertion and removal of the highest or lowest priority item.', 'd6666666-6666-6666-6666-666666666666'),
    ('Linked List', false, 'A linked list does not efficiently maintain priority order by itself.', 'd6666666-6666-6666-6666-666666666666'),
    ('Stack', false, 'A stack is useful for LIFO processing, not priority-based retrieval.', 'd6666666-6666-6666-6666-666666666666'),
    ('Depth-First Search', false, 'DFS explores deeply and does not guarantee shortest paths by edge count.', 'd7777777-7777-7777-7777-777777777777'),
    ('Breadth-First Search', true, 'BFS explores the graph level by level, giving shortest paths in unweighted graphs.', 'd7777777-7777-7777-7777-777777777777'),
    ('Topological Sort', false, 'Topological sort orders DAG vertices by dependency, but it is not a shortest-path traversal.', 'd7777777-7777-7777-7777-777777777777'),
    ('True', true, 'In-order traversal visits left subtree, node, then right subtree, which yields sorted keys in a BST.', 'd8888888-8888-8888-8888-888888888888'),
    ('False', false, 'Pre-order and post-order have different visit orders; in-order is the sorted traversal for BSTs.', 'd8888888-8888-8888-8888-888888888888'),
    ('Merge Sort', true, 'Merge sort guarantees O(n log n) time in the best, average, and worst cases.', 'd9999999-9999-9999-9999-999999999999'),
    ('Bubble Sort', false, 'Bubble sort has O(n squared) worst-case time.', 'd9999999-9999-9999-9999-999999999999'),
    ('Insertion Sort', false, 'Insertion sort has O(n squared) worst-case time.', 'd9999999-9999-9999-9999-999999999999'),
    ('True', false, 'Dijkstra''s algorithm assumes non-negative edge weights.', 'daaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    ('False', true, 'Graphs with negative edges require algorithms such as Bellman-Ford or careful reweighting.', 'daaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa');

-- 15. Insert Attempts (using valid hex 'e...')
INSERT INTO attempts (id, student_id, quiz_id, score, start_time, due_time, submitted_time, time_spent_seconds, status, error_reason)
VALUES
    ('e1111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', '91111111-1111-1111-1111-111111111111', 85.0, TO_TIMESTAMP('15-10-2025 09:10:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('15-10-2025 10:10:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('15-10-2025 10:05:00', 'DD-MM-YYYY HH24:MI:SS'), 3300, 'FINISHED', NULL),
    ('e2222222-2222-2222-2222-222222222222', '55555555-5555-5555-5555-555555555555', '93333333-3333-3333-3333-333333333333', 90.0, TO_TIMESTAMP('20-11-2025 10:05:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('20-11-2025 10:50:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('20-11-2025 10:43:00', 'DD-MM-YYYY HH24:MI:SS'), 2280, 'FINISHED', NULL),
    ('e3333333-3333-3333-3333-333333333333', '66666666-6666-6666-6666-666666666666', '95555555-5555-5555-5555-555555555555', NULL, TO_TIMESTAMP('12-12-2025 14:15:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('12-12-2025 16:15:00', 'DD-MM-YYYY HH24:MI:SS'), NULL, NULL, 'STARTED', NULL);

-- 16. Insert Junction Tables
INSERT INTO quiz_chapters (quiz_id, chapter_id)
VALUES
    ('91111111-1111-1111-1111-111111111111', '5e055691-371a-4de5-b61a-f6d06712c36a'),
    ('91111111-1111-1111-1111-111111111111', 'fe369f65-27f3-4314-9885-2099b0ca2fd8'),
    ('91111111-1111-1111-1111-111111111111', 'c5d67446-27f5-4fcf-97cd-a6de6109114e'),
    ('92222222-2222-2222-2222-222222222222', 'a8c06712-6aa5-4993-901b-77008af766d3'),
    ('92222222-2222-2222-2222-222222222222', 'd1f07793-cfd2-450f-887a-abfc5b189b4b'),
    ('93333333-3333-3333-3333-333333333333', '69c3255e-fbd2-4eb7-9a78-a5147b0da42d'),
    ('94444444-4444-4444-4444-444444444444', '069331da-fc65-45f3-bb7f-e23f8f9a3a50'),
    ('94444444-4444-4444-4444-444444444444', 'd8d35922-4538-4613-9fe8-5dcef719b520'),
    ('94444444-4444-4444-4444-444444444444', '6967be86-ae45-4e55-8faa-71318f50bbf6'),
    ('95555555-5555-5555-5555-555555555555', '6967be86-ae45-4e55-8faa-71318f50bbf6'),
    ('95555555-5555-5555-5555-555555555555', 'a8c06712-6aa5-4993-901b-77008af766d3'),
    ('95555555-5555-5555-5555-555555555555', 'd1f07793-cfd2-450f-887a-abfc5b189b4b');

INSERT INTO quiz_learning_outcomes (quiz_id, learning_outcome_id)
VALUES
    ('91111111-1111-1111-1111-111111111111', '81111111-1111-1111-1111-111111111111'),
    ('91111111-1111-1111-1111-111111111111', '84444444-4444-4444-4444-444444444444'),
    ('91111111-1111-1111-1111-111111111111', '85555555-5555-5555-5555-555555555555'),
    ('92222222-2222-2222-2222-222222222222', '87777777-7777-7777-7777-777777777777'),
    ('93333333-3333-3333-3333-333333333333', '84444444-4444-4444-4444-444444444444'),
    ('93333333-3333-3333-3333-333333333333', '85555555-5555-5555-5555-555555555555'),
    ('94444444-4444-4444-4444-444444444444', '85555555-5555-5555-5555-555555555555'),
    ('95555555-5555-5555-5555-555555555555', '86666666-6666-6666-6666-666666666666'),
    ('94444444-4444-4444-4444-444444444444', '86666666-6666-6666-6666-666666666666'),
    ('95555555-5555-5555-5555-555555555555', '87777777-7777-7777-7777-777777777777'),
    ('95555555-5555-5555-5555-555555555555', '8854781e-efc8-4130-9451-3b0391a32419');

INSERT INTO question_learning_outcomes (question_id, learning_outcome_id)
VALUES
    ('d1111111-1111-1111-1111-111111111111', '81111111-1111-1111-1111-111111111111'),
    ('d2222222-2222-2222-2222-222222222222', '84444444-4444-4444-4444-444444444444'),
    ('d3333333-3333-3333-3333-333333333333', '85555555-5555-5555-5555-555555555555'),
    ('d4444444-4444-4444-4444-444444444444', '87777777-7777-7777-7777-777777777777'),
    ('d5555555-5555-5555-5555-555555555555', '84444444-4444-4444-4444-444444444444'),
    ('d6666666-6666-6666-6666-666666666666', '85555555-5555-5555-5555-555555555555'),
    ('d7777777-7777-7777-7777-777777777777', '86666666-6666-6666-6666-666666666666'),
    ('d8888888-8888-8888-8888-888888888888', '85555555-5555-5555-5555-555555555555'),
    ('d9999999-9999-9999-9999-999999999999', '87777777-7777-7777-7777-777777777777'),
    ('daaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '86666666-6666-6666-6666-666666666666'),
    ('dbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '8854781e-efc8-4130-9451-3b0391a32419');
