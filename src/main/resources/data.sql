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
    ('c1111111-1111-1111-1111-111111111111', 'Cấu trúc dữ liệu và giải thuật', 'Cấu trúc dữ liệu cốt lõi, kỹ thuật thiết kế giải thuật, phân tích độ phức tạp, sắp xếp, tìm kiếm, cây, đồ thị, băm và quy hoạch động.', 'CO2003', true, 'https://example.com/covers/cs201-dsa.jpg', TO_TIMESTAMP('01-09-2025 08:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('15-12-2025 17:00:00', 'DD-MM-YYYY HH24:MI:SS'));

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
    ('b1111111-1111-1111-1111-111111111111', 'c1111111-1111-1111-1111-111111111111', 'Ma trận đề thi cuối kỳ môn DSA', 'Bản thiết kế bài thi cuối kỳ cân bằng cho môn Cấu trúc dữ liệu và giải thuật bao gồm phân tích độ phức tạp, các cấu trúc dữ liệu cốt lõi, cây, đồ thị, giải thuật tham lam và quy hoạch động.', '2026-07-19 06:37:47.516609', '2026-07-19 07:44:49.120831');

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

-- 12. Insert Quizzes
INSERT INTO quizzes (id, quiz_title, unlimited, time_limit_minutes, pass_score, start_time, due_time, attempt_limited, course_id, teacher_id, display)
VALUES
    ('91111111-1111-1111-1111-111111111111', 'DSA Bài kiểm tra 1: Độ phức tạp và Cấu trúc tuyến tính', false, 60, 70.0, TO_TIMESTAMP('15-10-2025 09:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('15-10-2025 11:00:00', 'DD-MM-YYYY HH24:MI:SS'), 1, 'c1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', true),
    ('92222222-2222-2222-2222-222222222222', 'DSA Thi giữa kỳ: Sắp xếp và Tìm kiếm', false, 120, 75.0, TO_TIMESTAMP('10-11-2025 13:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('10-11-2025 15:00:00', 'DD-MM-YYYY HH24:MI:SS'), 1, 'c1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', true),
    ('93333333-3333-3333-3333-333333333333', 'DSA Bài kiểm tra 2: Heaps và Hàng đợi ưu tiên', false, 45, 70.0, TO_TIMESTAMP('20-11-2025 10:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('20-11-2025 11:00:00', 'DD-MM-YYYY HH24:MI:SS'), 2, 'c1111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', true),
    ('94444444-4444-4444-4444-444444444444', 'DSA Bài kiểm tra 3: Cây và Đồ thị', false, 75, 80.0, TO_TIMESTAMP('01-12-2025 09:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('01-12-2025 10:30:00', 'DD-MM-YYYY HH24:MI:SS'), 2, 'c1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', true),
    ('95555555-5555-5555-5555-555555555555', 'DSA Thi cuối kỳ: Sắp xếp, Tìm kiếm, và Đồ thị', false, 120, 85.0, TO_TIMESTAMP('12-12-2025 14:00:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('12-12-2025 16:00:00', 'DD-MM-YYYY HH24:MI:SS'), 1, 'c1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', true);

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
    ('Đúng', true, 'Tìm kiếm nhị phân chia đôi khoảng tìm kiếm ở mỗi bước, do đó mất thời gian logarit.', 'd1111111-1111-1111-1111-111111111111'),
    ('Sai', false, 'Tìm kiếm tuyến tính là O(n), nhưng tìm kiếm nhị phân trên dữ liệu đã sắp xếp là O(log n).', 'd1111111-1111-1111-1111-111111111111'),
    ('Hàng đợi (Queue)', false, 'Hàng đợi cung cấp truy cập FIFO: vào trước, ra trước.', 'd2222222-2222-2222-2222-222222222222'),
    ('Ngăn xếp (Stack)', true, 'Ngăn xếp cung cấp truy cập LIFO: vào sau, ra trước.', 'd2222222-2222-2222-2222-222222222222'),
    ('Bảng băm (Hash Table)', false, 'Bảng băm ánh xạ khóa với giá trị và không áp đặt hành vi LIFO.', 'd2222222-2222-2222-2222-222222222222'),
    ('Đúng', true, 'Sắp xếp trộn phân tách đầu vào một cách đệ quy và gộp các nửa đã sắp xếp trong thời gian O(n log n).', 'd4444444-4444-4444-4444-444444444444'),
    ('Sai', false, 'Sắp xếp trộn có thời gian trường hợp xấu nhất là O(n log n), không phải hành vi bậc hai.', 'd4444444-4444-4444-4444-444444444444'),
    ('Đúng', true, 'Một binary heap kết hợp thuộc tính cấu trúc cây hoàn chỉnh với thứ tự min-heap hoặc max-heap.', 'd5555555-5555-5555-5555-555555555555'),
    ('Sai', false, 'Binary heaps phải thỏa mãn cả thuộc tính cấu trúc cây hoàn chỉnh và thuộc tính thứ tự heap thích hợp.', 'd5555555-5555-5555-5555-555555555555'),
    ('Binary Heap', true, 'Một binary heap hỗ trợ việc chèn và loại bỏ một cách hiệu quả các phần tử có mức ưu tiên cao nhất hoặc thấp nhất.', 'd6666666-6666-6666-6666-666666666666'),
    ('Danh sách liên kết (Linked List)', false, 'Danh sách liên kết tự nó không duy trì thứ tự ưu tiên một cách hiệu quả.', 'd6666666-6666-6666-6666-666666666666'),
    ('Ngăn xếp (Stack)', false, 'Ngăn xếp hữu ích cho xử lý LIFO, không phải để truy xuất dựa trên độ ưu tiên.', 'd6666666-6666-6666-6666-666666666666'),
    ('Tìm kiếm theo chiều sâu (DFS)', false, 'DFS khám phá theo chiều sâu và không đảm bảo tìm được đường đi ngắn nhất theo số cạnh.', 'd7777777-7777-7777-7777-777777777777'),
    ('Tìm kiếm theo chiều rộng (BFS)', true, 'BFS khám phá đồ thị theo từng mức, đưa ra các đường đi ngắn nhất trong các đồ thị không trọng số.', 'd7777777-7777-7777-7777-777777777777'),
    ('Sắp xếp tô pô (Topological Sort)', false, 'Sắp xếp tô pô sắp xếp các đỉnh của DAG theo sự phụ thuộc, nhưng không phải là phương pháp duyệt tìm đường đi ngắn nhất.', 'd7777777-7777-7777-7777-777777777777'),
    ('Đúng', true, 'Duyệt in-order truy cập theo thứ tự cây con trái, nút gốc, sau đó đến cây con phải, điều này tạo ra các khóa đã sắp xếp trong một BST.', 'd8888888-8888-8888-8888-888888888888'),
    ('Sai', false, 'Pre-order và post-order có thứ tự truy cập khác nhau; in-order là cách duyệt đã sắp xếp cho các BST.', 'd8888888-8888-8888-8888-888888888888'),
    ('Sắp xếp trộn (Merge Sort)', true, 'Sắp xếp trộn đảm bảo thời gian O(n log n) trong trường hợp tốt nhất, trung bình và xấu nhất.', 'd9999999-9999-9999-9999-999999999999'),
    ('Sắp xếp nổi bọt (Bubble Sort)', false, 'Sắp xếp nổi bọt có thời gian trường hợp xấu nhất là O(n^2).', 'd9999999-9999-9999-9999-999999999999'),
    ('Sắp xếp chèn (Insertion Sort)', false, 'Sắp xếp chèn có thời gian trường hợp xấu nhất là O(n^2).', 'd9999999-9999-9999-9999-999999999999'),
    ('Đúng', false, 'Giải thuật Dijkstra giả định các trọng số cạnh không âm.', 'daaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    ('Sai', true, 'Các đồ thị có trọng số âm yêu cầu các thuật toán như Bellman-Ford hoặc cần điều chỉnh trọng số cẩn thận.', 'daaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa');

-- 15. Insert Attempts (Giữ nguyên)
INSERT INTO attempts (id, student_id, quiz_id, score, start_time, due_time, submitted_time, time_spent_seconds, status, error_reason)
VALUES
    ('e1111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', '91111111-1111-1111-1111-111111111111', 85.0, TO_TIMESTAMP('15-10-2025 09:10:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('15-10-2025 10:10:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('15-10-2025 10:05:00', 'DD-MM-YYYY HH24:MI:SS'), 3300, 'FINISHED', NULL),
    ('e2222222-2222-2222-2222-222222222222', '55555555-5555-5555-5555-555555555555', '93333333-3333-3333-3333-333333333333', 90.0, TO_TIMESTAMP('20-11-2025 10:05:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('20-11-2025 10:50:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('20-11-2025 10:43:00', 'DD-MM-YYYY HH24:MI:SS'), 2280, 'FINISHED', NULL),
    ('e3333333-3333-3333-3333-333333333333', '66666666-6666-6666-6666-666666666666', '95555555-5555-5555-5555-555555555555', NULL, TO_TIMESTAMP('12-12-2025 14:15:00', 'DD-MM-YYYY HH24:MI:SS'), TO_TIMESTAMP('12-12-2025 16:15:00', 'DD-MM-YYYY HH24:MI:SS'), NULL, NULL, 'STARTED', NULL);

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
    ('daaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '86666666-6666-6666-6666-666666666666'),
    ('dbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '8854781e-efc8-4130-9451-3b0391a32419');

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
     $$# CCCT – Độ phức tạp giải thuật

## Độ phức tạp giải thuật

### Phân tích độ phức tạp tiệm cận
- Big-O (Cận trên / Upper bound) — Learn and exam
- Big-Ω (Cận dưới / Lower bound) — Learn and exam$$,
     '11111111-1111-1111-1111-111111111111'),
    ('e1000001-0000-4000-8000-000000000002', 'c1111111-1111-1111-1111-111111111111',
     'CCCT – Heap', 'CCCT – Heap', 'DRAFT', 0, 2,
     true, 'STRUCTURED',
     $$# CCCT – Heap

## Heap

### Các loại Heap
- Max-Heap — Learn and exam
- Min-Heap — Learn and exam$$,
     '11111111-1111-1111-1111-111111111111'),
    ('e1000001-0000-4000-8000-000000000003', 'c1111111-1111-1111-1111-111111111111',
     'CCCT – Đồ thị', 'CCCT – Đồ thị', 'DRAFT', 0, 2,
     true, 'STRUCTURED',
     $$# CCCT – Đồ thị

## Đồ thị

### Duyệt đồ thị
- BFS (Duyệt theo chiều rộng) — Learn and exam
- DFS (Duyệt theo chiều sâu) — Learn and exam$$,
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

-- Reset fixtures: the original PDFs remain in the private Supabase bucket.
-- Each boot recreates their LMS metadata and queues fresh AI ingestion instead
-- of storing an expiring signed URL in the seed data.
INSERT INTO materials
    (id, title, type, file_key, is_downloadable, status, ingestion_status, ingestion_revision, course_id)
VALUES
    ('f1000001-0000-4000-8000-000000000001',
     'DCMH.CO2003_Data_Structures_and_AlgoriThms.pdf',
     'PDF',
     'courses/c1111111-1111-1111-1111-111111111111/DCMH.CO2003_Data_Structures_and_AlgoriThms.pdf',
     true, 'UPLOADED', 'PENDING', 'f2000001-0000-4000-8000-000000000001',
     'c1111111-1111-1111-1111-111111111111'),
    ('f1000001-0000-4000-8000-000000000002',
     'DSABook_1.pdf',
     'PDF',
     'courses/c1111111-1111-1111-1111-111111111111/DSABook_1.pdf',
     true, 'UPLOADED', 'PENDING', 'f2000001-0000-4000-8000-000000000002',
     'c1111111-1111-1111-1111-111111111111');

INSERT INTO material_index_jobs
    (id, course_id, material_id, operation, revision, file_key, status, attempt_count, next_attempt_at)
VALUES
    ('f3000001-0000-4000-8000-000000000001',
     'c1111111-1111-1111-1111-111111111111', 'f1000001-0000-4000-8000-000000000001',
     'INGEST', 'f2000001-0000-4000-8000-000000000001',
     'courses/c1111111-1111-1111-1111-111111111111/DCMH.CO2003_Data_Structures_and_AlgoriThms.pdf',
     'PENDING', 0, NOW()),
    ('f3000001-0000-4000-8000-000000000002',
     'c1111111-1111-1111-1111-111111111111', 'f1000001-0000-4000-8000-000000000002',
     'INGEST', 'f2000001-0000-4000-8000-000000000002',
     'courses/c1111111-1111-1111-1111-111111111111/DSABook_1.pdf',
     'PENDING', 0, NOW());
