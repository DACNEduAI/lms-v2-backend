# Question Bank — Tóm tắt phần đã triển khai

## Flow nghiệp vụ

```text
Question Draft --Validate--> Bank Question --Add to Quiz--> Quiz Question Snapshot
  được sửa/xóa              đã duyệt, read-only          bản copy độc lập
```

- Validate chuyển toàn bộ Draft sang Bank trong một transaction.
- Chỉ Bank Question đã duyệt mới được thêm vào Quiz.
- Add to Quiz copy nội dung, đáp án và LO; sửa/xóa câu trong Quiz không ảnh hưởng Bank.

## Số bảng

### 7 bảng Question Bank

1. `cognitive_levels`: 6 mức Bloom chuẩn.
2. `question_drafts`: câu chưa duyệt.
3. `question_draft_answers`: đáp án Draft.
4. `question_draft_learning_outcomes`: Draft ↔ LO.
5. `question_bank_questions`: câu đã duyệt.
6. `question_bank_answers`: đáp án Bank.
7. `question_bank_question_learning_outcomes`: Bank Question ↔ LO.

### 4 bảng Quiz liên quan

- `quizzes`
- `questions`
- `answers`
- `question_learning_outcomes`

Flow chạm tổng cộng **11 bảng**: 7 bảng Question Bank và 4 bảng Quiz có sẵn.

## Quan hệ chính

```text
Course 1 ── N Question Draft
Course 1 ── N Bank Question

Question Draft 1 ── N Draft Answer
Question Draft N ── N Learning Outcome

Bank Question 1 ── N Bank Answer
Bank Question N ── N Learning Outcome

Cognitive Level 1 ── N Draft/Bank Question

Quiz 1 ── N Quiz Question Snapshot
Bank Question 1 ── N Quiz Question Snapshot (truy vết nguồn)
```

`origin_draft_id` lưu ID Draft nguồn để audit nhưng không có FK vì Draft bị xóa sau Validate.

## Quyết định đã chốt

- Hai root table vật lý riêng, không dùng một bảng + `status`.
- Java domain tách `QuestionDraft` và `QuestionBankQuestion`.
- MVP chỉ hỗ trợ `MULTIPLE_CHOICE`.
- Giữ Cognitive Level; bỏ Difficulty.
- Không có quan hệ CCCT ↔ Question Bank.
- Trước Validate bắt buộc có Chapter, ít nhất một LO, Cognitive Level, ít nhất hai đáp án và đúng một đáp án đúng.
- Bank Question read-only trong MVP.
- Quiz nhận snapshot, không đọc live content từ Bank.

## API và UI

- `/question-drafts`: CRUD Draft.
- `/question-drafts/{id}/validate`: chuyển Draft sang Bank.
- `/question-bank/questions`: câu đã duyệt.
- `/question-bank/cognitive-levels`: danh mục Cognitive Level.
- `/quizzes/{id}/questions/from-bank`: copy câu vào Quiz.

UI có hai bảng Not validated/Validated, Draft editor bên dưới, danh sách Quiz Draft và câu trong Quiz bên phải. Visual dùng design system hiện có.

## Ngoài phạm vi MVP

- Code Runner, Essay và loại câu hỏi khác.
- Revision/version chain và Retire Bank Question.
- Tự động map Question Bank với CCCT hoặc Exam Matrix Row.
- Workflow riêng theo nguồn AI/Teacher.

## Kiểm tra

- Backend compile/test-compile và unit test thành công.
- PostgreSQL integration test bằng Docker/Testcontainers thành công.
- Frontend typecheck và API contract tests thành công.
