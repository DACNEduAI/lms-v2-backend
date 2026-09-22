SET search_path TO edu, public;

BEGIN;

-- 1. Insert Users (Giữ nguyên thông tin người dùng cơ bản)
INSERT INTO users (id, email, password, first_name, last_name, avatar_url, role)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'martin.fowler@university.edu', '123456', 'Martin', 'Fowler', 'https://example.com/avatars/mf.jpg', 'TEACHER'),
    ('22222222-2222-2222-2222-222222222222', 'andrew.ng@university.edu', '123456', 'Andrew', 'Ng', 'https://example.com/avatars/an.jpg', 'TEACHER'),
    ('33333333-3333-3333-3333-333333333333', 'kelsey.hightower@university.edu', '123456', 'Kelsey', 'Hightower', 'https://example.com/avatars/kh.jpg', 'TEACHER'),
    ('44444444-4444-4444-4444-444444444444', 'alice.student@university.edu', '123456', 'Alice', 'Chen', 'https://example.com/avatars/ac.jpg', 'STUDENT'),
    ('55555555-5555-5555-5555-555555555555', 'bob.student@university.edu', '123456', 'Bob', 'Martin', 'https://example.com/avatars/bm.jpg', 'STUDENT'),
    ('66666666-6666-6666-6666-666666666666', 'carol.student@university.edu', '123456', 'Carol', 'Danvers', 'https://example.com/avatars/cd.jpg', 'STUDENT');

-- 2. Insert Students
INSERT INTO students (user_id, student_id, enrollment_year)
VALUES
    ('44444444-4444-4444-4444-444444444444', 'STU-2024-001', 2024),
    ('55555555-5555-5555-5555-555555555555', 'STU-2024-002', 2024),
    ('66666666-6666-6666-6666-666666666666', 'STU-2025-001', 2025);

-- 3. Insert Teachers
INSERT INTO teachers (user_id, employee_id, phone_number, department, specialization)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'EMP-DSA-01', '555-0199', 'Khoa học Máy tính', 'Cấu trúc Dữ liệu'),
    ('22222222-2222-2222-2222-222222222222', 'EMP-DSA-02', '555-0200', 'Khoa học Máy tính', 'Phân tích Giải thuật'),
    ('33333333-3333-3333-3333-333333333333', 'EMP-DSA-03', '555-0201', 'Khoa học Máy tính', 'Giải thuật Đồ thị');

-- 4. Insert Courses
INSERT INTO courses (id, course_name, description, course_code, active, cover_image, start_date, end_date)
VALUES
    ('c1111111-1111-1111-1111-111111111111', 'Data Structures and Algorithms', 'Cấu trúc dữ liệu cốt lõi, kỹ thuật thiết kế giải thuật, phân tích độ phức tạp, sắp xếp, tìm kiếm, cây, đồ thị, băm và quy hoạch động.', 'CO2003', true, 'https://example.com/covers/cs201-dsa.jpg', TO_TIMESTAMP('01-09-2025 08:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('15-12-2025 17:00:00', 'DD-MM-YYYY HH24:MI:SS'));

-- 5. Insert Enrollments
INSERT INTO enrollments (course_id, user_id, enrolled_date)
VALUES
    ('c1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', TO_TIMESTAMP('15-08-2025 09:00:00', 'DD-MM-YYYY HH24:MI:SS')),
    ('c1111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', TO_TIMESTAMP('15-08-2025 09:05:00', 'DD-MM-YYYY HH24:MI:SS')),
    ('c1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', TO_TIMESTAMP('15-08-2025 09:10:00', 'DD-MM-YYYY HH24:MI:SS')),
    ('c1111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', TO_TIMESTAMP('20-08-2025 10:00:00', 'DD-MM-YYYY HH24:MI:SS')),
    ('c1111111-1111-1111-1111-111111111111', '55555555-5555-5555-5555-555555555555', TO_TIMESTAMP('21-08-2025 11:30:00', 'DD-MM-YYYY HH24:MI:SS')),
    ('c1111111-1111-1111-1111-111111111111', '66666666-6666-6666-6666-666666666666', TO_TIMESTAMP('22-08-2025 09:15:00', 'DD-MM-YYYY HH24:MI:SS'));

-- 6. Insert Chapters (Cập nhật tiêu đề chương theo đề cương)
INSERT INTO chapters (chapter_id, title, description, course_id, created_at, updated_at)
VALUES
    ('8a01575f-a418-4107-b1b1-5eef75c5b72d', 'Giới thiệu', NULL, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 07:05:41.769227', '2026-07-19 07:20:45.408590'),
    ('5e055691-371a-4de5-b61a-f6d06712c36a', 'Độ phức tạp giải thuật', NULL, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 07:05:53.190081', '2026-07-19 07:20:21.950210'),
    ('fe369f65-27f3-4314-9885-2099b0ca2fd8', 'Danh sách', NULL, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 07:06:23.214023', '2026-07-19 07:20:58.587976'),
    ('c5d67446-27f5-4fcf-97cd-a6de6109114e', 'Chồng và Hàng', NULL, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 07:19:51.782248', '2026-07-19 07:33:28.944068'),
    ('069331da-fc65-45f3-bb7f-e23f8f9a3a50', 'Cây nhị phân', NULL, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 07:21:29.051404', '2026-07-19 07:21:29.051404'),
    ('d8d35922-4538-4613-9fe8-5dcef719b520', 'Cấu trúc cây nâng cao', NULL, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 07:21:49.823482', '2026-07-19 07:21:49.823482'),
    ('69c3255e-fbd2-4eb7-9a78-a5147b0da42d', 'Heap', NULL, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 07:22:07.547732', '2026-07-19 07:22:07.547732'),
    ('a8c06712-6aa5-4993-901b-77008af766d3', 'Sắp xếp', NULL, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 07:22:20.266461', '2026-07-19 07:36:03.118431'),
    ('6967be86-ae45-4e55-8faa-71318f50bbf6', 'Đồ thị', NULL, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 07:22:26.545267', '2026-07-19 07:22:26.545267'),
    ('d1f07793-cfd2-450f-887a-abfc5b189b4b', 'Tìm kiếm', NULL, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 07:22:38.409260', '2026-07-19 07:36:19.128899');

-- 7. Insert Group Learning Outcomes
INSERT INTO group_learning_outcomes (id, code, description, sort_order, course_id, created_at, updated_at)
VALUES
    ('71111111-1111-1111-1111-111111111111', 'L.O.1', 'Xác định được độ phức tạp của các giải thuật đơn giản (độ phức tạp đa thức trên vòng lặp lồng nhau - không đệ quy)', 1, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 06:37:47.495347', '2026-07-19 07:14:25.820358'),
    ('72222222-2222-2222-2222-222222222222', 'L.O.2', 'Thao tác được các cấu trúc dữ liệu cơ bản như danh sách, cây, đồ thị.', 2, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 06:37:47.495347', '2026-07-19 07:14:25.852697'),
    ('73333333-3333-3333-3333-333333333333', 'L.O.3', 'Cài đặt được các giải thuật cơ bản như sắp xếp, tìm kiếm', 3, 'c1111111-1111-1111-1111-111111111111', '2026-07-19 06:37:47.495347', '2026-07-19 07:14:25.881811');

-- 8. Insert Learning Outcomes
INSERT INTO learning_outcomes (id, prefix_number, description, sort_order, course_id, group_learning_outcome_id, created_at, updated_at)
VALUES
    ('81111111-1111-1111-1111-111111111111', 1, 'Nêu định nghĩa về độ phức tạp “Big O” của giải thuật.', 1, 'c1111111-1111-1111-1111-111111111111', '71111111-1111-1111-1111-111111111111', '2026-07-19 06:37:47.502246', '2026-07-19 07:14:25.919706'),
    ('82222222-2222-2222-2222-222222222222', 2, 'Xác định độ phức tạp của các giải thuật đơn giản có thời gian tính toán đa thức', 2, 'c1111111-1111-1111-1111-111111111111', '71111111-1111-1111-1111-111111111111', '2026-07-19 06:37:47.502246', '2026-07-19 07:14:25.919706'),
    ('84444444-4444-4444-4444-444444444444', 1, 'Nêu định nghĩa và biểu diễn được các cấu trúc dữ liệu cơ bản như: mảng, danh sách liên kết, chồng, hàng đợi, cây, đồ thị.', 1, 'c1111111-1111-1111-1111-111111111111', '72222222-2222-2222-2222-222222222222', '2026-07-19 06:37:47.502246', '2026-07-19 07:14:25.919706'),
    ('85555555-5555-5555-5555-555555555555', 2, 'Hiện thực được các phương thức cần thiết cho từng cấu trúc dữ liệu như: mảng, danh sách liên kết, chồng, hàng đợi, cây, đồ thị.', 2, 'c1111111-1111-1111-1111-111111111111', '72222222-2222-2222-2222-222222222222', '2026-07-19 06:37:47.502246', '2026-07-19 07:14:25.919706'),
    ('86666666-6666-6666-6666-666666666666', 1, 'Minh họa được hoạt động của các giải thuật tìm kiếm trên cấu trúc dữ liệu như: mảng, danh sách liên kết, chồng, hàng đợi, cây, đồ thị.', 1, 'c1111111-1111-1111-1111-111111111111', '73333333-3333-3333-3333-333333333333', '2026-07-19 06:37:47.502246', '2026-07-19 07:14:25.919706'),
    ('87777777-7777-7777-7777-777777777777', 2, 'Minh họa được từng bước hoạt động của các giải thuật sắp xếp trên một danh sách.', 2, 'c1111111-1111-1111-1111-111111111111', '73333333-3333-3333-3333-333333333333', '2026-07-19 06:37:47.502246', '2026-07-19 07:14:25.919706'),
    ('8854781e-efc8-4130-9451-3b0391a32419', 3, 'Hiện thực được các phương thức cần thiết và giải thuật đề xuất trên một cấu trúc dữ liệu cho sẵn để giải quyết bài toán', 3, 'c1111111-1111-1111-1111-111111111111', '73333333-3333-3333-3333-333333333333', '2026-07-19 07:06:38.928711', '2026-07-19 07:14:25.919706');

-- 9. Insert Chapter-Learning Outcome Relationships (Giữ nguyên)
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
    ('b1111111-1111-1111-1111-111111111111', 'c1111111-1111-1111-1111-111111111111', 'Ma trận đề thi cuối kỳ môn DSA', 'Bản thiết kế bài thi cuối kỳ cân bằng cho môn Data Structures and Algorithms bao gồm phân tích độ phức tạp, các cấu trúc dữ liệu cốt lõi, cây, đồ thị, giải thuật tham lam và quy hoạch động.', '2026-07-19 06:37:47.516609', '2026-07-19 07:44:49.120831');

-- 11. Insert Exam Matrix Rows
INSERT INTO exam_matrix_rows (id, matrix_id, chapter_id, topic, order_index)
VALUES
    ('8e513031-979a-49f1-9479-6cbfc325c97b', 'b1111111-1111-1111-1111-111111111111', '5e055691-371a-4de5-b61a-f6d06712c36a', 'Độ phức tạp giải thuật', 0),
    ('680a4f68-6203-41ea-bb53-d95152956b6d', 'b1111111-1111-1111-1111-111111111111', '5e055691-371a-4de5-b61a-f6d06712c36a', 'Độ phức tạp giải thuật', 1),
    ('a2cde69d-3faf-470b-a50a-dcf29b3e09e4', 'b1111111-1111-1111-1111-111111111111', 'fe369f65-27f3-4314-9885-2099b0ca2fd8', 'Danh sách', 2),
    ('ec938999-eead-47c2-b2f6-02d68c7865b9', 'b1111111-1111-1111-1111-111111111111', 'fe369f65-27f3-4314-9885-2099b0ca2fd8', 'Danh sách', 3),
    ('6ad52dd8-32ea-4f6f-a0c7-8545601f8031', 'b1111111-1111-1111-1111-111111111111', 'fe369f65-27f3-4314-9885-2099b0ca2fd8', 'Danh sách', 4),
    ('8ca0fdef-55cd-4dd8-bf61-5fbed2f1b8b4', 'b1111111-1111-1111-1111-111111111111', 'c5d67446-27f5-4fcf-97cd-a6de6109114e', 'Chồng và Hàng', 5),
    ('7ea27159-f06a-4cb8-93b4-17a0d98279ab', 'b1111111-1111-1111-1111-111111111111', 'c5d67446-27f5-4fcf-97cd-a6de6109114e', 'Chồng và Hàng', 6),
    ('4078d7a9-6062-4f92-b84d-158ad29d90ed', 'b1111111-1111-1111-1111-111111111111', 'c5d67446-27f5-4fcf-97cd-a6de6109114e', 'Chồng và Hàng', 7),
    ('ec404589-3aeb-496c-ae2c-0d22ef6867de', 'b1111111-1111-1111-1111-111111111111', 'a8c06712-6aa5-4993-901b-77008af766d3', 'Sắp xếp', 8),
    ('43f57a91-ce34-4d83-ba11-e7584bf57a88', 'b1111111-1111-1111-1111-111111111111', 'a8c06712-6aa5-4993-901b-77008af766d3', 'Sắp xếp', 9),
    ('cb382f32-f447-4696-96fb-3c36be5b3c07', 'b1111111-1111-1111-1111-111111111111', 'a8c06712-6aa5-4993-901b-77008af766d3', 'Sắp xếp', 10),
    ('cfde2627-dd69-4418-b687-9c279208a73d', 'b1111111-1111-1111-1111-111111111111', 'd1f07793-cfd2-450f-887a-abfc5b189b4b', 'Tìm kiếm', 11),
    ('af0ce644-f164-414c-96c9-c8d604e14824', 'b1111111-1111-1111-1111-111111111111', 'd1f07793-cfd2-450f-887a-abfc5b189b4b', 'Tìm kiếm', 12),
    ('6c213801-20e3-4a98-9c1f-7f58729d7acb', 'b1111111-1111-1111-1111-111111111111', 'd1f07793-cfd2-450f-887a-abfc5b189b4b', 'Tìm kiếm', 13);

-- 11a. Insert Exam Matrix Row-Learning Outcome Relationships (Giữ nguyên)
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
        ('CLL1', 'Hiểu', 'Có khả năng nắm bắt ý nghĩa, giải thích các khái niệm và diễn giải thông tin đã học.', 1),
        ('CLL2', 'Áp dụng', 'Có khả năng sử dụng kiến thức, sự kiện và kỹ thuật đã thu thập được để giải quyết các vấn đề thực tế.', 2),
        ('CLL3', 'Áp dụng nâng cao', 'Áp dụng nâng cao đòi hỏi tư duy phản biện, phân tích và giải quyết vấn đề phức tạp, nhiều bước.', 3)
) AS level(code, name, description, sort_order);

INSERT INTO custom_learning_level_learning_outcomes
(custom_learning_level_id, learning_outcome_id, is_selected)
SELECT custom_level.id, mapping.learning_outcome_id, TRUE
FROM (
         VALUES
             ('CLL1', '81111111-1111-1111-1111-111111111111'::uuid),
             ('CLL1', '84444444-4444-4444-4444-444444444444'::uuid),
             ('CLL1', '86666666-6666-6666-6666-666666666666'::uuid),
             ('CLL2', '82222222-2222-2222-2222-222222222222'::uuid),
             ('CLL2', '85555555-5555-5555-5555-555555555555'::uuid),
             ('CLL2', '87777777-7777-7777-7777-777777777777'::uuid),
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
(exam_matrix_row_id, custom_learning_level_id, matrix_id, question_type, question_count)
SELECT legacy.exam_matrix_row_id,
       custom_level.id,
       matrix_row.matrix_id,
       'MULTIPLE_CHOICE',
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
        ('Hiểu', legacy.understand_count),
        ('Áp dụng', legacy.apply_count),
        ('Áp dụng nâng cao', legacy.apply_plus_count)
    ) AS level_count(level_name, question_count)
         JOIN exam_matrix_rows matrix_row ON matrix_row.id = legacy.exam_matrix_row_id
         JOIN custom_learning_levels custom_level
              ON custom_level.matrix_id = matrix_row.matrix_id
                  AND custom_level.name = level_count.level_name
WHERE level_count.question_count > 0;

-- 11d. Published quick Exam Matrix: one MCQ for each learning level so the
-- Quiz2 flow can be exercised without waiting for a large generation job.
INSERT INTO exam_matrices
    (id, course_id, title, description, lifecycle_status, version, published_by, published_at, created_at, updated_at)
VALUES
    ('b2000001-0000-4000-8000-000000000001', 'c1111111-1111-1111-1111-111111111111',
     'Exam Matrix – Kiểm tra nhanh (3 câu)',
     'Matrix seed đã published để tạo nhanh Quiz2: mỗi mức học có đúng một câu trắc nghiệm.',
     'PUBLISHED', 1, '11111111-1111-1111-1111-111111111111',
     '2026-08-14 00:00:00', '2026-08-14 00:00:00', '2026-08-14 00:00:00');

INSERT INTO exam_matrix_rows (id, matrix_id, chapter_id, topic, order_index)
VALUES
    ('b2000021-0000-4000-8000-000000000001', 'b2000001-0000-4000-8000-000000000001',
     '5e055691-371a-4de5-b61a-f6d06712c36a', 'Độ phức tạp thuật toán', 0),
    ('b2000022-0000-4000-8000-000000000001', 'b2000001-0000-4000-8000-000000000001',
     '69c3255e-fbd2-4eb7-9a78-a5147b0da42d', 'Heap', 1),
    ('b2000023-0000-4000-8000-000000000001', 'b2000001-0000-4000-8000-000000000001',
     'fe369f65-27f3-4314-9885-2099b0ca2fd8', 'Danh sách', 2);

INSERT INTO custom_learning_levels (id, matrix_id, code, name, description, sort_order)
VALUES
    ('b2000011-0000-4000-8000-000000000001', 'b2000001-0000-4000-8000-000000000001',
     'CLL1', 'Hiểu', 'Nắm bắt và giải thích khái niệm đã học.', 1),
    ('b2000012-0000-4000-8000-000000000001', 'b2000001-0000-4000-8000-000000000001',
     'CLL2', 'Áp dụng', 'Vận dụng kiến thức để giải quyết bài toán.', 2),
    ('b2000013-0000-4000-8000-000000000001', 'b2000001-0000-4000-8000-000000000001',
     'CLL3', 'Áp dụng nâng cao', 'Phân tích và giải quyết bài toán nhiều bước.', 3);

INSERT INTO exam_matrix_row_learning_outcomes (exam_matrix_row_id, learning_outcome_id, is_selected)
VALUES
    ('b2000021-0000-4000-8000-000000000001', '81111111-1111-1111-1111-111111111111', TRUE),
    ('b2000022-0000-4000-8000-000000000001', '85555555-5555-5555-5555-555555555555', TRUE),
    ('b2000023-0000-4000-8000-000000000001', '8854781e-efc8-4130-9451-3b0391a32419', TRUE);

INSERT INTO custom_learning_level_learning_outcomes
    (custom_learning_level_id, learning_outcome_id, is_selected)
VALUES
    ('b2000011-0000-4000-8000-000000000001', '81111111-1111-1111-1111-111111111111', TRUE),
    ('b2000011-0000-4000-8000-000000000001', '82222222-2222-2222-2222-222222222222', TRUE),
    ('b2000012-0000-4000-8000-000000000001', '84444444-4444-4444-4444-444444444444', TRUE),
    ('b2000012-0000-4000-8000-000000000001', '85555555-5555-5555-5555-555555555555', TRUE),
    ('b2000013-0000-4000-8000-000000000001', '86666666-6666-6666-6666-666666666666', TRUE),
    ('b2000013-0000-4000-8000-000000000001', '87777777-7777-7777-7777-777777777777', TRUE),
    ('b2000013-0000-4000-8000-000000000001', '8854781e-efc8-4130-9451-3b0391a32419', TRUE);

INSERT INTO custom_learning_level_learning_outcomes
    (custom_learning_level_id, learning_outcome_id, is_selected)
SELECT level.id, outcome.id, FALSE
FROM custom_learning_levels level
         JOIN learning_outcomes outcome ON outcome.course_id = 'c1111111-1111-1111-1111-111111111111'
WHERE level.matrix_id = 'b2000001-0000-4000-8000-000000000001'
ON CONFLICT (custom_learning_level_id, learning_outcome_id) DO NOTHING;

INSERT INTO exam_matrix_row_learning_level_counts
    (exam_matrix_row_id, custom_learning_level_id, matrix_id, question_type, question_count)
VALUES
    ('b2000021-0000-4000-8000-000000000001', 'b2000011-0000-4000-8000-000000000001',
     'b2000001-0000-4000-8000-000000000001', 'MULTIPLE_CHOICE', 1),
    ('b2000022-0000-4000-8000-000000000001', 'b2000012-0000-4000-8000-000000000001',
     'b2000001-0000-4000-8000-000000000001', 'MULTIPLE_CHOICE', 1),
    ('b2000023-0000-4000-8000-000000000001', 'b2000013-0000-4000-8000-000000000001',
     'b2000001-0000-4000-8000-000000000001', 'MULTIPLE_CHOICE', 1);


-- 11f. Exam Matrix for Code Runner
INSERT INTO exam_matrices
    (id, course_id, title, description, lifecycle_status, version, published_by, published_at, created_at, updated_at)
VALUES
    ('b4000001-0000-4000-8000-000000000001', 'c1111111-1111-1111-1111-111111111111',
     'Exam Matrix – Code Runner',
     'Matrix cho bai tap Code Runner.',
     'PUBLISHED', 1, '11111111-1111-1111-1111-111111111111',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO exam_matrix_rows (id, matrix_id, chapter_id, topic, order_index)
VALUES
    ('b4000021-0000-4000-8000-000000000001', 'b4000001-0000-4000-8000-000000000001',
     '5e055691-371a-4de5-b61a-f6d06712c36a', 'Co ban', 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO custom_learning_levels (id, matrix_id, code, name, description, sort_order)
VALUES
    ('b4000011-0000-4000-8000-000000000001', 'b4000001-0000-4000-8000-000000000001',
     'CLL_CR', 'Thuc hanh', 'Ap dung code.', 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO exam_matrix_row_learning_outcomes (exam_matrix_row_id, learning_outcome_id, is_selected)
VALUES
    ('b4000021-0000-4000-8000-000000000001', '81111111-1111-1111-1111-111111111111', TRUE)
ON CONFLICT (exam_matrix_row_id, learning_outcome_id) DO NOTHING;

INSERT INTO custom_learning_level_learning_outcomes
    (custom_learning_level_id, learning_outcome_id, is_selected)
VALUES
    ('b4000011-0000-4000-8000-000000000001', '81111111-1111-1111-1111-111111111111', TRUE)
ON CONFLICT (custom_learning_level_id, learning_outcome_id) DO NOTHING;

INSERT INTO exam_matrix_row_learning_level_counts
    (exam_matrix_row_id, custom_learning_level_id, matrix_id, question_type, question_count)
VALUES
    ('b4000021-0000-4000-8000-000000000001', 'b4000011-0000-4000-8000-000000000001',
     'b4000001-0000-4000-8000-000000000001', 'CODE_RUNNER', 1)
ON CONFLICT (exam_matrix_row_id, custom_learning_level_id) DO NOTHING;

-- 11e. Matrix 2 seed data. These records are intentionally separate from the
-- existing Exam Matrix seed and are not available to Quiz2 or the AI pipeline.
INSERT INTO exam_matrices_2 (id, course_id, title, description)
VALUES
    ('b3000001-0000-4000-8000-000000000001', 'c1111111-1111-1111-1111-111111111111',
     'Matrix 2 - DSA midterm sample', 'Sample Matrix 2 with direct row-to-LO mapping.'),
    ('b3000001-0000-4000-8000-000000000002', 'c1111111-1111-1111-1111-111111111111',
     'Matrix 2 - DSA final sample', 'Second Matrix 2 sample for the course tab.');

INSERT INTO exam_matrix_2_custom_learning_levels (id, matrix_id, name, sort_order)
VALUES
    ('b3000011-0000-4000-8000-000000000001', 'b3000001-0000-4000-8000-000000000001', 'Understand', 0),
    ('b3000011-0000-4000-8000-000000000002', 'b3000001-0000-4000-8000-000000000001', 'Apply', 1),
    ('b3000011-0000-4000-8000-000000000003', 'b3000001-0000-4000-8000-000000000001', 'Advanced application', 2),
    ('b3000011-0000-4000-8000-000000000011', 'b3000001-0000-4000-8000-000000000002', 'Recall', 0),
    ('b3000011-0000-4000-8000-000000000012', 'b3000001-0000-4000-8000-000000000002', 'Problem solving', 1);

INSERT INTO exam_matrix_2_rows
    (id, matrix_id, chapter_id, mapped_learning_outcome_id, sort_order)
VALUES
    ('b3000021-0000-4000-8000-000000000001', 'b3000001-0000-4000-8000-000000000001',
     '5e055691-371a-4de5-b61a-f6d06712c36a', '81111111-1111-1111-1111-111111111111', 0),
    ('b3000021-0000-4000-8000-000000000002', 'b3000001-0000-4000-8000-000000000001',
     'fe369f65-27f3-4314-9885-2099b0ca2fd8', '84444444-4444-4444-4444-444444444444', 1),
    ('b3000021-0000-4000-8000-000000000011', 'b3000001-0000-4000-8000-000000000002',
     '69c3255e-fbd2-4eb7-9a78-a5147b0da42d', '85555555-5555-5555-5555-555555555555', 0),
    ('b3000021-0000-4000-8000-000000000012', 'b3000001-0000-4000-8000-000000000002',
     'a8c06712-6aa5-4993-901b-77008af766d3', '86666666-6666-6666-6666-666666666666', 1);

INSERT INTO exam_matrix_2_cell_counts (row_id, custom_learning_level_id, question_count)
VALUES
    ('b3000021-0000-4000-8000-000000000001', 'b3000011-0000-4000-8000-000000000001', 0),
    ('b3000021-0000-4000-8000-000000000001', 'b3000011-0000-4000-8000-000000000002', 1),
    ('b3000021-0000-4000-8000-000000000002', 'b3000011-0000-4000-8000-000000000001', 0),
    ('b3000021-0000-4000-8000-000000000002', 'b3000011-0000-4000-8000-000000000003', 2),
    ('b3000021-0000-4000-8000-000000000011', 'b3000011-0000-4000-8000-000000000011', 1),
    ('b3000021-0000-4000-8000-000000000011', 'b3000011-0000-4000-8000-000000000012', 2),
    ('b3000021-0000-4000-8000-000000000012', 'b3000011-0000-4000-8000-000000000012', 3);

-- Matrix 2 dedicated to Quiz3 Code Runner. Both allocated cells use
-- CODE_RUNNER, so a new assessment produces two C++17 questions by default.
INSERT INTO exam_matrices_2 (id, course_id, title, description)
VALUES
    ('b3000001-0000-4000-8000-000000000003', 'c1111111-1111-1111-1111-111111111111',
     'Matrix 2 - DSA Code Runner', 'Two C++17 Code Runner questions: heap construction and insertion sort.');

INSERT INTO exam_matrix_2_custom_learning_levels (id, matrix_id, name, sort_order)
VALUES
    ('b3000011-0000-4000-8000-000000000021', 'b3000001-0000-4000-8000-000000000003', 'Implementation', 0);

INSERT INTO exam_matrix_2_rows
    (id, matrix_id, chapter_id, mapped_learning_outcome_id, sort_order)
VALUES
    ('b3000021-0000-4000-8000-000000000021', 'b3000001-0000-4000-8000-000000000003',
     '69c3255e-fbd2-4eb7-9a78-a5147b0da42d', '85555555-5555-5555-5555-555555555555', 0),
    ('b3000021-0000-4000-8000-000000000022', 'b3000001-0000-4000-8000-000000000003',
     'a8c06712-6aa5-4993-901b-77008af766d3', '85555555-5555-5555-5555-555555555555', 1);

INSERT INTO exam_matrix_2_cell_counts
    (row_id, custom_learning_level_id, question_count, question_type)
VALUES
    ('b3000021-0000-4000-8000-000000000021', 'b3000011-0000-4000-8000-000000000021', 1, 'CODE_RUNNER'),
    ('b3000021-0000-4000-8000-000000000022', 'b3000011-0000-4000-8000-000000000021', 1, 'CODE_RUNNER');

-- Comprehensive 40-question final exam matrix. Every DSA learning outcome is
-- represented by one row; the row totals are 6, 6, 6, 6, 6, 5, and 5.
INSERT INTO exam_matrices_2 (id, course_id, title, description)
VALUES
    ('b3000001-0000-4000-8000-000000000004', 'c1111111-1111-1111-1111-111111111111',
     'Matrix 2 - DSA comprehensive final (40 questions)',
     'A 40-question final exam matrix covering every learning outcome in Data Structures and Algorithms.');

INSERT INTO exam_matrix_2_custom_learning_levels (id, matrix_id, name, sort_order)
VALUES
    ('b3000011-0000-4000-8000-000000000031', 'b3000001-0000-4000-8000-000000000004', 'Understand', 0),
    ('b3000011-0000-4000-8000-000000000032', 'b3000001-0000-4000-8000-000000000004', 'Apply', 1),
    ('b3000011-0000-4000-8000-000000000033', 'b3000001-0000-4000-8000-000000000004', 'Advanced application', 2);

INSERT INTO exam_matrix_2_rows
    (id, matrix_id, chapter_id, mapped_learning_outcome_id, sort_order)
VALUES
    -- LO 1: Define Big O.
    ('b3000021-0000-4000-8000-000000000031', 'b3000001-0000-4000-8000-000000000004',
     '5e055691-371a-4de5-b61a-f6d06712c36a', '81111111-1111-1111-1111-111111111111', 0),
    -- LO 2: Determine polynomial-time complexity.
    ('b3000021-0000-4000-8000-000000000032', 'b3000001-0000-4000-8000-000000000004',
     '5e055691-371a-4de5-b61a-f6d06712c36a', '82222222-2222-2222-2222-222222222222', 1),
    -- LO 3: Define and represent fundamental data structures.
    ('b3000021-0000-4000-8000-000000000033', 'b3000001-0000-4000-8000-000000000004',
     '8a01575f-a418-4107-b1b1-5eef75c5b72d', '84444444-4444-4444-4444-444444444444', 2),
    -- LO 4: Implement fundamental data-structure operations.
    ('b3000021-0000-4000-8000-000000000034', 'b3000001-0000-4000-8000-000000000004',
     'fe369f65-27f3-4314-9885-2099b0ca2fd8', '85555555-5555-5555-5555-555555555555', 3),
    -- LO 5: Demonstrate searching algorithms.
    ('b3000021-0000-4000-8000-000000000035', 'b3000001-0000-4000-8000-000000000004',
     'd1f07793-cfd2-450f-887a-abfc5b189b4b', '86666666-6666-6666-6666-666666666666', 4),
    -- LO 6: Demonstrate sorting algorithms.
    ('b3000021-0000-4000-8000-000000000036', 'b3000001-0000-4000-8000-000000000004',
     'a8c06712-6aa5-4993-901b-77008af766d3', '87777777-7777-7777-7777-777777777777', 5),
    -- LO 7: Implement a proposed algorithm for a given data structure.
    ('b3000021-0000-4000-8000-000000000037', 'b3000001-0000-4000-8000-000000000004',
     '6967be86-ae45-4e55-8faa-71318f50bbf6', '8854781e-efc8-4130-9451-3b0391a32419', 6);

INSERT INTO exam_matrix_2_cell_counts
    (row_id, custom_learning_level_id, question_count)
VALUES
    -- LO 1 total: 6.
    ('b3000021-0000-4000-8000-000000000031', 'b3000011-0000-4000-8000-000000000031', 2),
    ('b3000021-0000-4000-8000-000000000031', 'b3000011-0000-4000-8000-000000000032', 3),
    ('b3000021-0000-4000-8000-000000000031', 'b3000011-0000-4000-8000-000000000033', 1),
    -- LO 2 total: 6.
    ('b3000021-0000-4000-8000-000000000032', 'b3000011-0000-4000-8000-000000000031', 2),
    ('b3000021-0000-4000-8000-000000000032', 'b3000011-0000-4000-8000-000000000032', 3),
    ('b3000021-0000-4000-8000-000000000032', 'b3000011-0000-4000-8000-000000000033', 1),
    -- LO 3 total: 6.
    ('b3000021-0000-4000-8000-000000000033', 'b3000011-0000-4000-8000-000000000031', 2),
    ('b3000021-0000-4000-8000-000000000033', 'b3000011-0000-4000-8000-000000000032', 3),
    ('b3000021-0000-4000-8000-000000000033', 'b3000011-0000-4000-8000-000000000033', 1),
    -- LO 4 total: 6.
    ('b3000021-0000-4000-8000-000000000034', 'b3000011-0000-4000-8000-000000000031', 1),
    ('b3000021-0000-4000-8000-000000000034', 'b3000011-0000-4000-8000-000000000032', 3),
    ('b3000021-0000-4000-8000-000000000034', 'b3000011-0000-4000-8000-000000000033', 2),
    -- LO 5 total: 6.
    ('b3000021-0000-4000-8000-000000000035', 'b3000011-0000-4000-8000-000000000031', 1),
    ('b3000021-0000-4000-8000-000000000035', 'b3000011-0000-4000-8000-000000000032', 3),
    ('b3000021-0000-4000-8000-000000000035', 'b3000011-0000-4000-8000-000000000033', 2),
    -- LO 6 total: 5.
    ('b3000021-0000-4000-8000-000000000036', 'b3000011-0000-4000-8000-000000000031', 2),
    ('b3000021-0000-4000-8000-000000000036', 'b3000011-0000-4000-8000-000000000032', 2),
    ('b3000021-0000-4000-8000-000000000036', 'b3000011-0000-4000-8000-000000000033', 1),
    -- LO 7 total: 5.
    ('b3000021-0000-4000-8000-000000000037', 'b3000011-0000-4000-8000-000000000031', 1),
    ('b3000021-0000-4000-8000-000000000037', 'b3000011-0000-4000-8000-000000000032', 2),
    ('b3000021-0000-4000-8000-000000000037', 'b3000011-0000-4000-8000-000000000033', 2);

-- 12. Insert Quizzes
INSERT INTO quizzes (id, quiz_title, unlimited, time_limit_minutes, pass_score, start_time, due_time, attempt_limited, course_id, teacher_id, display)
VALUES
    ('91111111-1111-1111-1111-111111111111', 'DSA Quiz 1: Complexity and Linear Structures', false, 60, 70.0, TO_TIMESTAMP('15-10-2025 09:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('15-10-2025 11:00:00', 'DD-MM-YYYY HH24:MI:SS'), 1, 'c1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', true),
    ('92222222-2222-2222-2222-222222222222', 'DSA Midterm: Sorting and Searching', false, 120, 75.0, TO_TIMESTAMP('10-11-2025 13:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('10-11-2025 15:00:00', 'DD-MM-YYYY HH24:MI:SS'), 1, 'c1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', true),
    ('93333333-3333-3333-3333-333333333333', 'DSA Quiz 2: Heaps and Priority Queues', false, 45, 70.0, TO_TIMESTAMP('20-11-2025 10:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('20-11-2025 11:00:00', 'DD-MM-YYYY HH24:MI:SS'), 2, 'c1111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', true),
    ('94444444-4444-4444-4444-444444444444', 'DSA Quiz 3: Trees and Graphs', false, 75, 80.0, TO_TIMESTAMP('01-12-2025 09:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('01-12-2025 10:30:00', 'DD-MM-YYYY HH24:MI:SS'), 2, 'c1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', true),
    ('95555555-5555-5555-5555-555555555555', 'DSA Final Exam: Sorting, Searching, and Graphs', false, 120, 85.0, TO_TIMESTAMP('12-12-2025 14:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('12-12-2025 16:00:00', 'DD-MM-YYYY HH24:MI:SS'), 1, 'c1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', true);

-- 13. Insert Questions
INSERT INTO questions (id, content, type, quiz_id)
VALUES
    ('d1111111-1111-1111-1111-111111111111', 'Tìm kiếm nhị phân trên một mảng đã sắp xếp chạy trong thời gian O(log n).', 'TRUE_FALSE', '91111111-1111-1111-1111-111111111111'),
    ('d2222222-2222-2222-2222-222222222222', 'Cấu trúc dữ liệu nào cung cấp truy cập LIFO?', 'MULTIPLE_CHOICE', '91111111-1111-1111-1111-111111111111'),
    ('d3333333-3333-3333-3333-333333333333', 'Giải thích khi nào bạn sẽ chọn danh sách liên kết thay vì mảng và bao gồm các đánh đổi cho việc chèn và truy cập ngẫu nhiên.', 'ESSAY', '91111111-1111-1111-1111-111111111111'),
    ('d4444444-4444-4444-4444-444444444444', 'Sắp xếp trộn sử dụng chiến lược chia để trị và có độ phức tạp thời gian trường hợp xấu nhất là O(n log n).', 'TRUE_FALSE', '92222222-2222-2222-2222-222222222222'),
    ('d5555555-5555-5555-5555-555555555555', 'Binary heap là một cây nhị phân hoàn chỉnh duy trì thuộc tính thứ tự heap.', 'TRUE_FALSE', '93333333-3333-3333-3333-333333333333'),
    ('d6666666-6666-6666-6666-666666666666', 'Cấu trúc nào thường được sử dụng để hiện thực hàng đợi ưu tiên?', 'MULTIPLE_CHOICE', '93333333-3333-3333-3333-333333333333'),
    ('d7777777-7777-7777-7777-777777777777', 'Duyệt đồ thị nào tìm đường đi ngắn nhất theo số lượng cạnh trong một đồ thị không có trọng số?', 'MULTIPLE_CHOICE', '94444444-4444-4444-4444-444444444444'),
    ('d8888888-8888-8888-8888-888888888888', 'Trong một cây nhị phân tìm kiếm, thao tác duyệt in-order (trung tự) truy cập các khóa theo thứ tự đã sắp xếp.', 'TRUE_FALSE', '94444444-4444-4444-4444-444444444444'),
    ('d9999999-9999-9999-9999-999999999999', 'Giải thuật sắp xếp nào đảm bảo thời gian trường hợp xấu nhất là O(n log n)?', 'MULTIPLE_CHOICE', '95555555-5555-5555-5555-555555555555'),
    ('daaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Giải thuật Dijkstra có thể được sử dụng trực tiếp trên các đồ thị có trọng số cạnh âm.', 'TRUE_FALSE', '95555555-5555-5555-5555-555555555555'),
    ('dbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Mô tả cách hiện thực duyệt theo chiều rộng (BFS) cho một đồ thị, bao gồm các cấu trúc dữ liệu được sử dụng và độ phức tạp thời gian của nó.', 'ESSAY', '95555555-5555-5555-5555-555555555555');

-- 14. Insert Answers
INSERT INTO answers (content, is_correct, explanation, question_id)
VALUES
    ('True', true, 'Binary search halves the search space at each step, thus taking logarithmic time.', 'd1111111-1111-1111-1111-111111111111'),
    ('False', false, 'Linear search is O(n), but binary search on sorted data is O(log n).', 'd1111111-1111-1111-1111-111111111111'),
    ('Queue', false, 'A queue provides FIFO access: first-in, first-out.', 'd2222222-2222-2222-2222-222222222222'),
    ('Stack', true, 'A stack provides LIFO access: last-in, first-out.', 'd2222222-2222-2222-2222-222222222222'),
    ('Hash Table', false, 'A hash table maps keys to values and doesn''t enforce LIFO behavior.', 'd2222222-2222-2222-2222-222222222222'),
    ('True', true, 'Merge sort recursively divides the input and merges the sorted halves in O(n log n) time.', 'd4444444-4444-4444-4444-444444444444'),
    ('False', false, 'Merge sort has a worst-case time of O(n log n), not quadratic behavior.', 'd4444444-4444-4444-4444-444444444444'),
    ('True', true, 'A binary heap combines a complete tree structure property with min-heap or max-heap ordering.', 'd5555555-5555-5555-5555-555555555555'),
    ('False', false, 'Binary heaps must satisfy both the complete tree structure property and the heap order property.', 'd5555555-5555-5555-5555-555555555555'),
    ('Binary Heap', true, 'A binary heap efficiently supports insertion and removal of the highest or lowest priority elements.', 'd6666666-6666-6666-6666-666666666666'),
    ('Danh sách liên kết (Linked List)', false, 'A linked list itself doesn''t maintain priority order efficiently.', 'd6666666-6666-6666-6666-666666666666'),
    ('Stack', false, 'A stack is useful for LIFO processing, not for priority-based retrieval.', 'd6666666-6666-6666-6666-666666666666'),
    ('Depth-First Search (DFS)', false, 'DFS explores as deep as possible and doesn''t guarantee finding the shortest path in terms of edges.', 'd7777777-7777-7777-7777-777777777777'),
    ('Breadth-First Search (BFS)', true, 'BFS explores the graph level by level, yielding the shortest path in unweighted graphs.', 'd7777777-7777-7777-7777-777777777777'),
    ('Topological Sort', false, 'Topological sort orders DAG vertices by dependencies, but it''s not a traversal method for finding shortest paths.', 'd7777777-7777-7777-7777-777777777777'),
    ('True', true, 'In-order traversal visits the left subtree, root, then right subtree, which produces sorted keys in a BST.', 'd8888888-8888-8888-8888-888888888888'),
    ('False', false, 'Pre-order and post-order visit nodes differently; in-order is the sorted traversal for BSTs.', 'd8888888-8888-8888-8888-888888888888'),
    ('Merge Sort', true, 'Merge sort guarantees O(n log n) time in the best, average, and worst cases.', 'd9999999-9999-9999-9999-999999999999'),
    ('Sắp xếp nổi bọt (Bubble Sort)', false, 'Sắp xếp nổi bọt có thời gian trường hợp xấu nhất là O(n^2).', 'd9999999-9999-9999-9999-999999999999'),
    ('Insertion Sort', false, 'Insertion sort has a worst-case time complexity of O(n^2).', 'd9999999-9999-9999-9999-999999999999'),
    ('True', false, 'Dijkstra''s algorithm assumes non-negative edge weights.', 'daaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    ('False', true, 'Graphs with negative weights require algorithms like Bellman-Ford or careful weight adjustments.', 'daaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa');


-- 14.5. Insert Quiz Releases
INSERT INTO quiz_releases (id, quiz_id, release_number, quiz_version, active, published_at)
VALUES
    ('c1111111-1111-1111-1111-111111111111', '91111111-1111-1111-1111-111111111111', 1, 1, true, CURRENT_TIMESTAMP),
    ('c2222222-2222-2222-2222-222222222222', '93333333-3333-3333-3333-333333333333', 1, 1, true, CURRENT_TIMESTAMP),
    ('c3333333-3333-3333-3333-333333333333', '95555555-5555-5555-5555-555555555555', 1, 1, true, CURRENT_TIMESTAMP);

-- 15. Insert Attempts
INSERT INTO attempts (id, student_id, quiz_id, quiz_release_id, score, start_time, due_time, submitted_time, time_spent_seconds, status, error_reason)
VALUES
    ('e1111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', '91111111-1111-1111-1111-111111111111', 'c1111111-1111-1111-1111-111111111111', 85.0, TO_TIMESTAMP('15-10-2025 09:10:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('15-10-2025 10:10:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('15-10-2025 10:05:00', 'DD-MM-YYYY HH24:MI:SS'), 3300, 'FINISHED', NULL),
    ('e2222222-2222-2222-2222-222222222222', '55555555-5555-5555-5555-555555555555', '93333333-3333-3333-3333-333333333333', 'c2222222-2222-2222-2222-222222222222', 90.0, TO_TIMESTAMP('20-11-2025 10:05:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('20-11-2025 10:50:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('20-11-2025 10:43:00', 'DD-MM-YYYY HH24:MI:SS'), 2280, 'FINISHED', NULL),
    ('e3333333-3333-3333-3333-333333333333', '66666666-6666-6666-6666-666666666666', '95555555-5555-5555-5555-555555555555', 'c3333333-3333-3333-3333-333333333333', NULL, TO_TIMESTAMP('12-12-2025 14:15:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('12-12-2025 16:15:00', 'DD-MM-YYYY HH24:MI:SS'), NULL, NULL, 'STARTED', NULL);

-- 16. Insert Junction Tables (Giữ nguyên)
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
    ('daaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '86666666-6666-6666-6666-666666666666') ON CONFLICT (question_id, learning_outcome_id) DO NOTHING;

-- 16a. Always-available DSA practice quizzes.
-- Each quiz has 20 questions.  A staged seed keeps the question, answer, and
-- learning-outcome mappings derived from the same source of truth: every
-- question has exactly one learning outcome and every quiz covers all seven.
CREATE TEMP TABLE dsa_practice_quiz_seed_questions
(
    question_id         UUID PRIMARY KEY,
    quiz_id             UUID NOT NULL,
    content             TEXT NOT NULL,
    learning_outcome_id UUID NOT NULL,
    correct_answer      TEXT NOT NULL,
    wrong_answer_1      TEXT NOT NULL,
    wrong_answer_2      TEXT NOT NULL,
    wrong_answer_3      TEXT NOT NULL
) ON COMMIT DROP;

INSERT INTO dsa_practice_quiz_seed_questions
    (question_id, quiz_id, content, learning_outcome_id, correct_answer,
     wrong_answer_1, wrong_answer_2, wrong_answer_3)
VALUES
    -- Practice Quiz A
    ('f1110001-0000-4000-8000-000000000001', 'f1100001-0000-4000-8000-000000000001', 'Big O ký hiệu điều gì về một giải thuật?', '81111111-1111-1111-1111-111111111111', 'Cận trên tiệm cận của tốc độ tăng thời gian hoặc bộ nhớ', 'Số dòng mã nguồn', 'Giá trị nhỏ nhất của đầu vào', 'Số lần chương trình biên dịch'),
    ('f1110001-0000-4000-8000-000000000002', 'f1100001-0000-4000-8000-000000000001', 'Khi phân tích Big O, các hằng số nhân dương thường được xử lý như thế nào?', '81111111-1111-1111-1111-111111111111', 'Bỏ qua vì chúng không đổi tốc độ tăng tiệm cận', 'Nhân vào kích thước đầu vào', 'Đổi thành O(1)', 'Cộng vào mọi số mũ'),
    ('f1110001-0000-4000-8000-000000000003', 'f1100001-0000-4000-8000-000000000001', 'Biểu thức nào mô tả thời gian tuyến tính?', '81111111-1111-1111-1111-111111111111', 'O(n)', 'O(1)', 'O(log n)', 'O(n²)'),
    ('f1110001-0000-4000-8000-000000000004', 'f1100001-0000-4000-8000-000000000001', 'Hai vòng lặp lồng nhau, mỗi vòng chạy n lần, có độ phức tạp thời gian nào?', '82222222-2222-2222-2222-222222222222', 'O(n²)', 'O(n)', 'O(log n)', 'O(1)'),
    ('f1110001-0000-4000-8000-000000000005', 'f1100001-0000-4000-8000-000000000001', 'Một vòng lặp tăng i từ 1 lên n với i *= 2 sau mỗi bước có độ phức tạp nào?', '82222222-2222-2222-2222-222222222222', 'O(log n)', 'O(1)', 'O(n)', 'O(n log n)'),
    ('f1110001-0000-4000-8000-000000000006', 'f1100001-0000-4000-8000-000000000001', 'Nếu một đoạn mã lần lượt chạy O(n) rồi O(n²), độ phức tạp tổng là gì?', '82222222-2222-2222-2222-222222222222', 'O(n²)', 'O(n)', 'O(n³)', 'O(log n)'),
    ('f1110001-0000-4000-8000-000000000007', 'f1100001-0000-4000-8000-000000000001', 'Mảng lưu các phần tử trong bộ nhớ theo đặc điểm nào?', '84444444-4444-4444-4444-444444444444', 'Các vị trí liên tiếp và truy cập bằng chỉ số', 'Các nút chỉ có liên kết tới phần tử trước', 'Chỉ cho phép thêm ở đầu', 'Luôn được sắp xếp'),
    ('f1110001-0000-4000-8000-000000000008', 'f1100001-0000-4000-8000-000000000001', 'Nguyên tắc hoạt động của stack là gì?', '84444444-4444-4444-4444-444444444444', 'LIFO, phần tử vào sau ra trước', 'FIFO, phần tử vào trước ra trước', 'Phần tử nhỏ nhất ra trước', 'Truy cập ngẫu nhiên duy nhất'),
    ('f1110001-0000-4000-8000-000000000009', 'f1100001-0000-4000-8000-000000000001', 'Đồ thị thường được biểu diễn bằng cấu trúc nào để lưu các cạnh kề?', '84444444-4444-4444-4444-444444444444', 'Danh sách kề', 'Ngăn xếp gọi hàm', 'Mảng một chiều duy nhất', 'Hàng đợi ưu tiên bắt buộc'),
    ('f1110001-0000-4000-8000-000000000010', 'f1100001-0000-4000-8000-000000000001', 'Thao tác nào thêm một phần tử vào đỉnh stack?', '85555555-5555-5555-5555-555555555555', 'push', 'pop', 'dequeue', 'peek'),
    ('f1110001-0000-4000-8000-000000000011', 'f1100001-0000-4000-8000-000000000001', 'Trong danh sách liên kết đơn, muốn chèn nút mới sau nút p cần cập nhật liên kết nào?', '85555555-5555-5555-5555-555555555555', 'new.next = p.next rồi p.next = new', 'p.next = null', 'head = null', 'Xóa toàn bộ danh sách'),
    ('f1110001-0000-4000-8000-000000000012', 'f1100001-0000-4000-8000-000000000001', 'Thao tác lấy phần tử đầu của queue thường được gọi là gì?', '85555555-5555-5555-5555-555555555555', 'dequeue', 'push', 'append', 'rotate'),
    ('f1110001-0000-4000-8000-000000000013', 'f1100001-0000-4000-8000-000000000001', 'Điều kiện cần để dùng tìm kiếm nhị phân trên mảng là gì?', '86666666-6666-6666-6666-666666666666', 'Mảng phải được sắp xếp theo thứ tự xác định', 'Mảng phải có số phần tử chẵn', 'Mảng chỉ chứa số nguyên', 'Mảng phải là mảng hai chiều'),
    ('f1110001-0000-4000-8000-000000000014', 'f1100001-0000-4000-8000-000000000001', 'Khi BFS duyệt đồ thị, cấu trúc dữ liệu nào được dùng để lưu các đỉnh chờ xử lý?', '86666666-6666-6666-6666-666666666666', 'Queue', 'Stack', 'Binary search tree', 'Hash function'),
    ('f1110001-0000-4000-8000-000000000015', 'f1100001-0000-4000-8000-000000000001', 'Sau khi so sánh khóa cần tìm nhỏ hơn phần tử giữa trong tìm kiếm nhị phân tăng dần, bước tiếp theo là gì?', '86666666-6666-6666-6666-666666666666', 'Chỉ tiếp tục tìm ở nửa bên trái', 'Chỉ tiếp tục tìm ở nửa bên phải', 'Dừng ngay vì không có khóa', 'Đổi mảng thành stack'),
    ('f1110001-0000-4000-8000-000000000016', 'f1100001-0000-4000-8000-000000000001', 'Trong bubble sort, một lượt quét đầy đủ chủ yếu làm gì?', '87777777-7777-7777-7777-777777777777', 'Đưa phần tử lớn nhất chưa sắp xếp về cuối đoạn', 'Đưa phần tử nhỏ nhất về đầu bằng chia đôi', 'Chọn một pivot', 'Xây dựng heap'),
    ('f1110001-0000-4000-8000-000000000017', 'f1100001-0000-4000-8000-000000000001', 'Insertion sort xử lý phần tử kế tiếp bằng cách nào?', '87777777-7777-7777-7777-777777777777', 'Chèn nó vào đúng vị trí trong đoạn đã sắp xếp', 'Đổi chỗ ngẫu nhiên với phần tử đầu', 'Chia mảng thành hai nửa bằng nhau', 'Luôn chuyển nó xuống cuối mảng'),
    ('f1110001-0000-4000-8000-000000000018', 'f1100001-0000-4000-8000-000000000001', 'Merge sort cần thực hiện bước nào sau khi sắp xếp hai nửa?', '87777777-7777-7777-7777-777777777777', 'Trộn hai nửa đã sắp xếp', 'Bỏ nửa bên phải', 'Chọn phần tử giữa làm pivot', 'Đảo ngược toàn bộ mảng'),
    ('f1110001-0000-4000-8000-000000000019', 'f1100001-0000-4000-8000-000000000001', 'Để kiểm tra ngoặc cân bằng trong biểu thức, cấu trúc dữ liệu phù hợp nhất là gì?', '8854781e-efc8-4130-9451-3b0391a32419', 'Stack', 'Queue', 'Mảng chỉ đọc', 'Danh sách kề'),
    ('f1110001-0000-4000-8000-000000000020', 'f1100001-0000-4000-8000-000000000001', 'Để tìm đường đi ngắn nhất theo số cạnh trong đồ thị không trọng số, nên hiện thực giải thuật nào?', '8854781e-efc8-4130-9451-3b0391a32419', 'BFS với queue', 'DFS đệ quy duy nhất', 'Selection sort', 'Tìm kiếm tuyến tính'),
    -- Practice Quiz B
    ('f1120001-0000-4000-8000-000000000001', 'f1100002-0000-4000-8000-000000000002', 'Mục đích chính của phân tích độ phức tạp tiệm cận là gì?', '81111111-1111-1111-1111-111111111111', 'So sánh tốc độ tăng tài nguyên khi kích thước đầu vào lớn', 'Đếm số lỗi cú pháp', 'Đo tốc độ mạng', 'Chọn tên biến'),
    ('f1120001-0000-4000-8000-000000000002', 'f1100002-0000-4000-8000-000000000002', 'Hàm nào tăng chậm hơn khi n rất lớn?', '81111111-1111-1111-1111-111111111111', 'O(log n)', 'O(n)', 'O(n²)', 'O(2ⁿ)'),
    ('f1120001-0000-4000-8000-000000000003', 'f1100002-0000-4000-8000-000000000002', 'Nếu một thao tác luôn mất cùng số bước bất kể n, nó có Big O là gì?', '81111111-1111-1111-1111-111111111111', 'O(1)', 'O(n)', 'O(log n)', 'O(n²)'),
    ('f1120001-0000-4000-8000-000000000004', 'f1100002-0000-4000-8000-000000000002', 'Ba vòng lặp lồng nhau, mỗi vòng chạy n lần, có độ phức tạp nào?', '82222222-2222-2222-2222-222222222222', 'O(n³)', 'O(n²)', 'O(n)', 'O(log n)'),
    ('f1120001-0000-4000-8000-000000000005', 'f1100002-0000-4000-8000-000000000002', 'Vòng lặp chạy từ 0 đến n với bước tăng 1 có số lần lặp theo bậc nào?', '82222222-2222-2222-2222-222222222222', 'O(n)', 'O(1)', 'O(log n)', 'O(n²)'),
    ('f1120001-0000-4000-8000-000000000006', 'f1100002-0000-4000-8000-000000000002', 'Một vòng lặp ngoài chạy n lần, vòng trong chạy 10 lần cố định. Độ phức tạp là gì?', '82222222-2222-2222-2222-222222222222', 'O(n)', 'O(10n²)', 'O(log n)', 'O(1)'),
    ('f1120001-0000-4000-8000-000000000007', 'f1100002-0000-4000-8000-000000000002', 'Queue tuân theo quy tắc nào?', '84444444-4444-4444-4444-444444444444', 'FIFO, vào trước ra trước', 'LIFO, vào sau ra trước', 'Ưu tiên khóa lớn nhất', 'Chỉ đọc tuần tự'),
    ('f1120001-0000-4000-8000-000000000008', 'f1100002-0000-4000-8000-000000000002', 'Một cây nhị phân là cây mà mỗi nút có tối đa bao nhiêu nút con?', '84444444-4444-4444-4444-444444444444', 'Hai', 'Một', 'Ba', 'Không giới hạn'),
    ('f1120001-0000-4000-8000-000000000009', 'f1100002-0000-4000-8000-000000000002', 'Trong danh sách liên kết, mỗi nút thường chứa gì?', '84444444-4444-4444-4444-444444444444', 'Dữ liệu và liên kết tới nút kế tiếp hoặc lân cận', 'Chỉ một chỉ số mảng', 'Chỉ khóa đã sắp xếp', 'Một ma trận kề đầy đủ'),
    ('f1120001-0000-4000-8000-000000000010', 'f1100002-0000-4000-8000-000000000002', 'Để xem phần tử trên đỉnh stack mà không xóa nó, dùng thao tác nào?', '85555555-5555-5555-5555-555555555555', 'peek', 'pop', 'enqueue', 'insertAfter'),
    ('f1120001-0000-4000-8000-000000000011', 'f1100002-0000-4000-8000-000000000002', 'Khi thêm phần tử vào cuối queue, thao tác được gọi là gì?', '85555555-5555-5555-5555-555555555555', 'enqueue', 'dequeue', 'pop', 'traverse'),
    ('f1120001-0000-4000-8000-000000000012', 'f1100002-0000-4000-8000-000000000002', 'Trong cây tìm kiếm nhị phân, khóa nhỏ hơn nút gốc của một cây con được đặt ở đâu?', '85555555-5555-5555-5555-555555555555', 'Cây con trái', 'Cây con phải', 'Luôn ở lá sâu nhất', 'Trong queue ngoài cây'),
    ('f1120001-0000-4000-8000-000000000013', 'f1100002-0000-4000-8000-000000000002', 'Tìm kiếm tuyến tính kiểm tra các phần tử theo cách nào?', '86666666-6666-6666-6666-666666666666', 'Lần lượt cho tới khi thấy khóa hoặc hết dữ liệu', 'Luôn bắt đầu ở giữa và chia đôi', 'Chỉ kiểm tra phần tử cuối', 'Sắp xếp bằng heap trước'),
    ('f1120001-0000-4000-8000-000000000014', 'f1100002-0000-4000-8000-000000000002', 'Trong DFS lặp, cấu trúc nào thường dùng thay cho lời gọi đệ quy?', '86666666-6666-6666-6666-666666666666', 'Stack', 'Queue', 'Hash table', 'Mảng kết quả sắp xếp'),
    ('f1120001-0000-4000-8000-000000000015', 'f1100002-0000-4000-8000-000000000002', 'Trong BFS, khi phát hiện một đỉnh kề chưa thăm, bước đúng là gì?', '86666666-6666-6666-6666-666666666666', 'Đánh dấu đã thăm và đưa nó vào queue', 'Xóa nó khỏi đồ thị', 'Đưa nó vào stack gọi hàm', 'Sắp xếp toàn bộ đồ thị'),
    ('f1120001-0000-4000-8000-000000000016', 'f1100002-0000-4000-8000-000000000002', 'Selection sort chọn phần tử nào trong mỗi lượt để đặt vào đầu phần chưa sắp xếp?', '87777777-7777-7777-7777-777777777777', 'Phần tử nhỏ nhất', 'Phần tử lớn nhất luôn luôn', 'Phần tử giữa', 'Một phần tử ngẫu nhiên'),
    ('f1120001-0000-4000-8000-000000000017', 'f1100002-0000-4000-8000-000000000002', 'Sau lượt đầu của selection sort tăng dần, vị trí đầu mảng chứa gì?', '87777777-7777-7777-7777-777777777777', 'Phần tử nhỏ nhất của toàn mảng', 'Phần tử lớn nhất của toàn mảng', 'Pivot của quicksort', 'Phần tử cuối ban đầu'),
    ('f1120001-0000-4000-8000-000000000018', 'f1100002-0000-4000-8000-000000000002', 'Quick sort phân hoạch mảng quanh thành phần nào?', '87777777-7777-7777-7777-777777777777', 'Pivot', 'Queue', 'Root của heap', 'Con trỏ null'),
    ('f1120001-0000-4000-8000-000000000019', 'f1100002-0000-4000-8000-000000000002', 'Để đảo ngược một chuỗi ký tự, cách dùng cấu trúc dữ liệu nào là phù hợp?', '8854781e-efc8-4130-9451-3b0391a32419', 'Đẩy từng ký tự vào stack rồi pop ra', 'Đưa ký tự vào queue rồi dequeue', 'Chạy BFS trên chuỗi', 'Dùng binary search'),
    ('f1120001-0000-4000-8000-000000000020', 'f1100002-0000-4000-8000-000000000002', 'Để phát hiện đường đi trong đồ thị không trọng số từ s đến t, giải thuật nào cho số cạnh ít nhất?', '8854781e-efc8-4130-9451-3b0391a32419', 'BFS', 'DFS', 'Insertion sort', 'Linear search'),
    -- Practice Quiz C
    ('f1130001-0000-4000-8000-000000000001', 'f1100003-0000-4000-8000-000000000003', 'Big O tập trung vào hành vi của giải thuật trong trường hợp nào?', '81111111-1111-1111-1111-111111111111', 'Khi kích thước đầu vào tăng lớn', 'Chỉ khi đầu vào rỗng', 'Chỉ khi chạy trên máy nhanh', 'Chỉ khi không có vòng lặp'),
    ('f1130001-0000-4000-8000-000000000002', 'f1100003-0000-4000-8000-000000000003', 'Độ phức tạp O(n log n) tăng nhanh hơn hay chậm hơn O(n²) khi n lớn?', '81111111-1111-1111-1111-111111111111', 'Chậm hơn O(n²)', 'Nhanh hơn O(n²)', 'Bằng O(n²)', 'Không phụ thuộc vào n'),
    ('f1130001-0000-4000-8000-000000000003', 'f1100003-0000-4000-8000-000000000003', 'Big O nào thường được xem là hằng số?', '81111111-1111-1111-1111-111111111111', 'O(1)', 'O(n)', 'O(n log n)', 'O(n²)'),
    ('f1130001-0000-4000-8000-000000000004', 'f1100003-0000-4000-8000-000000000003', 'Nếu lệnh trong vòng lặp chạy n lần và mỗi lần mất O(1), tổng thời gian là gì?', '82222222-2222-2222-2222-222222222222', 'O(n)', 'O(1)', 'O(log n)', 'O(n²)'),
    ('f1130001-0000-4000-8000-000000000005', 'f1100003-0000-4000-8000-000000000003', 'Một vòng lặp giảm n xuống một nửa mỗi lần lặp có độ phức tạp gì?', '82222222-2222-2222-2222-222222222222', 'O(log n)', 'O(n)', 'O(n²)', 'O(2ⁿ)'),
    ('f1130001-0000-4000-8000-000000000006', 'f1100003-0000-4000-8000-000000000003', 'Hai vòng lặp nối tiếp, mỗi vòng chạy n lần, có độ phức tạp nào?', '82222222-2222-2222-2222-222222222222', 'O(n)', 'O(n²)', 'O(log n)', 'O(1)'),
    ('f1130001-0000-4000-8000-000000000007', 'f1100003-0000-4000-8000-000000000003', 'Ưu điểm nổi bật của mảng đối với phần tử ở vị trí i là gì?', '84444444-4444-4444-4444-444444444444', 'Truy cập trực tiếp bằng chỉ số', 'Chèn đầu luôn O(1) không cần dịch', 'Không cần biết kích thước', 'Không có thứ tự vị trí'),
    ('f1130001-0000-4000-8000-000000000008', 'f1100003-0000-4000-8000-000000000003', 'Heap nhị phân thường được dùng để hiện thực cấu trúc nào?', '84444444-4444-4444-4444-444444444444', 'Priority queue', 'Linked list đơn', 'Ma trận kề', 'Stack gọi hàm'),
    ('f1130001-0000-4000-8000-000000000009', 'f1100003-0000-4000-8000-000000000003', 'Trong cây, nút không có nút con được gọi là gì?', '84444444-4444-4444-4444-444444444444', 'Nút lá', 'Nút gốc', 'Nút cha bắt buộc', 'Cạnh kề'),
    ('f1130001-0000-4000-8000-000000000010', 'f1100003-0000-4000-8000-000000000003', 'Để xóa đỉnh stack, thao tác nào được thực hiện?', '85555555-5555-5555-5555-555555555555', 'pop', 'push', 'enqueue', 'front'),
    ('f1130001-0000-4000-8000-000000000011', 'f1100003-0000-4000-8000-000000000003', 'Trong danh sách liên kết đơn, cần giữ tham chiếu nào để duyệt từ đầu đến cuối?', '85555555-5555-5555-5555-555555555555', 'Con trỏ head rồi theo các liên kết next', 'Chỉ số cuối của mảng', 'Một pivot', 'Một queue rỗng'),
    ('f1130001-0000-4000-8000-000000000012', 'f1100003-0000-4000-8000-000000000003', 'Để thêm cạnh vô hướng u-v vào danh sách kề, cần làm gì?', '85555555-5555-5555-5555-555555555555', 'Thêm v vào danh sách u và u vào danh sách v', 'Chỉ thêm v vào danh sách u', 'Xóa u và v', 'Đưa cả hai vào stack'),
    ('f1130001-0000-4000-8000-000000000013', 'f1100003-0000-4000-8000-000000000003', 'Tìm kiếm nhị phân bắt đầu kiểm tra phần tử nào của đoạn tìm kiếm?', '86666666-6666-6666-6666-666666666666', 'Phần tử ở giữa', 'Phần tử đầu duy nhất', 'Phần tử cuối duy nhất', 'Một phần tử ngẫu nhiên'),
    ('f1130001-0000-4000-8000-000000000014', 'f1100003-0000-4000-8000-000000000003', 'DFS thường đi theo cách nào trước khi quay lui?', '86666666-6666-6666-6666-666666666666', 'Đi sâu hết mức có thể theo một nhánh', 'Thăm toàn bộ đỉnh cùng mức', 'Luôn chọn cạnh ngắn nhất', 'Sắp xếp các đỉnh'),
    ('f1130001-0000-4000-8000-000000000015', 'f1100003-0000-4000-8000-000000000003', 'Sau khi tìm thấy khóa ở vị trí giữa trong binary search, giải thuật làm gì?', '86666666-6666-6666-6666-666666666666', 'Trả về vị trí đó', 'Tiếp tục tìm cả hai nửa', 'Sắp xếp lại mảng', 'Đưa khóa vào queue'),
    ('f1130001-0000-4000-8000-000000000016', 'f1100003-0000-4000-8000-000000000003', 'Bubble sort tăng dần hoán đổi hai phần tử kề nhau khi nào?', '87777777-7777-7777-7777-777777777777', 'Khi phần tử bên trái lớn hơn phần tử bên phải', 'Khi chúng bằng nhau bắt buộc', 'Khi phần tử trái nhỏ hơn', 'Chỉ khi mảng đã sắp xếp'),
    ('f1130001-0000-4000-8000-000000000017', 'f1100003-0000-4000-8000-000000000003', 'Trong insertion sort, đoạn bên trái của chỉ số hiện tại có tính chất gì?', '87777777-7777-7777-7777-777777777777', 'Đã được sắp xếp', 'Luôn rỗng', 'Luôn giảm dần bất kể yêu cầu', 'Là một heap hoàn chỉnh'),
    ('f1130001-0000-4000-8000-000000000018', 'f1100003-0000-4000-8000-000000000003', 'Bước đầu tiên của merge sort là gì?', '87777777-7777-7777-7777-777777777777', 'Chia dãy thành các nửa nhỏ hơn', 'Chọn phần tử nhỏ nhất toàn cục', 'Đưa mọi phần tử vào queue', 'Tìm khóa bằng binary search'),
    ('f1130001-0000-4000-8000-000000000019', 'f1100003-0000-4000-8000-000000000003', 'Để đánh giá biểu thức hậu tố, giải thuật nên dùng cấu trúc dữ liệu nào?', '8854781e-efc8-4130-9451-3b0391a32419', 'Stack để lưu các toán hạng trung gian', 'Queue để đảo biểu thức', 'Binary search tree bắt buộc', 'Mảng không thể thay đổi'),
    ('f1130001-0000-4000-8000-000000000020', 'f1100003-0000-4000-8000-000000000003', 'Để tìm các thành phần liên thông của đồ thị, có thể lặp lại giải thuật nào từ mỗi đỉnh chưa thăm?', '8854781e-efc8-4130-9451-3b0391a32419', 'DFS hoặc BFS', 'Merge sort', 'Binary search', 'Selection sort');

INSERT INTO quizzes
    (id, quiz_title, description, unlimited, time_limit_minutes, pass_score, start_time, due_time,
     attempt_limited, course_id, teacher_id, display, lifecycle_status, source_type,
     version, published_by, published_at)
VALUES
    ('f1100001-0000-4000-8000-000000000001', 'DSA Practice Quiz A',
     'Always-available practice quiz covering every Data Structures and Algorithms learning outcome.',
     true, NULL, 50.0, NULL, NULL, NULL, 'c1111111-1111-1111-1111-111111111111',
     '11111111-1111-1111-1111-111111111111', true, 'PUBLISHED', 'MANUAL', 1,
     '11111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP),
    ('f1100002-0000-4000-8000-000000000002', 'DSA Practice Quiz B',
     'Always-available practice quiz covering every Data Structures and Algorithms learning outcome.',
     true, NULL, 50.0, NULL, NULL, NULL, 'c1111111-1111-1111-1111-111111111111',
     '22222222-2222-2222-2222-222222222222', true, 'PUBLISHED', 'MANUAL', 1,
     '22222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP),
    ('f1100003-0000-4000-8000-000000000003', 'DSA Practice Quiz C',
     'Always-available practice quiz covering every Data Structures and Algorithms learning outcome.',
     true, NULL, 50.0, NULL, NULL, NULL, 'c1111111-1111-1111-1111-111111111111',
     '33333333-3333-3333-3333-333333333333', true, 'PUBLISHED', 'MANUAL', 1,
     '33333333-3333-3333-3333-333333333333', CURRENT_TIMESTAMP);

INSERT INTO questions
    (id, content, content_format, explanation, explanation_format, type, quiz_id,
     review_status, source_type)
SELECT question_id, content, 'PLAIN_TEXT',
       'Đáp án đúng: ' || correct_answer, 'PLAIN_TEXT', 'MULTIPLE_CHOICE', quiz_id,
       'APPROVED', 'MANUAL'
FROM dsa_practice_quiz_seed_questions;

INSERT INTO answers (content, content_format, is_correct, explanation, explanation_format, question_id)
SELECT answer.content, 'PLAIN_TEXT', answer.is_correct,
       CASE WHEN answer.is_correct THEN 'Đáp án đúng.' ELSE 'Đáp án chưa đúng.' END,
       'PLAIN_TEXT', seed.question_id
FROM dsa_practice_quiz_seed_questions seed
CROSS JOIN LATERAL (
    VALUES
        (seed.correct_answer, true),
        (seed.wrong_answer_1, false),
        (seed.wrong_answer_2, false),
        (seed.wrong_answer_3, false)
) AS answer(content, is_correct);

INSERT INTO quiz_learning_outcomes (quiz_id, learning_outcome_id)
SELECT DISTINCT quiz_id, learning_outcome_id
FROM dsa_practice_quiz_seed_questions;

INSERT INTO question_learning_outcomes (question_id, learning_outcome_id)
SELECT question_id, learning_outcome_id
FROM dsa_practice_quiz_seed_questions;

INSERT INTO quiz_chapters (quiz_id, chapter_id)
SELECT quiz.id, chapter.chapter_id
FROM (VALUES
          ('f1100001-0000-4000-8000-000000000001'::uuid),
          ('f1100002-0000-4000-8000-000000000002'::uuid),
          ('f1100003-0000-4000-8000-000000000003'::uuid)
     ) AS quiz(id)
CROSS JOIN chapters chapter
WHERE chapter.course_id = 'c1111111-1111-1111-1111-111111111111';

INSERT INTO quiz_releases (id, quiz_id, release_number, quiz_version, active, published_by, published_at)
VALUES
    ('f1200001-0000-4000-8000-000000000001', 'f1100001-0000-4000-8000-000000000001', 1, 1, true, '11111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP),
    ('f1200002-0000-4000-8000-000000000002', 'f1100002-0000-4000-8000-000000000002', 1, 1, true, '22222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP),
    ('f1200003-0000-4000-8000-000000000003', 'f1100003-0000-4000-8000-000000000003', 1, 1, true, '33333333-3333-3333-3333-333333333333', CURRENT_TIMESTAMP);

-- 17. Persistent CCCT seed profiles for the DSA course.
-- schema.sql recreates the development database at every BE startup, then this
-- script restores these selectable DRAFT profiles with stable UUIDs.
INSERT INTO ccct_profiles
    (id, course_id, name, template_title, status, version, format_version,
     structured_current, last_mode, markdown_content, created_by)
VALUES
    ('e1000001-0000-4000-8000-000000000001', 'c1111111-1111-1111-1111-111111111111',
     'CCCT – Độ phức tạp giải thuật', 'CCCT – Độ phức tạp giải thuật', 'DRAFT', 0, 2,
     true, 'STRUCTURED',
     '# CCCT – Độ phức tạp giải thuật

## Độ phức tạp giải thuật

### Phân tích độ phức tạp tiệm cận
- Big-O (Cận trên / Upper bound) — Learn and exam
- Big-Ω (Cận dưới / Lower bound) — Learn and exam',
     '11111111-1111-1111-1111-111111111111'),
    ('e1000001-0000-4000-8000-000000000002', 'c1111111-1111-1111-1111-111111111111',
     'CCCT – Heap', 'CCCT – Heap', 'DRAFT', 0, 2,
     true, 'STRUCTURED',
     '# CCCT – Heap

## Heap

### Các loại Heap
- Max-Heap — Learn and exam
- Min-Heap — Learn and exam',
     '11111111-1111-1111-1111-111111111111'),
    ('e1000001-0000-4000-8000-000000000003', 'c1111111-1111-1111-1111-111111111111',
     'CCCT – Đồ thị', 'CCCT – Đồ thị', 'DRAFT', 0, 2,
     true, 'STRUCTURED',
     '# CCCT – Đồ thị

## Đồ thị

### Duyệt đồ thị
- BFS (Duyệt theo chiều rộng) — Learn and exam
- DFS (Duyệt theo chiều sâu) — Learn and exam',
     '11111111-1111-1111-1111-111111111111');

-- Format v2 requires every Course Chapter in its persisted order. Only the
-- requested chapter contains topics; the remaining chapters are intentionally empty.
INSERT INTO ccct_chapters (id, profile_id, course_chapter_id, title, custom_content, order_index)
VALUES
    ('e2000001-0000-4000-8000-000000000001', 'e1000001-0000-4000-8000-000000000001', '069331da-fc65-45f3-bb7f-e23f8f9a3a50', 'Cây nhị phân', '', 0),
    ('e2000001-0000-4000-8000-000000000002', 'e1000001-0000-4000-8000-000000000001', '5e055691-371a-4de5-b61a-f6d06712c36a', 'Độ phức tạp giải thuật', '', 1),
    ('e2000001-0000-4000-8000-000000000003', 'e1000001-0000-4000-8000-000000000001', '6967be86-ae45-4e55-8faa-71318f50bbf6', 'Đồ thị', '', 2),
    ('e2000001-0000-4000-8000-000000000004', 'e1000001-0000-4000-8000-000000000001', '69c3255e-fbd2-4eb7-9a78-a5147b0da42d', 'Heap', '', 3),
    ('e2000001-0000-4000-8000-000000000005', 'e1000001-0000-4000-8000-000000000001', '8a01575f-a418-4107-b1b1-5eef75c5b72d', 'Giới thiệu', '', 4),
    ('e2000001-0000-4000-8000-000000000006', 'e1000001-0000-4000-8000-000000000001', 'a8c06712-6aa5-4993-901b-77008af766d3', 'Sắp xếp', '', 5),
    ('e2000001-0000-4000-8000-000000000007', 'e1000001-0000-4000-8000-000000000001', 'c5d67446-27f5-4fcf-97cd-a6de6109114e', 'Chồng và Hàng', '', 6),
    ('e2000001-0000-4000-8000-000000000008', 'e1000001-0000-4000-8000-000000000001', 'd1f07793-cfd2-450f-887a-abfc5b189b4b', 'Tìm kiếm', '', 7),
    ('e2000001-0000-4000-8000-000000000009', 'e1000001-0000-4000-8000-000000000001', 'd8d35922-4538-4613-9fe8-5dcef719b520', 'Cấu trúc cây nâng cao', '', 8),
    ('e2000001-0000-4000-8000-000000000010', 'e1000001-0000-4000-8000-000000000001', 'fe369f65-27f3-4314-9885-2099b0ca2fd8', 'Danh sách', '', 9),
    ('e2000001-0000-4000-8000-000000000011', 'e1000001-0000-4000-8000-000000000002', '069331da-fc65-45f3-bb7f-e23f8f9a3a50', 'Cây nhị phân', '', 0),
    ('e2000001-0000-4000-8000-000000000012', 'e1000001-0000-4000-8000-000000000002', '5e055691-371a-4de5-b61a-f6d06712c36a', 'Độ phức tạp giải thuật', '', 1),
    ('e2000001-0000-4000-8000-000000000013', 'e1000001-0000-4000-8000-000000000002', '6967be86-ae45-4e55-8faa-71318f50bbf6', 'Đồ thị', '', 2),
    ('e2000001-0000-4000-8000-000000000014', 'e1000001-0000-4000-8000-000000000002', '69c3255e-fbd2-4eb7-9a78-a5147b0da42d', 'Heap', '', 3),
    ('e2000001-0000-4000-8000-000000000015', 'e1000001-0000-4000-8000-000000000002', '8a01575f-a418-4107-b1b1-5eef75c5b72d', 'Giới thiệu', '', 4),
    ('e2000001-0000-4000-8000-000000000016', 'e1000001-0000-4000-8000-000000000002', 'a8c06712-6aa5-4993-901b-77008af766d3', 'Sắp xếp', '', 5),
    ('e2000001-0000-4000-8000-000000000017', 'e1000001-0000-4000-8000-000000000002', 'c5d67446-27f5-4fcf-97cd-a6de6109114e', 'Chồng và Hàng', '', 6),
    ('e2000001-0000-4000-8000-000000000018', 'e1000001-0000-4000-8000-000000000002', 'd1f07793-cfd2-450f-887a-abfc5b189b4b', 'Tìm kiếm', '', 7),
    ('e2000001-0000-4000-8000-000000000019', 'e1000001-0000-4000-8000-000000000002', 'd8d35922-4538-4613-9fe8-5dcef719b520', 'Cấu trúc cây nâng cao', '', 8),
    ('e2000001-0000-4000-8000-000000000020', 'e1000001-0000-4000-8000-000000000002', 'fe369f65-27f3-4314-9885-2099b0ca2fd8', 'Danh sách', '', 9),
    ('e2000001-0000-4000-8000-000000000021', 'e1000001-0000-4000-8000-000000000003', '069331da-fc65-45f3-bb7f-e23f8f9a3a50', 'Cây nhị phân', '', 0),
    ('e2000001-0000-4000-8000-000000000022', 'e1000001-0000-4000-8000-000000000003', '5e055691-371a-4de5-b61a-f6d06712c36a', 'Độ phức tạp giải thuật', '', 1),
    ('e2000001-0000-4000-8000-000000000023', 'e1000001-0000-4000-8000-000000000003', '6967be86-ae45-4e55-8faa-71318f50bbf6', 'Đồ thị', '', 2),
    ('e2000001-0000-4000-8000-000000000024', 'e1000001-0000-4000-8000-000000000003', '69c3255e-fbd2-4eb7-9a78-a5147b0da42d', 'Heap', '', 3),
    ('e2000001-0000-4000-8000-000000000025', 'e1000001-0000-4000-8000-000000000003', '8a01575f-a418-4107-b1b1-5eef75c5b72d', 'Giới thiệu', '', 4),
    ('e2000001-0000-4000-8000-000000000026', 'e1000001-0000-4000-8000-000000000003', 'a8c06712-6aa5-4993-901b-77008af766d3', 'Sắp xếp', '', 5),
    ('e2000001-0000-4000-8000-000000000027', 'e1000001-0000-4000-8000-000000000003', 'c5d67446-27f5-4fcf-97cd-a6de6109114e', 'Chồng và Hàng', '', 6),
    ('e2000001-0000-4000-8000-000000000028', 'e1000001-0000-4000-8000-000000000003', 'd1f07793-cfd2-450f-887a-abfc5b189b4b', 'Tìm kiếm', '', 7),
    ('e2000001-0000-4000-8000-000000000029', 'e1000001-0000-4000-8000-000000000003', 'd8d35922-4538-4613-9fe8-5dcef719b520', 'Cấu trúc cây nâng cao', '', 8),
    ('e2000001-0000-4000-8000-000000000030', 'e1000001-0000-4000-8000-000000000003', 'fe369f65-27f3-4314-9885-2099b0ca2fd8', 'Danh sách', '', 9);

INSERT INTO ccct_topics (id, chapter_id, title, order_index)
VALUES
    ('e3000001-0000-4000-8000-000000000001', 'e2000001-0000-4000-8000-000000000002', 'Phân tích độ phức tạp tiệm cận', 0),
    ('e3000001-0000-4000-8000-000000000002', 'e2000001-0000-4000-8000-000000000014', 'Các loại Heap', 0),
    ('e3000001-0000-4000-8000-000000000003', 'e2000001-0000-4000-8000-000000000023', 'Duyệt đồ thị', 0);

INSERT INTO ccct_items (id, topic_id, content, coverage_status, note, order_index)
VALUES
    ('e4000001-0000-4000-8000-000000000001', 'e3000001-0000-4000-8000-000000000001', 'Big-O (Cận trên / Upper bound)', 'LEARN_AND_EXAM', '', 0),
    ('e4000001-0000-4000-8000-000000000002', 'e3000001-0000-4000-8000-000000000001', 'Big-Ω (Cận dưới / Lower bound)', 'LEARN_AND_EXAM', '', 1),
    ('e4000001-0000-4000-8000-000000000003', 'e3000001-0000-4000-8000-000000000002', 'Max-Heap', 'LEARN_AND_EXAM', '', 0),
    ('e4000001-0000-4000-8000-000000000004', 'e3000001-0000-4000-8000-000000000002', 'Min-Heap', 'LEARN_AND_EXAM', '', 1),
    ('e4000001-0000-4000-8000-000000000005', 'e3000001-0000-4000-8000-000000000003', 'BFS (Duyệt theo chiều rộng)', 'LEARN_AND_EXAM', '', 0),
    ('e4000001-0000-4000-8000-000000000006', 'e3000001-0000-4000-8000-000000000003', 'DFS (Duyệt theo chiều sâu)', 'LEARN_AND_EXAM', '', 1);

-- <<<<<<< HEAD
-- -- Material rows are intentionally not seeded. A local reset uses LocalStack,
-- -- while the former fixture keys only exist in the production Supabase bucket.
-- -- Local PDF metadata and indexing jobs are created through the normal upload
-- -- and confirm flow after the corresponding object exists in local storage.
-- =======
-- -- Persistent CCCT paired with the dedicated Code Runner matrix above.
-- INSERT INTO ccct_profiles
--     (id, course_id, name, template_title, status, version, format_version,
--      structured_current, last_mode, markdown_content, created_by)
-- VALUES
--     ('e1000001-0000-4000-8000-000000000004', 'c1111111-1111-1111-1111-111111111111',
--      'CCCT - DSA Code Runner', 'CCCT - DSA Code Runner', 'DRAFT', 0, 2,
--      true, 'STRUCTURED',
--      '# CCCT - DSA Code Runner
--
-- ## Heap and sorting
--
-- ### C++17 function implementation
-- - Build a min-heap from an array - Learn and exam
-- - Implement insertion sort - Learn and exam',
--      '11111111-1111-1111-1111-111111111111');
--
-- INSERT INTO ccct_chapters (id, profile_id, course_chapter_id, title, custom_content, order_index)
-- VALUES
--     ('e2000001-0000-4000-8000-000000000031', 'e1000001-0000-4000-8000-000000000004', '069331da-fc65-45f3-bb7f-e23f8f9a3a50', 'Binary trees', '', 0),
--     ('e2000001-0000-4000-8000-000000000032', 'e1000001-0000-4000-8000-000000000004', '5e055691-371a-4de5-b61a-f6d06712c36a', 'Algorithm complexity', '', 1),
--     ('e2000001-0000-4000-8000-000000000033', 'e1000001-0000-4000-8000-000000000004', '6967be86-ae45-4e55-8faa-71318f50bbf6', 'Graphs', '', 2),
--     ('e2000001-0000-4000-8000-000000000034', 'e1000001-0000-4000-8000-000000000004', '69c3255e-fbd2-4eb7-9a78-a5147b0da42d', 'Heap', '', 3),
--     ('e2000001-0000-4000-8000-000000000035', 'e1000001-0000-4000-8000-000000000004', '8a01575f-a418-4107-b1b1-5eef75c5b72d', 'Introduction', '', 4),
--     ('e2000001-0000-4000-8000-000000000036', 'e1000001-0000-4000-8000-000000000004', 'a8c06712-6aa5-4993-901b-77008af766d3', 'Sorting', '', 5),
--     ('e2000001-0000-4000-8000-000000000037', 'e1000001-0000-4000-8000-000000000004', 'c5d67446-27f5-4fcf-97cd-a6de6109114e', 'Stack and queue', '', 6),
--     ('e2000001-0000-4000-8000-000000000038', 'e1000001-0000-4000-8000-000000000004', 'd1f07793-cfd2-450f-887a-abfc5b189b4b', 'Searching', '', 7),
--     ('e2000001-0000-4000-8000-000000000039', 'e1000001-0000-4000-8000-000000000004', 'd8d35922-4538-4613-9fe8-5dcef719b520', 'Advanced trees', '', 8),
--     ('e2000001-0000-4000-8000-000000000040', 'e1000001-0000-4000-8000-000000000004', 'fe369f65-27f3-4314-9885-2099b0ca2fd8', 'Lists', '', 9);
--
-- INSERT INTO ccct_topics (id, chapter_id, title, order_index)
-- VALUES
--     ('e3000001-0000-4000-8000-000000000004', 'e2000001-0000-4000-8000-000000000034', 'Min-heap implementation', 0),
--     ('e3000001-0000-4000-8000-000000000005', 'e2000001-0000-4000-8000-000000000036', 'Insertion sort', 0);
--
-- INSERT INTO ccct_items (id, topic_id, content, coverage_status, note, order_index)
-- VALUES
--     ('e4000001-0000-4000-8000-000000000007', 'e3000001-0000-4000-8000-000000000004', 'Build a min-heap from an array using sift-down', 'LEARN_AND_EXAM', '', 0),
--     ('e4000001-0000-4000-8000-000000000008', 'e3000001-0000-4000-8000-000000000005', 'Insertion sort in ascending order', 'LEARN_AND_EXAM', '', 0);
--
-- -- Reset fixtures: the original PDFs remain in the private Supabase bucket.
-- -- Each boot recreates their LMS metadata and queues fresh AI ingestion instead
-- -- of storing an expiring signed URL in the seed data.
-- INSERT INTO materials
--     (id, title, type, file_key, is_downloadable, status, ingestion_status, ingestion_revision, course_id)
-- VALUES
--     ('f1000001-0000-4000-8000-000000000001',
--      'DCMH.CO2003_Data_Structures_and_AlgoriThms.pdf',
--      'PDF',
--      'courses/c1111111-1111-1111-1111-111111111111/DCMH.CO2003_Data_Structures_and_AlgoriThms.pdf',
--      true, 'UPLOADED', 'PENDING', 'f2000001-0000-4000-8000-000000000001',
--      'c1111111-1111-1111-1111-111111111111'),
--     ('f1000001-0000-4000-8000-000000000002',
--      'DSABook_1.pdf',
--      'PDF',
--      'courses/c1111111-1111-1111-1111-111111111111/DSABook_1.pdf',
--      true, 'UPLOADED', 'PENDING', 'f2000001-0000-4000-8000-000000000002',
--      'c1111111-1111-1111-1111-111111111111');
--
-- INSERT INTO material_index_jobs
--     (id, course_id, material_id, operation, revision, file_key, status, attempt_count, next_attempt_at)
-- VALUES
--     ('f3000001-0000-4000-8000-000000000001',
--      'c1111111-1111-1111-1111-111111111111', 'f1000001-0000-4000-8000-000000000001',
--      'INGEST', 'f2000001-0000-4000-8000-000000000001',
--      'courses/c1111111-1111-1111-1111-111111111111/DCMH.CO2003_Data_Structures_and_AlgoriThms.pdf',
--      'PENDING', 0, NOW()),
--     ('f3000001-0000-4000-8000-000000000002',
--      'c1111111-1111-1111-1111-111111111111', 'f1000001-0000-4000-8000-000000000002',
--      'INGEST', 'f2000001-0000-4000-8000-000000000002',
--      'courses/c1111111-1111-1111-1111-111111111111/DSABook_1.pdf',
--      'PENDING', 0, NOW());
-- -- 17. Mock Code Runner Question for Testing
INSERT INTO questions (id, content, type, quiz_id)
VALUES ('d0000000-0000-0000-0000-000000000000', 'Write a function to return the sum of two integers a and b.', 'CODE_RUNNER', '91111111-1111-1111-1111-111111111111');
-- >>>>>>> feature/my-code-runner-fixes

INSERT INTO code_runner_configs (id, question_id, schema_version, contract_id, contract_version, contract_status, title, statement, execution_profile_id, execution_profile_version, archetype_id, archetype_version, comparator_id, comparator_version, entry_point, signature_json, starter_code, reference_solution, revision)
VALUES (
    'f0000000-0000-0000-0000-000000000000',
    'd0000000-0000-0000-0000-000000000000',
    '2.0.0', 'sum_two_integers', '1.0.0', 'VERIFIED_EXECUTION_GOLD',
    'Sum of two integers',
    'Write a function that takes two integers a and b and returns their sum.',
    'dsa_cpp17', '1.0.0',
    'scalar_function', '1.0.0',
    'exact_typed', '1.0.0',
    'sum',
    '{"return_fixture":{"id":"integer","version":"1.0.0"},"arguments":[{"name":"a","fixture":{"id":"integer","version":"1.0.0"},"constraints":{}},{"name":"b","fixture":{"id":"integer","version":"1.0.0"},"constraints":{}}]}'::jsonb,
    'int sum(int a, int b) {\n    // Write your code here\n    return 0;\n}',
    'int sum(int a, int b) {\n    return a + b;\n}',
    1
)
ON CONFLICT (id) DO UPDATE SET
    contract_status = EXCLUDED.contract_status,
    execution_profile_id = EXCLUDED.execution_profile_id,
    execution_profile_version = EXCLUDED.execution_profile_version,
    archetype_id = EXCLUDED.archetype_id,
    archetype_version = EXCLUDED.archetype_version,
    comparator_id = EXCLUDED.comparator_id,
    comparator_version = EXCLUDED.comparator_version,
    entry_point = EXCLUDED.entry_point,
    signature_json = EXCLUDED.signature_json,
    starter_code = EXCLUDED.starter_code,
    reference_solution = EXCLUDED.reference_solution,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO code_runner_test_cases (id, code_runner_config_id, external_test_id, category, visibility, arguments_json, expected_output_json, points, ordinal)
VALUES 
('a0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000000', 'public_1', 'basic', 'PUBLIC', '{"a":1,"b":2}'::jsonb, '3'::jsonb, 0.0, 1),
('a0000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000000', 'hidden_1', 'hidden', 'HIDDEN', '{"a":5,"b":-1}'::jsonb, '4'::jsonb, 2.0, 2)
ON CONFLICT (id) DO UPDATE SET
    external_test_id = EXCLUDED.external_test_id,
    category = EXCLUDED.category,
    visibility = EXCLUDED.visibility,
    arguments_json = EXCLUDED.arguments_json,
    expected_output_json = EXCLUDED.expected_output_json,
    points = EXCLUDED.points,
    ordinal = EXCLUDED.ordinal;

-- This deterministic verification evidence belongs only to the local demo
-- fixture. Student RUN/SUBMIT requests still execute through Judge0; the seed
-- merely keeps the already-published quiz consistent with the publish gate.
INSERT INTO code_runner_verifications
    (id, revision_id, verified_by, verified_at, execution_profile,
     judge0_submission_id, verdict, stdout, stderr, time_ms, memory_kb,
     wall_time_limit_ms, memory_limit_kb)
VALUES
    ('f4000000-0000-0000-0000-000000000000',
     'f0000000-0000-0000-0000-000000000000',
     '11111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP,
     '{"id":"dsa_cpp17","version":"1.0.0"}'::jsonb,
     'seed-fixture-reference', 'ACCEPTED', '3\n4', '', 1.0, 1024, 5000, 131072)
ON CONFLICT (id) DO UPDATE SET
    verdict = EXCLUDED.verdict,
    verified_at = EXCLUDED.verified_at,
    execution_profile = EXCLUDED.execution_profile;

INSERT INTO code_runner_verification_cases
    (id, verification_id, external_test_id, visibility, verdict, time_ms, memory_kb, diagnostic)
VALUES
    ('f4100000-0000-0000-0000-000000000001', 'f4000000-0000-0000-0000-000000000000',
     'public_1', 'PUBLIC', 'ACCEPTED', 1.0, 1024, ''),
    ('f4100000-0000-0000-0000-000000000002', 'f4000000-0000-0000-0000-000000000000',
     'hidden_1', 'HIDDEN', 'ACCEPTED', 1.0, 1024, '')
ON CONFLICT (id) DO UPDATE SET
    verdict = EXCLUDED.verdict,
    time_ms = EXCLUDED.time_ms,
    memory_kb = EXCLUDED.memory_kb,
    diagnostic = EXCLUDED.diagnostic;

INSERT INTO quiz_release_code_runner_bindings (quiz_release_id, question_id, code_runner_config_id)
VALUES ('c1111111-1111-1111-1111-111111111111', 'd0000000-0000-0000-0000-000000000000', 'f0000000-0000-0000-0000-000000000000') ON CONFLICT (quiz_release_id, question_id) DO NOTHING;

-- 18. Seed Assessment & Draft for Quiz2 Code Runner Testing
INSERT INTO assessments (id, course_id, title, description, unlimited, variant_count, option_count, allow_recently_used_questions, lifecycle_status, current_step, version, exam_matrix_id, ccct_profile_id, created_at, updated_at)
VALUES ('a0000000-0000-0000-0000-000000000000', 'c1111111-1111-1111-1111-111111111111', 'Code Runner Test Assessment', 'Testing Code Runner Verify Button', false, 1, 4, true, 'REVIEW_PENDING', 6, 1, 'b4000001-0000-4000-8000-000000000001', 'e1000001-0000-4000-8000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO assessment_generation_batches (id, assessment_id, status, option_count, total_cells, completed_cells, generated_questions, request_snapshot, idempotency_key, created_at, updated_at)
VALUES ('b0000000-0000-0000-0000-000000000000', 'a0000000-0000-0000-0000-000000000000', 'COMPLETED', 4, 1, 1, 1, '{}', 'seed-batch-coderunner', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO assessment_generation_batch_items (id, batch_id, matrix_row_id, custom_learning_level_id, question_type, required_count, generated_count, status, updated_at)
VALUES ('c0000000-0000-0000-0000-000000000000', 'b0000000-0000-0000-0000-000000000000', 'b4000021-0000-4000-8000-000000000001', 'b4000011-0000-4000-8000-000000000001', 'CODE_RUNNER', 1, 1, 'COMPLETED', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO question_drafts (id, course_id, content, question_type, code_runner_contract, assessment_id, generation_batch_id, chapter_id, cognitive_level_id, created_by)
VALUES (
    'd1111111-0000-0000-0000-000000000000',
    'c1111111-1111-1111-1111-111111111111',
    'Write a function to return the sum of two integers a and b.',
    'CODE_RUNNER',
    '{"schema_version":"2.0.0","contract_id":"sum_two_integers","contract_version":"1.0.0","status":"READY_FOR_REVIEW","title":"Sum Two Integers","statement":"Write a function to return the sum of two integers a and b.","runtime":{"execution_profile":{"id":"dsa_cpp17","version":"1.0.0"},"archetype":{"id":"scalar_function","version":"1.0.0"},"comparator":{"id":"exact_typed","version":"1.0.0"},"entry_point":"sum","signature":{"return_fixture":{"id":"integer","version":"1.0.0"},"arguments":[{"name":"a","fixture":{"id":"integer","version":"1.0.0"}},{"name":"b","fixture":{"id":"integer","version":"1.0.0"}}]},"starter_code":"int sum(int a, int b) {\n    return 0;\n}","public_tests":[{"id":"t1","category":"Basic","visibility":"PUBLIC","arguments":{"a":1,"b":2},"expected_output":3,"points":0}],"banned_api_patterns":[]},"private_grading":{"reference_solution":"int sum(int a, int b) {\n    return a + b;\n}","hidden_tests":[{"id":"h1","category":"Hidden","visibility":"HIDDEN","arguments":{"a":10,"b":20},"expected_output":30,"points":10}]}}'::jsonb,
    'a0000000-0000-0000-0000-000000000000',
    'b0000000-0000-0000-0000-000000000000',
    '8a01575f-a418-4107-b1b1-5eef75c5b72d',
    (SELECT id FROM cognitive_levels WHERE code = 'APPLY' LIMIT 1),
    '11111111-1111-1111-1111-111111111111'
)
ON CONFLICT (id) DO UPDATE SET 
    code_runner_contract = EXCLUDED.code_runner_contract,
    chapter_id = EXCLUDED.chapter_id,
    cognitive_level_id = EXCLUDED.cognitive_level_id,
    created_by = EXCLUDED.created_by;

INSERT INTO question_draft_learning_outcomes (question_draft_id, learning_outcome_id)
VALUES (
    'd1111111-0000-0000-0000-000000000000',
    '81111111-1111-1111-1111-111111111111'
)
ON CONFLICT DO NOTHING;
-- Matrix 3 reset fixtures for the BigO chapter. Matrix 3 uses selected
-- relations, so every CLL × CLO cell below is intentionally ticked.
INSERT INTO exam_matrices_3
    (id, course_id, title, description, total_question_count, lifecycle_status)
VALUES
    ('d5000001-0000-4000-8000-000000000001', 'c1111111-1111-1111-1111-111111111111',
     'Matrix3 BigO - 3 questions', 'Two BigO CLO rows with all CLL relations selected.', 3, 'DRAFT'),
    ('d5000001-0000-4000-8000-000000000002', 'c1111111-1111-1111-1111-111111111111',
     'Matrix3 BigO - 6 questions', 'Two BigO CLO rows with all CLL relations selected.', 6, 'DRAFT');

INSERT INTO exam_matrix_3_custom_learning_levels (id, matrix_id, name, sort_order)
VALUES
    ('d5100001-0000-4000-8000-000000000001', 'd5000001-0000-4000-8000-000000000001', 'Know', 0),
    ('d5100001-0000-4000-8000-000000000002', 'd5000001-0000-4000-8000-000000000001', 'Understand', 1),
    ('d5100001-0000-4000-8000-000000000003', 'd5000001-0000-4000-8000-000000000001', 'Master', 2),
    ('d5100001-0000-4000-8000-000000000004', 'd5000001-0000-4000-8000-000000000002', 'Know', 0),
    ('d5100001-0000-4000-8000-000000000005', 'd5000001-0000-4000-8000-000000000002', 'Understand', 1),
    ('d5100001-0000-4000-8000-000000000006', 'd5000001-0000-4000-8000-000000000002', 'Master', 2);

INSERT INTO exam_matrix_3_rows
    (id, matrix_id, chapter_id, mapped_learning_outcome_id, sort_order)
VALUES
    ('d5200001-0000-4000-8000-000000000001', 'd5000001-0000-4000-8000-000000000001',
     '5e055691-371a-4de5-b61a-f6d06712c36a', '81111111-1111-1111-1111-111111111111', 0),
    ('d5200001-0000-4000-8000-000000000002', 'd5000001-0000-4000-8000-000000000001',
     '5e055691-371a-4de5-b61a-f6d06712c36a', '82222222-2222-2222-2222-222222222222', 1),
    ('d5200001-0000-4000-8000-000000000003', 'd5000001-0000-4000-8000-000000000002',
     '5e055691-371a-4de5-b61a-f6d06712c36a', '81111111-1111-1111-1111-111111111111', 0),
    ('d5200001-0000-4000-8000-000000000004', 'd5000001-0000-4000-8000-000000000002',
     '5e055691-371a-4de5-b61a-f6d06712c36a', '82222222-2222-2222-2222-222222222222', 1);

INSERT INTO exam_matrix_3_cell_relations (row_id, custom_learning_level_id)
VALUES
    ('d5200001-0000-4000-8000-000000000001', 'd5100001-0000-4000-8000-000000000001'),
    ('d5200001-0000-4000-8000-000000000001', 'd5100001-0000-4000-8000-000000000002'),
    ('d5200001-0000-4000-8000-000000000001', 'd5100001-0000-4000-8000-000000000003'),
    ('d5200001-0000-4000-8000-000000000002', 'd5100001-0000-4000-8000-000000000001'),
    ('d5200001-0000-4000-8000-000000000002', 'd5100001-0000-4000-8000-000000000002'),
    ('d5200001-0000-4000-8000-000000000002', 'd5100001-0000-4000-8000-000000000003'),
    ('d5200001-0000-4000-8000-000000000003', 'd5100001-0000-4000-8000-000000000004'),
    ('d5200001-0000-4000-8000-000000000003', 'd5100001-0000-4000-8000-000000000005'),
    ('d5200001-0000-4000-8000-000000000003', 'd5100001-0000-4000-8000-000000000006'),
    ('d5200001-0000-4000-8000-000000000004', 'd5100001-0000-4000-8000-000000000004'),
    ('d5200001-0000-4000-8000-000000000004', 'd5100001-0000-4000-8000-000000000005'),
    ('d5200001-0000-4000-8000-000000000004', 'd5100001-0000-4000-8000-000000000006');

-- QBank2 reset fixtures for CO2003.  They are deliberately plain-text and
-- self-contained so teachers can exercise manual Quiz3 few-shot selection.
INSERT INTO question_bank_2_groups (id, course_id, code, code_sequence, name, description)
VALUES
    ('a1000001-0000-4000-8000-000000000001', 'c1111111-1111-1111-1111-111111111111', 'QBG-001', 1, 'Stack implementation', 'Push, pop, and peek operations on a stack.'),
    ('a1000001-0000-4000-8000-000000000002', 'c1111111-1111-1111-1111-111111111111', 'QBG-002', 2, 'Queue/FIFO operations', 'Enqueue, dequeue, and FIFO behavior of a queue.'),
    ('a1000001-0000-4000-8000-000000000003', 'c1111111-1111-1111-1111-111111111111', 'QBG-003', 3, 'Binary search', 'Binary search over a sorted array.'),
    ('a1000001-0000-4000-8000-000000000004', 'c1111111-1111-1111-1111-111111111111', 'QBG-004', 4, 'Sorting trace', 'Trace the steps of a sorting algorithm.'),
    ('a1000001-0000-4000-8000-000000000005', 'c1111111-1111-1111-1111-111111111111', 'QBG-005', 5, 'BigO - Quicksort worst case', 'Kiểm tra trình độ hiểu biết về độ phức tạp giải thuật qua trường hợp xấu nhất của Quicksort.'),
    ('a1000001-0000-4000-8000-000000000006', 'c1111111-1111-1111-1111-111111111111', 'QBG-006', 6, 'BigO - Phân tích độ phức tạp', 'Kiểm tra trình độ hiểu biết về độ phức tạp giải thuật BigO.');

INSERT INTO question_bank_2_examples (id, group_id, content, sequence)
VALUES
    ('a1100001-0000-4000-8000-000000000001','a1000001-0000-4000-8000-000000000001','Question: after push(10), push(20), pop(), what is returned? Correct: 20. Distractors: 10, null, 0.',1),
    ('a1100001-0000-4000-8000-000000000002','a1000001-0000-4000-8000-000000000001','Question: which operation reads the top of a stack without removing it? Correct: peek. Distractors: pop, push, enqueue.',2),
    ('a1100001-0000-4000-8000-000000000003','a1000001-0000-4000-8000-000000000001','Question: what condition occurs when popping an empty stack? Correct: underflow. Distractors: overflow, collision, recursion.',3),
    ('a1100001-0000-4000-8000-000000000004','a1000001-0000-4000-8000-000000000001','Question: why is a stack suitable for matching parentheses? Correct: last in, first out. Distractors: FIFO, random access, automatic sorting.',4),
    ('a1100001-0000-4000-8000-000000000005','a1000001-0000-4000-8000-000000000002','Question: empty queue; enqueue A, enqueue B, dequeue(). What is returned? Correct: A. Distractors: B, null, 0.',1),
    ('a1100001-0000-4000-8000-000000000006','a1000001-0000-4000-8000-000000000002','Question: what does FIFO mean for a queue? Correct: first in, first out. Distractors: last in, first out, top first, random.',2),
    ('a1100001-0000-4000-8000-000000000007','a1000001-0000-4000-8000-000000000002','Question: which operation appends an item to the rear of a queue? Correct: enqueue. Distractors: dequeue, pop, peek.',3),
    ('a1100001-0000-4000-8000-000000000008','a1000001-0000-4000-8000-000000000003','Question: what property is required before binary search? Correct: sorted input. Distractors: unique values, even length, positive integers.',1),
    ('a1100001-0000-4000-8000-000000000009','a1000001-0000-4000-8000-000000000003','Question: each binary-search step discards approximately what portion of the search interval? Correct: one half. Distractors: one quarter, one third, none.',2),
    ('a1100001-0000-4000-8000-000000000010','a1000001-0000-4000-8000-000000000004','Question: after one Bubble Sort pass, which value moves to the end of the unsorted region? Correct: largest value. Distractors: smallest value, first value, pivot.',1),
    ('a1100001-0000-4000-8000-000000000011','a1000001-0000-4000-8000-000000000005','Sự lặp lại trong trường hợp xấu nhất của Quicksort là gì và độ phức tạp thời gian trong trường hợp xấu nhất là gì?
A. T(n) = T(n - 2) + O(n) và O(n²)
B. T(n) = T(n - 1) + O(n) và O(n²)
C. T(n) = 2T(n / 2) + O(n) và O(n log n)
D. T(n) = T(n / 10) + T(9n / 10) + O(n) và O(n log n)
Đáp án đúng: B.',1),
    ('a1100001-0000-4000-8000-000000000012','a1000001-0000-4000-8000-000000000006','Độ phức tạp thời gian của đoạn lệnh sau là gì?
for (int i = 1; i < n; i *= 2) { ... }
A. O(1)
B. O(log n)
C. O(n)
D. O(n log n)
Đáp án đúng: B.',1),
    ('a1100001-0000-4000-8000-000000000013','a1000001-0000-4000-8000-000000000006','Độ phức tạp thời gian của đoạn lệnh sau là gì?
for (int i = 0; i < n; i++) {
    for (int j = 1; j < n; j *= 2) { ... }
}
A. O(log n)
B. O(n)
C. O(n log n)
D. O(n²)
Đáp án đúng: C.',2);

INSERT INTO question_bank_2_code_counters (course_id, next_value)
VALUES ('c1111111-1111-1111-1111-111111111111', 7);

-- Published Quiz3 base fixture for the student attempt flow.  It has no
-- shuffled variants and remains available after every development reset.
INSERT INTO quiz3_assessments
    (id, course_id, title, description, unlimited, time_limit_minutes, pass_score,
     start_time, due_time, attempt_limited, variant_count, lifecycle_status, current_step,
     ccct_profile_id, ccct_profile_version, matrix_source_type, matrix2_id, matrix2_version,
     base_confirmed_by, base_confirmed_at, created_by, version, published_by, published_at)
VALUES
    ('f9000001-0000-4000-8000-000000000001', 'c1111111-1111-1111-1111-111111111111',
     'Quiz3 Student Attempt Demo', 'Published Quiz3 base assessment for student attempt testing.',
     true, NULL, 5.0, NULL, NULL, NULL, 1, 'PUBLISHED', 9,
     'e1000001-0000-4000-8000-000000000001', 0, 'MATRIX2',
     'b3000001-0000-4000-8000-000000000001', 0,
     '11111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP,
     '11111111-1111-1111-1111-111111111111', 1,
     '11111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP);

INSERT INTO quiz3_generation_jobs
    (id, assessment_id, course_id, matrix_source_type, matrix2_id, matrix2_version,
     requested_by, request_snapshot, payload_hash, idempotency_key, status, phase,
     attempt_count, generated_question_count, total_question_count, completed_at)
VALUES
    ('f9000001-0000-4000-8000-000000000002', 'f9000001-0000-4000-8000-000000000001',
     'c1111111-1111-1111-1111-111111111111', 'MATRIX2',
     'b3000001-0000-4000-8000-000000000001', 0,
     '11111111-1111-1111-1111-111111111111',
     CAST('{"matrix":{"rows":[{"id":"b3000021-0000-4000-8000-000000000001","learningOutcome":{"id":"81111111-1111-1111-1111-111111111111"},"cells":[{"customLearningLevelId":"b3000011-0000-4000-8000-000000000002","questionCount":1}]},{"id":"b3000021-0000-4000-8000-000000000002","learningOutcome":{"id":"84444444-4444-4444-4444-444444444444"},"cells":[{"customLearningLevelId":"b3000011-0000-4000-8000-000000000003","questionCount":2}]}]}}' AS jsonb),
     'a333333333333333333333333333333333333333333333333333333333333333',
     'seed-quiz3-student-attempt-demo-v1', 'COMPLETED', 'DONE', 1, 3, 3, CURRENT_TIMESTAMP);

INSERT INTO quizzes
    (id, quiz_title, description, unlimited, time_limit_minutes, pass_score, start_time, due_time,
     attempt_limited, course_id, teacher_id, display, lifecycle_status, source_type,
     version, published_by, published_at)
VALUES
    ('f9000001-0000-4000-8000-000000000003', 'Quiz3 Student Attempt Demo - 001',
     'Published Quiz3 base assessment for student attempt testing.', true, NULL, 5.0, NULL, NULL,
     NULL, 'c1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111',
     true, 'PUBLISHED', 'AI_GENERATED', 1,
     '11111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP);

UPDATE quiz3_generation_jobs
SET quiz_id = 'f9000001-0000-4000-8000-000000000003'
WHERE id = 'f9000001-0000-4000-8000-000000000002';

UPDATE quiz3_assessments
SET base_generation_job_id = 'f9000001-0000-4000-8000-000000000002'
WHERE id = 'f9000001-0000-4000-8000-000000000001';

INSERT INTO quiz3_assessment_variants
    (id, assessment_id, quiz_id, variant_code, order_index, shuffle_seed, variant_kind,
     coverage_status, coverage_snapshot)
VALUES
    ('f9000001-0000-4000-8000-000000000004', 'f9000001-0000-4000-8000-000000000001',
     'f9000001-0000-4000-8000-000000000003', '001', 0, 424242, 'BASE', 'COMPLIANT',
     CAST('{"status":"COMPLIANT","questionCount":3}' AS jsonb));

INSERT INTO questions
    (id, content, content_format, explanation, explanation_format, type, quiz_id,
     review_status, source_type, order_index)
VALUES
    ('f9000001-0000-4000-8000-000000000011',
     'What is the time complexity of binary search on a sorted array?', 'PLAIN_TEXT',
     'Binary search halves the remaining search interval on each step.', 'PLAIN_TEXT',
     'MULTIPLE_CHOICE', 'f9000001-0000-4000-8000-000000000003', 'APPROVED', 'AI_GENERATED', 0),
    ('f9000001-0000-4000-8000-000000000012',
     'Which data structure follows the LIFO principle?', 'PLAIN_TEXT',
     'LIFO means the last inserted item is the first item removed.', 'PLAIN_TEXT',
     'MULTIPLE_CHOICE', 'f9000001-0000-4000-8000-000000000003', 'APPROVED', 'AI_GENERATED', 1),
    ('f9000001-0000-4000-8000-000000000013',
     'Which operation removes an item from the front of a queue?', 'PLAIN_TEXT',
     'A queue removes its oldest item with dequeue.', 'PLAIN_TEXT',
     'MULTIPLE_CHOICE', 'f9000001-0000-4000-8000-000000000003', 'APPROVED', 'AI_GENERATED', 2);

INSERT INTO answers (content, content_format, is_correct, explanation, explanation_format, question_id)
VALUES
    ('O(1)', 'PLAIN_TEXT', false, NULL, 'PLAIN_TEXT', 'f9000001-0000-4000-8000-000000000011'),
    ('O(log n)', 'PLAIN_TEXT', true, NULL, 'PLAIN_TEXT', 'f9000001-0000-4000-8000-000000000011'),
    ('O(n)', 'PLAIN_TEXT', false, NULL, 'PLAIN_TEXT', 'f9000001-0000-4000-8000-000000000011'),
    ('O(n log n)', 'PLAIN_TEXT', false, NULL, 'PLAIN_TEXT', 'f9000001-0000-4000-8000-000000000011'),
    ('Queue', 'PLAIN_TEXT', false, NULL, 'PLAIN_TEXT', 'f9000001-0000-4000-8000-000000000012'),
    ('Stack', 'PLAIN_TEXT', true, NULL, 'PLAIN_TEXT', 'f9000001-0000-4000-8000-000000000012'),
    ('Binary heap', 'PLAIN_TEXT', false, NULL, 'PLAIN_TEXT', 'f9000001-0000-4000-8000-000000000012'),
    ('Hash table', 'PLAIN_TEXT', false, NULL, 'PLAIN_TEXT', 'f9000001-0000-4000-8000-000000000012'),
    ('enqueue', 'PLAIN_TEXT', false, NULL, 'PLAIN_TEXT', 'f9000001-0000-4000-8000-000000000013'),
    ('dequeue', 'PLAIN_TEXT', true, NULL, 'PLAIN_TEXT', 'f9000001-0000-4000-8000-000000000013'),
    ('peek', 'PLAIN_TEXT', false, NULL, 'PLAIN_TEXT', 'f9000001-0000-4000-8000-000000000013'),
    ('push', 'PLAIN_TEXT', false, NULL, 'PLAIN_TEXT', 'f9000001-0000-4000-8000-000000000013');

INSERT INTO quiz_learning_outcomes (quiz_id, learning_outcome_id)
VALUES
    ('f9000001-0000-4000-8000-000000000003', '81111111-1111-1111-1111-111111111111'),
    ('f9000001-0000-4000-8000-000000000003', '84444444-4444-4444-4444-444444444444');

INSERT INTO question_learning_outcomes (question_id, learning_outcome_id)
VALUES
    ('f9000001-0000-4000-8000-000000000011', '81111111-1111-1111-1111-111111111111'),
    ('f9000001-0000-4000-8000-000000000012', '84444444-4444-4444-4444-444444444444'),
    ('f9000001-0000-4000-8000-000000000013', '84444444-4444-4444-4444-444444444444');

INSERT INTO quiz3_question_provenance
    (question_id, generation_job_id, matrix2_row_id, matrix2_custom_learning_level_id,
     learning_outcome_id, allocation_snapshot)
VALUES
    ('f9000001-0000-4000-8000-000000000011', 'f9000001-0000-4000-8000-000000000002',
     'b3000021-0000-4000-8000-000000000001', 'b3000011-0000-4000-8000-000000000002',
     '81111111-1111-1111-1111-111111111111',
     CAST('{"matrixRowId":"b3000021-0000-4000-8000-000000000001","customLearningLevelId":"b3000011-0000-4000-8000-000000000002","learningOutcomeId":"81111111-1111-1111-1111-111111111111"}' AS jsonb)),
    ('f9000001-0000-4000-8000-000000000012', 'f9000001-0000-4000-8000-000000000002',
     'b3000021-0000-4000-8000-000000000002', 'b3000011-0000-4000-8000-000000000003',
     '84444444-4444-4444-4444-444444444444',
     CAST('{"matrixRowId":"b3000021-0000-4000-8000-000000000002","customLearningLevelId":"b3000011-0000-4000-8000-000000000003","learningOutcomeId":"84444444-4444-4444-4444-444444444444"}' AS jsonb)),
    ('f9000001-0000-4000-8000-000000000013', 'f9000001-0000-4000-8000-000000000002',
     'b3000021-0000-4000-8000-000000000002', 'b3000011-0000-4000-8000-000000000003',
     '84444444-4444-4444-4444-444444444444',
     CAST('{"matrixRowId":"b3000021-0000-4000-8000-000000000002","customLearningLevelId":"b3000011-0000-4000-8000-000000000003","learningOutcomeId":"84444444-4444-4444-4444-444444444444"}' AS jsonb));

-- 21. Published mixed practice quiz. This is intentionally separate from the
-- legacy Quiz 1 Code Runner fixture so students can exercise a complete quiz
-- containing both a conventional question and an executable-code question.
INSERT INTO quizzes
    (id, quiz_title, description, unlimited, time_limit_minutes, pass_score,
     attempt_limited, course_id, teacher_id, display, lifecycle_status,
     source_type, version, published_by, published_at)
VALUES
    ('f9100001-0000-4000-8000-000000000001', 'DSA Practice: Arrays and Functions',
     'A mixed practice quiz with a multiple-choice question and a C++17 Code Runner exercise.',
     true, NULL, 5.0, NULL, 'c1111111-1111-1111-1111-111111111111',
     '11111111-1111-1111-1111-111111111111', true, 'PUBLISHED', 'MANUAL', 1,
     '11111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP);

INSERT INTO questions
    (id, content, content_format, explanation, explanation_format, type, quiz_id,
     review_status, source_type, order_index)
VALUES
    ('f9100001-0000-4000-8000-000000000011',
     'Which time complexity describes accessing an array element by a valid index?',
     'PLAIN_TEXT', 'An array address is calculated directly from its base address and index.',
     'PLAIN_TEXT', 'MULTIPLE_CHOICE', 'f9100001-0000-4000-8000-000000000001',
     'APPROVED', 'MANUAL', 0),
    ('f9100001-0000-4000-8000-000000000012',
     'Implement multiply(a, b), which returns the product of two integers.',
     'PLAIN_TEXT', 'Return the multiplication of the two integer parameters.',
     'PLAIN_TEXT', 'CODE_RUNNER', 'f9100001-0000-4000-8000-000000000001',
     'APPROVED', 'MANUAL', 1);

INSERT INTO answers (content, content_format, is_correct, explanation, explanation_format, question_id)
VALUES
    ('O(1)', 'PLAIN_TEXT', true, 'Indexed array access is constant time.', 'PLAIN_TEXT', 'f9100001-0000-4000-8000-000000000011'),
    ('O(log n)', 'PLAIN_TEXT', false, 'Logarithmic time is typical of operations such as binary search.', 'PLAIN_TEXT', 'f9100001-0000-4000-8000-000000000011'),
    ('O(n)', 'PLAIN_TEXT', false, 'A sequential scan is linear, unlike direct indexed access.', 'PLAIN_TEXT', 'f9100001-0000-4000-8000-000000000011'),
    ('O(n log n)', 'PLAIN_TEXT', false, 'This is commonly associated with efficient comparison sorting.', 'PLAIN_TEXT', 'f9100001-0000-4000-8000-000000000011');

INSERT INTO quiz_chapters (quiz_id, chapter_id)
VALUES ('f9100001-0000-4000-8000-000000000001', '5e055691-371a-4de5-b61a-f6d06712c36a');

INSERT INTO quiz_learning_outcomes (quiz_id, learning_outcome_id)
VALUES
    ('f9100001-0000-4000-8000-000000000001', '81111111-1111-1111-1111-111111111111'),
    ('f9100001-0000-4000-8000-000000000001', '82222222-2222-2222-2222-222222222222');

INSERT INTO question_learning_outcomes (question_id, learning_outcome_id)
VALUES
    ('f9100001-0000-4000-8000-000000000011', '81111111-1111-1111-1111-111111111111'),
    ('f9100001-0000-4000-8000-000000000012', '82222222-2222-2222-2222-222222222222');

INSERT INTO quiz_releases (id, quiz_id, release_number, quiz_version, active, published_by, published_at)
VALUES
    ('f9100001-0000-4000-8000-000000000021', 'f9100001-0000-4000-8000-000000000001',
     1, 1, true, '11111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP);

INSERT INTO code_runner_configs
    (id, question_id, schema_version, contract_id, contract_version, contract_status,
     title, statement, execution_profile_id, execution_profile_version, archetype_id,
     archetype_version, comparator_id, comparator_version, entry_point, signature_json,
     starter_code, reference_solution, revision)
VALUES
    ('f9100001-0000-4000-8000-000000000022', 'f9100001-0000-4000-8000-000000000012',
     '2.0.0', 'multiply_two_integers', '1.0.0', 'VERIFIED_EXECUTION_GOLD',
     'Multiply two integers', 'Write a function that takes integers a and b and returns a multiplied by b.',
     'dsa_cpp17', '1.0.0', 'scalar_function', '1.0.0', 'exact_typed', '1.0.0', 'multiply',
     '{"return_fixture":{"id":"integer","version":"1.0.0"},"arguments":[{"name":"a","fixture":{"id":"integer","version":"1.0.0"},"constraints":{}},{"name":"b","fixture":{"id":"integer","version":"1.0.0"},"constraints":{}}]}'::jsonb,
     'int multiply(int a, int b) {\n    // Write your code here\n    return 0;\n}',
     'int multiply(int a, int b) {\n    return a * b;\n}', 1);

INSERT INTO code_runner_test_cases
    (id, code_runner_config_id, external_test_id, category, visibility, arguments_json,
     expected_output_json, points, ordinal)
VALUES
    ('f9100001-0000-4000-8000-000000000031', 'f9100001-0000-4000-8000-000000000022',
     'public_1', 'basic', 'PUBLIC', '{"a":3,"b":4}'::jsonb, '12'::jsonb, 0.0, 1),
    ('f9100001-0000-4000-8000-000000000032', 'f9100001-0000-4000-8000-000000000022',
     'hidden_1', 'negative_operand', 'HIDDEN', '{"a":-5,"b":6}'::jsonb, '-30'::jsonb, 5.0, 2);

-- The fixture records a deterministic successful verification of the reference
-- solution. Live student execution still runs through Judge0.
INSERT INTO code_runner_verifications
    (id, revision_id, verified_by, verified_at, execution_profile, judge0_submission_id,
     verdict, stdout, stderr, time_ms, memory_kb, wall_time_limit_ms, memory_limit_kb)
VALUES
    ('f9100001-0000-4000-8000-000000000041', 'f9100001-0000-4000-8000-000000000022',
     '11111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP,
     '{"id":"dsa_cpp17","version":"1.0.0"}'::jsonb, 'seed-fixture-multiply-reference',
     'ACCEPTED', '12\n-30', '', 1.0, 1024, 5000, 131072);

INSERT INTO code_runner_verification_cases
    (id, verification_id, external_test_id, visibility, verdict, time_ms, memory_kb, diagnostic)
VALUES
    ('f9100001-0000-4000-8000-000000000051', 'f9100001-0000-4000-8000-000000000041',
     'public_1', 'PUBLIC', 'ACCEPTED', 1.0, 1024, ''),
    ('f9100001-0000-4000-8000-000000000052', 'f9100001-0000-4000-8000-000000000041',
     'hidden_1', 'HIDDEN', 'ACCEPTED', 1.0, 1024, '');

INSERT INTO quiz_release_code_runner_bindings (quiz_release_id, question_id, code_runner_config_id)
VALUES
    ('f9100001-0000-4000-8000-000000000021', 'f9100001-0000-4000-8000-000000000012',
     'f9100001-0000-4000-8000-000000000022');

COMMIT;
