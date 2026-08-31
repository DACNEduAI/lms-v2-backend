# Question Bank 2 (QBank2) Feature Reference

This document describes the implemented Question Bank 2 feature across:

- `lms-v2-backend`: Spring Boot API, persistence, authorization, Quiz 3 integration, and AI synchronization.
- `lms-v4-frontend`: teacher-facing QBank2 management and Quiz 3 regeneration UI.
- `ai_pipeline`: QBank2 vector indexing, automatic example selection, and AI question regeneration. This is not part of the Spring Boot repository, but it is included because the backend calls it directly.

QBank2 is separate from the older Question Bank feature. QBank2 stores course-owned, plain-text examples grouped by content area. The examples act as few-shot style references for Quiz 3 generation; they are not directly copied into a quiz as ordinary bank questions.

## 1. Functional Overview

QBank2 currently supports four connected workflows.

### 1.1 Manage course content groups and examples

A teacher can:

- View all QBank2 content groups for a course.
- Create a content group with a required name and description.
- Receive an immutable, course-scoped generated code such as `QBG-001`.
- Edit a group's name and description.
- Delete a group and all of its examples.
- Add plain-text examples to a group.
- Edit or delete individual examples.
- Search groups by group code or description.

### 1.2 Automatically guide Quiz 3 base generation

For each Quiz 3 generation slot, the AI pipeline:

- Builds a search query from the custom learning level name and learning-outcome code.
- Searches the course-scoped QBank2 vector collection.
- Selects at most one highest-scoring example.
- Uses the selected example as a question-style contract instead of material RAG context.
- Falls back to ordinary material RAG when no QBank2 example is available or selection fails.
- Returns a QBank2 reference snapshot with each generated question.

The Spring backend persists this snapshot as question provenance. The possible selection modes are:

- `AUTO_SELECTED`: the vector search selected an example.
- `RAG_FALLBACK`: no QBank2 example was used, so material RAG generated the question.
- `MANUAL_OVERRIDE`: a teacher regenerated the question using the QBank2 review workflow.

### 1.3 Manually regenerate a Quiz 3 review question

While reviewing the base Quiz 3 variant (`Quiz 001`), a teacher can:

- Open the **Tạo lại với QBank2** modal for a question.
- Search QBank2 groups by code or description.
- Select zero or one example.
- Start an asynchronous regeneration job.
- Regenerate without an example, using the original question and matrix metadata as context.
- Regenerate with one example, using it as the authoritative remix/style reference.
- Poll the job until a working draft is available.
- Load the generated question, four answers, correct-answer choice, and explanation into the editor.
- Edit the generated draft before saving.
- Save through the normal optimistic-lock question update flow.
- Cancel editing and discard the QBank2 working draft.

Saving the question commits `MANUAL_OVERRIDE` provenance. Merely generating or loading a draft does not change the saved question.

### 1.4 Import and update content with Excel

A teacher can download a template for the current group context or export one selected group for editing. Each workbook contains only protected `Metadata` and `Examples` sheets; there is no Groups sheet or group-key column. Metadata contains `Mã môn học`, `Mã nhóm câu hỏi`, `Tên nhóm câu hỏi`, and `Mô tả nhóm câu hỏi`; creation time is not included. In New group mode, name and description are editable and optionally prefilled from the current form. Both a new-group template and an existing-group export can create a new group: the workbook name, description, and example content are used while its source code and example IDs are discarded, and Spring generates new identifiers. When importing into the same selected existing group, its saved code/name/description remain unchanged and the workbook becomes its complete replacement example set. The frontend classifies new, modified, deleted, and unchanged rows, and Spring verifies that diff before synchronously updating relational and vector data.

## 2. Frontend Features

### 2.1 Course QBank2 tab

The course page exposes a teacher-only **QBank2** tab. Students do not receive this tab.

The panel contains:

- A group list ordered by backend `codeSequence`.
- Automatic selection of the first group when the panel opens.
- A group create/edit form.
- A read-only generated group-code display.
- Group deletion with browser confirmation.
- An example list with preserved line breaks.
- Inline example editing with save and cancel actions.
- Example deletion with browser confirmation.
- A new-example text area with a suggested question/answer/distractor format.
- Shared pending state that disables conflicting writes.
- Loading and API-error states.

All successful mutations invalidate the React Query key `['qbank2', courseId]`, causing the complete course bank to be reloaded.

### 2.2 Group search

The **Search content group** modal:

- Searches only the already-loaded course groups; it does not call a server-side search endpoint.
- Matches `code` and `description`.
- Is case-insensitive and accent-insensitive, including Vietnamese `đ`/`Đ` normalization.
- Ranks exact code, code prefix, description prefix, code substring, then description substring.
- Preserves the original group order when ranks are equal.
- Requires explicit selection and confirmation before navigating to a group.
- Supports closing with the close button, Cancel, or Escape.

Group `name` is displayed in the panel but is not part of the search match.

### 2.3 Quiz 3 QBank2 regeneration modal

The regeneration action is displayed on non-read-only Quiz 3 question cards. The modal:

- Reuses the QBank2 course query cache and group search helper.
- Displays matching groups and their examples.
- Enforces zero-or-one example selection in the UI; selecting another example replaces the previous selection.
- Allows regeneration with no selected example.
- Polls the job endpoint every 1.2 seconds while status is `QUEUED` or `RUNNING`.
- Handles an HTTP `409` response as an existing active job and continues monitoring it.
- Fetches the completed working draft after status becomes `APPLIED_TO_WORKING_DRAFT`.
- Applies the draft to local editor state and opens question editing.
- Leaves persistence to the normal **Save question** action.

The question card also displays QBank2 provenance metadata:

- Content group codes (`metadata.contentGroupIds` in the current frontend contract).
- Example IDs.

The backend additionally returns `qbank2SelectionMode`, although the current card does not render it directly.

## 3. Spring Boot API

All QBank2 management and Quiz 3 endpoints require `ROLE_TEACHER`. The service layer also verifies that the signed-in teacher has access to the course.

### 3.1 QBank2 management endpoints

Base path: `/api/courses/{courseId}/qbank2/groups`

| Method | Path | Function | Success response |
| --- | --- | --- | --- |
| `GET` | `/` | List all groups and nested examples for the course | `200 QuestionBank2View` |
| `POST` | `/` | Create a group and generate its code | `201 GroupView` |
| `PUT` | `/{groupId}` | Update group name and description | `200 GroupView` |
| `DELETE` | `/{groupId}` | Delete a group and its examples | `204` |
| `POST` | `/{groupId}/examples` | Append an example to a group | `201 ExampleView` |
| `PUT` | `/{groupId}/examples/{exampleId}` | Update example content | `200 ExampleView` |
| `DELETE` | `/{groupId}/examples/{exampleId}` | Delete an example | `204` |

Group input:

```json
{
  "name": "Stack implementation",
  "description": "Apply push, pop, and LIFO behavior"
}
```

Example input:

```json
{
  "content": "Question: ...\nCorrect answer: ...\nDistractors: ..."
}
```

List response shape:

```json
{
  "courseId": "uuid",
  "groups": [
    {
      "id": "uuid",
      "code": "QBG-001",
      "name": "Stack",
      "description": "Stack operations",
      "codeSequence": 1,
      "examples": [
        {
          "id": "uuid",
          "content": "Question: ...",
          "sequence": 1,
          "createdAt": "date-time",
          "updatedAt": "date-time"
        }
      ],
      "createdAt": "date-time",
      "updatedAt": "date-time"
    }
  ]
}
```

### 3.2 Quiz 3 manual-regeneration endpoints

Base path: `/api/courses/{courseId}/quiz3/assessments`

| Method | Path suffix | Function | Success response |
| --- | --- | --- | --- |
| `POST` | `/{assessmentId}/variants/{variantId}/questions/{questionId}/qbank2-regenerate` | Start regeneration with zero or one example ID | `202 QBank2RegenerationJobView` |
| `GET` | `/{assessmentId}/variants/{variantId}/questions/{questionId}/qbank2-regeneration-jobs/{jobId}` | Read job state | `200 QBank2RegenerationJobView` |
| `GET` | `/{assessmentId}/variants/{variantId}/questions/{questionId}/qbank2-regeneration-draft` | Read the latest working draft, or `null` | `200 QBank2RegenerationDraft` |
| `POST` | `/{assessmentId}/variants/{variantId}/questions/{questionId}/qbank2-regeneration-draft/discard` | Cancel an active job and delete the draft | `204` |

Start request:

```json
{
  "exampleIds": ["uuid"]
}
```

`exampleIds` may be empty or contain one unique ID. More than one ID is rejected with `400 Bad Request`.

If the question already has a `QUEUED` or `RUNNING` job, the start endpoint returns that job with HTTP `409 Conflict`. The frontend treats this as a resumable, existing operation.

Job states:

| Status | Meaning |
| --- | --- |
| `QUEUED` | Persisted and waiting for an executor slot |
| `RUNNING` | Claimed and calling the AI service |
| `APPLIED_TO_WORKING_DRAFT` | AI output passed validation and was saved as an editable draft |
| `FAILED` | Generation or output validation failed |
| `CANCELLED` | Discarded by the teacher |
| `TIMED_OUT` | Queue or processing lease exceeded three minutes |

### 3.3 Internal AI calls made by Spring Boot

These paths are relative to the configured `aiWebClient` base URL.

| Method | AI path | Caller | Purpose |
| --- | --- | --- | --- |
| `POST` | `/qbank2/examples/index` | `QBank2VectorSyncService` | Embed and upsert one committed example |
| `POST` | `/qbank2/examples/delete` | `QBank2VectorSyncService` | Remove one example vector |
| `POST` | `/quiz3/questions/regenerate-with-qbank2` | `Quiz3QBank2RegenerationService` | Generate one full four-option MCQ draft |
| `POST` | `/quiz3/generate` | `Quiz3GenerationDispatcher` | Generate the Quiz 3 base quiz; QBank2 selection occurs inside the AI pipeline |

### 3.4 QBank2 Excel import endpoints

Base path: `/api/courses/{courseId}/qbank2/imports`

| Method | Path | Function |
| --- | --- | --- |
| `POST` | `/preview` | Validate and preview a new-group import |
| `PUT` | `/preview` | Validate the diff for an existing-group replacement |
| `POST` | `/` | Create one group and synchronously index its examples |
| `PUT` | `/` | Replace one existing group's example set and synchronously reconcile vectors |

Excel parsing uses the frontend's existing `exceljs` dependency. Every normalized request contains exactly one group with an internal `GROUP` key, the complete final example list, and explicit modified/deleted ID sets. Group codes remain protected display metadata and are never accepted as editable import data. Spring recomputes the diff and returns a conflict when client and database state disagree.

## 4. Authorization and Business Rules

### 4.1 Course ownership

- Controllers require `hasRole('TEACHER')`.
- QBank2 CRUD calls `ExamMatrixAccess.requireCourseTeacher(courseId)`.
- Group reads and writes include `course_id` in their SQL predicates.
- Example writes first load the parent group within the course.
- Manual regeneration verifies every requested example belongs to the same course.

### 4.2 Group rules

- `name` is required and must not be blank.
- `description` is required and must not be blank.
- Leading and trailing whitespace is removed before persistence.
- Codes are generated as `QBG-%03d`; values above 999 continue naturally, for example `QBG-1000`.
- Code sequences are allocated atomically per course through an upserted counter row.
- Existing codes and sequences are not reused after deletion.
- Group updates do not change the generated code or sequence.

### 4.3 Example rules

- `content` is required and must not be blank.
- Leading and trailing whitespace is removed before persistence.
- Sequence is `MAX(sequence) + 1` within the group.
- Existing sequences are not compacted or reordered after deletion.
- Examples are returned in sequence order, then ID order.

### 4.4 Manual Quiz 3 regeneration rules

- Available only while the assessment is `REVIEW_PENDING` at step 6.
- Available only for the `BASE` variant, shown to the user as Quiz 001.
- The question must belong to that assessment and variant.
- The original question must have exactly four answers.
- Zero or one QBank2 example may be supplied.
- A selected example must belong to the assessment's course.
- Only one active regeneration job may exist per question.
- AI output must contain nonblank question content, exactly four answers, and exactly one correct answer.
- Draft output uses `PLAIN_TEXT` content and explanation formats.
- The draft records the original question version.
- Saving uses the existing question optimistic lock. Provenance is applied only when the draft base version equals the expected saved-question version.

## 5. Persistence Model

### 5.1 Core QBank2 tables

```text
edu.courses
  1
  |
  +-- 0..1 edu.question_bank_2_code_counters
  |
  +-- * edu.question_bank_2_groups
          |
          +-- * edu.question_bank_2_examples
```

`edu.question_bank_2_code_counters`

- `course_id`: primary key and course foreign key with cascade delete.
- `next_value`: next positive group-code sequence.

`edu.question_bank_2_groups`

- UUID primary key.
- Course foreign key with cascade delete.
- Required `code`, `code_sequence`, `name`, and `description`.
- Created and updated timestamps.
- Unique `(course_id, code)` and `(course_id, code_sequence)` constraints.
- Index on `(course_id, code_sequence)`.

`edu.question_bank_2_examples`

- UUID primary key.
- Group foreign key with cascade delete.
- Required plain-text `content` and positive `sequence`.
- Created and updated timestamps.
- Unique `(group_id, sequence)` constraint.
- Index on `(group_id, sequence)`.

Deleting a course cascades to its QBank2 counter, groups, and examples. Deleting a group cascades to its relational examples; Spring separately removes the corresponding vectors.

### 5.2 Quiz 3 provenance fields

`edu.quiz3_question_provenance` stores:

- `qbank2_selection_mode`.
- `qbank2_content_group_ids` as JSONB.
- `qbank2_content_group_codes` as JSONB.
- `qbank2_example_ids` as JSONB.
- `qbank2_reference_snapshot` as JSONB.

The snapshot preserves the selected group/example details at generation time. Later edits or deletions in QBank2 therefore do not erase the historical generation context.

When shuffled variants are cloned from the base quiz, all QBank2 provenance columns are copied to the cloned questions.

### 5.3 Manual-regeneration tables

`edu.quiz3_qbank2_regeneration_jobs` stores request ownership, selected IDs, a complete request snapshot, base question version, job status/phase, lease, errors, and timestamps.

`edu.quiz3_qbank2_regeneration_drafts` stores one latest draft per question, including generated content, explanation, answers, selected group/example IDs, and reference snapshot.

A partial unique index on `question_id` for `QUEUED` and `RUNNING` jobs enforces one active operation per question.

## 6. Vector Synchronization and Automatic Selection

### 6.1 Write-time synchronization

Committed relational state and the QBank2 vector index are synchronized as follows:

| Relational operation | Vector operation |
| --- | --- |
| Create group | None; a group has no vector without examples |
| Update group | Synchronously upsert every example because group metadata is embedded/indexed |
| Delete group | Synchronously delete the captured example vectors |
| Create example | Synchronously index the new example |
| Update example | Delete the old vector and index the updated example |
| Delete example | Synchronously delete the captured example vector |

CRUD and Excel imports use `QBank2VectorSyncService` before their Spring transaction returns. Existing-group imports delete removed vectors, delete and reindex modified examples, index new examples, and skip unchanged examples. A vector failure propagates and rolls back PostgreSQL changes; startup bootstrap repairs rare partial external writes that PostgreSQL cannot roll back.

### 6.2 Startup/backfill synchronization

`QBank2VectorBootstrap` starts after a default five-second delay and retries every 30 seconds until one full pass succeeds. It reads every group/example row and upserts each example. A failed pass is logged and retried without preventing backend startup.

Once a pass succeeds, the component marks itself complete in memory and does not run another full pass until the application restarts.

### 6.3 Vector document and selection behavior

The AI service embeds:

```text
Content Group description: {group description}
Example: {example content}
```

It stores course, group, and example metadata in Qdrant. A deterministic UUIDv5 point ID is derived from the index profile and example ID. A source hash prevents unchanged examples from being re-embedded.

Automatic Quiz 3 selection:

1. Build `{custom learning level name} {learning-outcome code}`.
2. Embed the query.
3. Search the QBank2 collection with `top_k=1`, filtered by course ID.
4. If found, generate with the example style contract and record `AUTO_SELECTED` plus a snapshot and score.
5. If absent or selection throws an exception, use material RAG and record `RAG_FALLBACK`.

## 7. Manual Regeneration Flow

```text
Teacher opens question modal
        |
        +-- select zero or one course example
        |
POST qbank2-regenerate
        |
        +-- authorize course/review/base question
        +-- validate example ownership and original 4-answer MCQ
        +-- save immutable job request + base question version
        |
Scheduled dispatcher claims job
        |
POST AI /quiz3/questions/regenerate-with-qbank2
        |
        +-- no example: generate a fresh replacement from original question metadata
        +-- one example: remix using teacher-selected example as style contract
        |
Validate 4 answers / exactly 1 correct / nonblank question
        |
Save working draft -> frontend polls and loads it into local editor
        |
Teacher edits and presses Save question
        |
Normal optimistic-lock update + MANUAL_OVERRIDE provenance commit
```

With no selected example, the AI service checks whether the generated question and answers exactly duplicate the original after normalized comparison. It retries once before failing. With a selected example, the example controls question form, reasoning style, and distractor style while matrix metadata remains contextual guidance.

## 8. Error and Consistency Behavior

- Blank input returns `400 Bad Request`.
- Missing groups, examples, or regeneration jobs return `404 Not Found`.
- Invalid review stage, variant, or question association returns `409 Conflict`.
- A concurrent active regeneration request returns its existing job with `409 Conflict`.
- A stale question version causes the normal question update to return a conflict and prevents QBank2 provenance from being committed.
- AI response errors and contract violations mark the job `FAILED` with `QBANK2_GENERATION_FAILED`.
- Stored remote error messages are truncated to 1,000 characters.
- Running leases and old queued jobs are marked `TIMED_OUT` after three minutes.
- Discard marks active work `CANCELLED` and deletes the current draft.
- Closing the modal alone does not call discard; cancelling question editing does.

## 9. Current Seed Data

The backend currently contains 79 SQL files under `src/main/resources/question-bank-data`. Each file creates one QBank2 group, for a current total of:

- 79 content groups.
- 1,364 examples.

The files are organized under `Quiz` and `Exam` directories and contain explicit UUIDs, course IDs, group metadata, example content, and sequences. A mirrored deployment copy also exists under `configuration/backend_config/resources/question-bank-data` at the workspace level.

These are data resources rather than runtime Java classes. Their execution mechanism depends on the surrounding deployment/database initialization process; they are not referenced directly by `QuestionBank2Service`.

## 10. Code Map

### 10.1 Spring Boot backend

| File | Responsibility |
| --- | --- |
| `src/main/java/edu/ai/lms/core/course/adapter/in/QuestionBank2Controller.java` | Teacher-only REST endpoints for group/example CRUD |
| `src/main/java/edu/ai/lms/core/course/app/service/qbank2/QuestionBank2Service.java` | Authorization, validation, SQL CRUD, sequence/code generation, response records, and synchronous vector updates |
| `src/main/java/edu/ai/lms/core/course/adapter/in/QuestionBank2ImportController.java` | Teacher-only POST create and PUT replacement preview/commit endpoints |
| `src/main/java/edu/ai/lms/core/course/app/service/qbank2/QBank2ImportService.java` | Authoritative diff validation, direct-table transactional replacement, QBG allocation, and synchronous vector updates |
| `src/main/java/edu/ai/lms/core/course/app/service/qbank2/QBank2ImportModels.java` | Import request, issue, preview, mapping, summary, and status contracts |
| `src/main/java/edu/ai/lms/core/course/app/service/qbank2/QBank2VectorSyncService.java` | Synchronous Spring-to-AI vector index/delete client |
| `src/main/java/edu/ai/lms/core/course/app/service/qbank2/QBank2VectorBootstrap.java` | Scheduled initial/backfill vector indexing with retry |
| `src/main/java/edu/ai/lms/core/quiz3/Quiz3QBank2RegenerationService.java` | Manual job creation, validation, dispatch, AI call, working draft, provenance application, timeout/discard behavior |
| `src/main/java/edu/ai/lms/core/quiz3/Quiz3AssessmentController.java` | Exposes the four manual-regeneration endpoints |
| `src/main/java/edu/ai/lms/core/quiz3/Quiz3AssessmentService.java` | Persists automatic QBank2 provenance, returns metadata, applies manual provenance on save, clones provenance to variants |
| `src/main/java/edu/ai/lms/core/quiz3/Quiz3Models.java` | QBank2 request/job/draft records and question metadata/reference contracts |
| `src/main/java/edu/ai/lms/core/quiz3/Quiz3GenerationDispatcher.java` | Sends base Quiz 3 generation snapshots to the AI pipeline |
| `src/main/java/edu/ai/lms/core/quiz3/InternalQuiz3WebhookController.java` | Receives signed base-generation callbacks containing QBank2 references |
| `src/main/resources/db/manual/V20260817_01__qbank2.sql` | Core QBank2 tables, constraints, and indexes |
| `src/main/resources/db/manual/V20260817_02__quiz3_qbank2_auto_reference_snapshot.sql` | Quiz 3 provenance columns and regeneration job/draft tables |
| `src/main/resources/question-bank-data/**/*.questions.sql` | Course-specific QBank2 seed content |

Backend tests:

| File | Coverage |
| --- | --- |
| `src/test/java/edu/ai/lms/core/course/app/service/qbank2/QuestionBank2ServiceValidationUT.java` | Group-code formatting and blank-value validation |
| `src/test/java/edu/ai/lms/core/course/app/service/qbank2/QBank2ImportServiceValidationUT.java` | Import limits, ordering, identifiers, duplicate-content acceptance, and code-field rejection |
| `src/test/java/edu/ai/lms/core/quiz3/Quiz3QBank2RegenerationContractUT.java` | Zero-or-one example contract and duplicate/multiple rejection |

### 10.2 React frontend

| File | Responsibility |
| --- | --- |
| `src/routes/courses.$courseId.tsx` | Teacher-only course tab registration and panel mounting |
| `src/features/qbank2/QBank2Panel.tsx` | Group/example management plus template, export, and import toolbar actions |
| `src/features/qbank2/QBank2ImportDialog.tsx` | File selection, validation, preview, confirmation, result mapping, polling, and retry UI |
| `src/features/qbank2/qbank2-excel.ts` | ExcelJS template/export generation and protected workbook parsing |
| `src/features/qbank2/api/qbank2.contracts.ts` | TypeScript QBank2 CRUD and import contracts |
| `src/features/qbank2/api/qbank2.api.ts` | QBank2 CRUD plus POST-create and PUT-replacement import clients |
| `src/features/qbank2/hooks/use-qbank2.ts` | React Query read/mutation hooks and cache invalidation |
| `src/features/qbank2/qbank2-search.ts` | Accent-insensitive matching and ranking |
| `src/features/qbank2/index.ts` | QBank2 feature export |
| `src/features/quiz3/Quiz3Workspace.tsx` | Regeneration button/modal, polling, draft application, save/discard integration, provenance display |
| `src/features/quiz3/qbank2-example-selection.ts` | Zero-or-one checkbox selection helper |
| `src/features/quiz3/api/quiz3.api.ts` | Start/job/draft/discard API calls and `409` active-job handling |
| `src/features/quiz3/api/quiz3.contracts.ts` | QBank2 job, draft, status, and question-provenance types |

Frontend tests:

| File | Coverage |
| --- | --- |
| `src/features/qbank2/QBank2Panel.test.tsx` | New-group state and search-modal selection workflow |
| `src/features/qbank2/qbank2-excel.test.ts` | Template/export protection, round-trip parsing, and workbook validation |
| `src/features/qbank2/qbank2-search.test.ts` | Code search, accent-insensitive Vietnamese description search, and ranking |
| `src/features/qbank2/api/qbank2.api.test.ts` | All seven management endpoint paths and methods |
| `src/features/quiz3/qbank2-example-selection.test.tsx` | Zero-or-one example selection in helper and DOM interactions |

### 10.3 AI pipeline integration

| File | Responsibility |
| --- | --- |
| `src/api/lms_integration.py` | Internal index/delete/regenerate endpoints called by Spring |
| `src/lms/qbank2_vector_service.py` | Embedding payload, source hash, Qdrant upsert/delete, course-filtered top-one selection, reference snapshots |
| `src/pipeline/question_generation_pipeline.py` | Automatic QBank2-first Quiz 3 generation and material-RAG fallback |
| `src/lms/quiz3_qbank2_regeneration_service.py` | Manual zero-example replacement and one-example remix generation |
| `src/lms/schemas.py` | Strict vector and regeneration request/response validation |
| `src/lms/question_mapper.py` | Preserves `qbank2Reference` in generated question payloads |
| `src/llm/quiz_agent.py` | QBank2 remix/style prompt behavior |
| `src/config.py` | `QDRANT_QBANK2_COLLECTION` and `QBANK2_INDEX_PROFILE` configuration |
| `main.py` | Constructs and wires the vector and regeneration services |

Relevant AI tests:

- `tests/test_qbank2_vector_service.py`
- `tests/test_quiz3_qbank2_regeneration_service.py`
- `tests/test_quiz3_qbank2_regeneration_contract.py`
- QBank2 cases in `tests/test_generation_pipelines.py`

## 11. Known Scope and Implementation Notes

- QBank2 examples are plain text; there is no rich-text/image editor in the bank-management panel.
- There is no server-side group search or pagination; the frontend loads the entire course bank and filters locally.
- There is no example reordering or group reordering UI/API.
- There is no code editing or code reuse after deletion.
- There is no standalone endpoint for one group or one example; the list endpoint returns the complete nested bank.
- Automatic base generation selects top one by vector similarity without a configured minimum score in the current code.
- Manual regeneration accepts at most one example and performs no vector retrieval.
- Closing the manual modal does not cancel a running server job.
- The core backend test coverage is contract/validation focused; there is no QBank2 CRUD integration test in the current test tree.
- Vector synchronization is eventually consistent through durable PostgreSQL tasks. Relational writes do not depend on AI availability; failed tasks retain their errors and retry state, while startup bootstrap remains a backfill repair mechanism.

## 12. Quick Maintenance Guide

When changing a QBank2 payload, update the matching contracts in all participating layers:

1. Spring records in `QuestionBank2Service` or `Quiz3Models`.
2. Frontend contracts and API client.
3. AI Pydantic schemas for internal calls.
4. Persistence migration if provenance or draft fields change.
5. Contract tests in the backend, frontend, and AI pipeline.

When changing group metadata used for embeddings, also update `QBank2VectorSyncService`, `QBank2VectorExample`, and `QBank2Example.embedding_text`/`source_hash` so re-indexing behavior remains deterministic.
