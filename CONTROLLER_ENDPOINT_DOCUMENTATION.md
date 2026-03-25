# LMS Controller Endpoint Documentation

This document describes all controller endpoints in the project, including:
- What each endpoint does
- The related LMS business logic
- What each endpoint field means (path/query/body and response fields)

## 1) StudentAuthController
Base path: `/api/auth/student`

### 1.1 POST `/register`
- What the endpoint does:
  - Registers a new student account and returns authentication token data.
- LMS business logic:
  - Creates a student user profile in the authentication domain.
  - Enforces student identity and minimum password constraints before account creation.
  - Issues JWT token immediately after successful registration.

Request body (`StudentRegisterRequest`):
- `firstName` (String, required): Student given name.
- `lastName` (String, required): Student family name.
- `email` (String, required, email format): Login identifier for the student.
- `password` (String, required, min 8 chars): Credential used for authentication.
- `studentId` (String, required): Institution-specific student identifier.
- `enrollmentYear` (Long, required): Academic year the student enrolled.

Response body (`StudentRegisterResponse`):
- `token` (String): JWT access token.
- `type` (String): Token type (`Bearer`).
- `email` (String): Registered student email.

### 1.2 POST `/login`
- What the endpoint does:
  - Authenticates a student and returns JWT token information.
- LMS business logic:
  - Validates student credentials.
  - Loads student identity context for role-based access to LMS student flows.

Request body (`StudentSignInRequest`):
- `email` (String, required, email format): Student login email.
- `password` (String, required): Student login password.

Response body (`StudentSignInResponse`):
- `token` (String): JWT access token.
- `type` (String): Token type (`Bearer`).
- `email` (String): Student email.
- `firstName` (String): Student first name.
- `lastName` (String): Student last name.
- `role` (String): Auth role (`STUDENT`).

## 2) TeacherAuthController
Base path: `/api/auth/teacher`

### 2.1 POST `/register`
- What the endpoint does:
  - Registers a new teacher account and returns authentication token data.
- LMS business logic:
  - Creates teacher identity/profile used as course ownership principal.
  - Captures teacher employment metadata used for institutional tracking.
  - Issues JWT token for immediate authorized access.

Request body (`TeacherRegisterRequest`):
- `firstName` (String, required): Teacher given name.
- `lastName` (String, required): Teacher family name.
- `email` (String, required, email format): Teacher login email.
- `password` (String, required): Teacher login password.
- `employeeId` (String, required): Institution employee identifier.
- `phoneNumber` (String, optional): Teacher contact number.
- `department` (String, optional): Academic/organizational department.
- `specialization` (String, optional): Subject specialization.

Response body (`TeacherRegisterResponse`):
- `token` (String): JWT access token.
- `type` (String): Token type (`Bearer`).
- `email` (String): Registered teacher email.

### 2.2 POST `/login`
- What the endpoint does:
  - Authenticates a teacher and returns JWT token information.
- LMS business logic:
  - Validates teacher credentials.
  - Activates teacher role context used for privileged operations (course editing, enrollment management, content creation).

Request body (`TeacherSignInRequest`):
- `email` (String, required, email format): Teacher login email.
- `password` (String, required): Teacher login password.

Response body (`TeacherSignInResponse`):
- `token` (String): JWT access token.
- `type` (String): Token type (`Bearer`).
- `email` (String): Teacher email.
- `firstName` (String): Teacher first name.
- `lastName` (String): Teacher last name.
- `role` (String): Auth role (`TEACHER`).

## 3) AttemptController
Base path: `/api/{courseId}/attempt`
Role guard: `STUDENT`

### 3.1 POST `/quiz/{quizId}/start`
- What the endpoint does:
  - Starts a quiz attempt for the authenticated student.
- LMS business logic:
  - Creates an attempt lifecycle record with `STARTED` status.
  - Uses quiz timing policy (due time, limits) to initialize attempt state.

Path fields:
- `courseId` (UUID): Course context containing the quiz.
- `quizId` (UUID): Quiz to attempt.

Response body (`StartAttemptResponse`):
- `attemptId` (UUID): Created attempt identifier.
- `quizId` (UUID): Quiz linked to the attempt.
- `status` (String): Attempt state (started lifecycle state).
- `startTime` (LocalDateTime): Attempt start timestamp.
- `dueTime` (LocalDateTime): Effective submission deadline for this attempt.

### 3.2 POST `/quiz/{quizId}/submit`
- What the endpoint does:
  - Submits answers for an active attempt.
- LMS business logic:
  - Validates that path `quizId` matches body `quizId`.
  - Evaluates submission against quiz/question definitions.
  - Finalizes attempt with score, status, and submission metadata.

Path fields:
- `courseId` (UUID): Course context.
- `quizId` (UUID): Quiz being submitted.

Request body (`SubmitAttemptRequest`):
- `quizId` (UUID, required): Must match URL quiz ID.
- `submittedAnswers` (List, optional/validated): List of student answers.
  - `questionId` (UUID, required): Question being answered.
  - `studentAnswerId` (Long, required): Selected answer number/ID for that question.
- `timeSpentSeconds` (Integer, optional, min 0): Time consumed by student attempt.

Response body (`SubmitAttemptResponse`):
- `attemptId` (UUID): Submitted attempt ID.
- `quizId` (UUID): Submitted quiz ID.
- `status` (String): Final attempt status.
- `score` (Double): Computed grade/score.
- `timeSpentSeconds` (Integer): Stored attempt duration.
- `submittedTime` (LocalDateTime): Submission timestamp.
- `dueTime` (LocalDateTime): Deadline used for evaluation.
- `errorReason` (String): Late/error reason if applicable.

### 3.3 GET `/quiz/{quizId}/me`
- What the endpoint does:
  - Retrieves all attempts of the authenticated student for a specific quiz.
- LMS business logic:
  - Returns student-specific attempt history for progress/retake visibility.

Path fields:
- `courseId` (UUID): Course context.
- `quizId` (UUID): Quiz for history retrieval.

Response body (`GetMyAttemptsResponse`):
- `quizId` (UUID): Quiz context.
- `attempts` (List): Student attempts summary.
  - `attemptId` (UUID): Attempt identifier.
  - `status` (String): Attempt status.
  - `score` (Double): Attempt score.
  - `startTime` (LocalDateTime): Attempt start.
  - `dueTime` (LocalDateTime): Attempt deadline.
  - `submittedTime` (LocalDateTime): Submission time.
  - `timeSpentSeconds` (Integer): Duration used.
  - `errorReason` (String): Error/late reason if any.

## 4) CourseController
Base path: `/api/course`
Role guard:
- Read: `TEACHER` or `STUDENT`
- Write/Delete: `TEACHER`

### 4.1 GET `/{courseId}`
- What the endpoint does:
  - Fetches a specific course.
- LMS business logic:
  - Exposes a course aggregate view including grouped/ungrouped learning outcomes.

Path fields:
- `courseId` (UUID): Course identifier.

Response body (`GetCourseResponse`):
- `courseId`, `courseName`, `courseCode`, `description`, `teacherId`, `active`, `coverImage`
- `startDate`, `endDate`
- `groupLearningOutcomes` (List): Each group has `id`, `name`, `description`, `learningOutcomes`.
- `ungroupedLearningOutcomes` (List): Each item has `id`, `code`, `description`.

### 4.2 GET `/`
- What the endpoint does:
  - Lists courses with pagination/sorting controls.
- LMS business logic:
  - Returns visible course summaries for user context.

Query fields:
- `pageSize` (int, default `5`): Max courses returned.
- `sortDirection` (String, default `asc`): Name sort direction (`asc`/`desc`).

Response body (`GetAllCoursesResponse`):
- `courses` (List): Course summaries (`courseId`, `courseName`, `courseCode`, `description`, `active`, `coverImage`, `startDate`, `endDate`).
- `totalCourses` (int): Count in this response context.
- `pageSize` (int): Applied page size.
- `sortDirection` (String): Applied sorting direction.

### 4.3 POST `/`
- What the endpoint does:
  - Creates a new course.
- LMS business logic:
  - Teacher becomes course owner.
  - Course date range must be valid.
  - Optional initial learning outcomes can be seeded during creation.

Request body (`CreateCourseRequest`):
- `courseName` (String, required): Course display title.
- `description` (String, optional): Course description.
- `courseCode` (String, required): Institutional code.
- `active` (Boolean, default `true`): Course availability state.
- `coverImage` (String, optional): Course image reference.
- `startDate` (LocalDate, required): Course start date.
- `endDate` (LocalDate, required): Course end date; must be after start.
- `learningOutcomes` (List, optional): Initial CLOs.
  - `code` (String, required): Outcome code.
  - `description` (String, required): Outcome description.

Response body (`CreateCourseResponse`):
- `courseId`, `courseName`, `courseCode`, `description`, `teacherId`, `active`, `coverImage`, `startDate`, `endDate`

### 4.4 PUT `/{courseId}`
- What the endpoint does:
  - Updates an existing course.
- LMS business logic:
  - Only course owner teacher can modify.
  - Date range remains validated.

Path fields:
- `courseId` (UUID): Target course.

Request body (`ModifyCourseRequest`):
- `courseName` (String, required)
- `description` (String, optional)
- `courseCode` (String, required)
- `active` (Boolean, default `true`)
- `coverImage` (String, optional)
- `startDate` (LocalDate, required)
- `endDate` (LocalDate, required; after start)

Response body (`ModifyCourseResponse`):
- `courseId`, `courseName`, `courseCode`, `description`, `teacherId`, `active`, `coverImage`, `startDate`, `endDate`, `updatedAt`

### 4.5 DELETE `/{courseId}`
- What the endpoint does:
  - Deletes a course.
- LMS business logic:
  - Only owner teacher can remove course aggregate and dependent associations.

Path fields:
- `courseId` (UUID): Course to delete.

Response body (`DeleteCourseResponse`):
- `courseId` (UUID): Deleted course ID.
- `message` (String): Deletion result message.
- `success` (Boolean): Deletion status.

## 5) ChapterController
Base path: `/api/{courseId}/chapter`
Role guard:
- Read: `TEACHER` or `STUDENT`
- Write/Delete: `TEACHER`

### 5.1 GET `/`
- What the endpoint does:
  - Lists all chapters in a course.
- LMS business logic:
  - Retrieves chapter-level structure for course navigation/order.

Path fields:
- `courseId` (UUID): Parent course.

Response body (`List<GetChapterResponse>`): each item contains:
- `chapterId`, `title`, `description`, `courseId`
- `learningOutcomeIds`
- `ungroupedLearningOutcomes` (id, code, description)
- `groupedLearningOutcomes` (id, name, description, learningOutcomes)
- `createdAt`, `updatedAt`

### 5.2 GET `/{chapterId}`
- What the endpoint does:
  - Gets one chapter by course and chapter IDs.
- LMS business logic:
  - Ensures chapter belongs to the course context.

Path fields:
- `courseId` (UUID): Course context.
- `chapterId` (UUID): Chapter identifier.

Response body (`GetChapterResponse`): same structure as above.

### 5.3 POST `/`
- What the endpoint does:
  - Creates a chapter in a course.
- LMS business logic:
  - Only course owner teacher can append chapter content units.

Path fields:
- `courseId` (UUID): Course receiving the chapter.

Request body (`CreateChapterRequest`):
- `title` (String, required): Chapter title.
- `description` (String, optional): Chapter details.

Response body (`CreateChapterResponse`):
- `chapterId`, `title`, `description`, `courseId`, `createdAt`

### 5.4 PUT `/{chapterId}`
- What the endpoint does:
  - Updates chapter metadata.
- LMS business logic:
  - Owner teacher edits chapter content structure.

Path fields:
- `courseId` (UUID)
- `chapterId` (UUID)

Request body (`ModifyChapterRequest`):
- `title` (String, required): New chapter title.
- `description` (String, optional): New chapter description.

Response body (`ModifyChapterResponse`):
- `chapterId`, `title`, `description`, `courseId`, `updatedAt`

### 5.5 DELETE `/{chapterId}`
- What the endpoint does:
  - Deletes a chapter from a course.
- LMS business logic:
  - Owner teacher removes chapter from course structure.

Path fields:
- `courseId` (UUID)
- `chapterId` (UUID)

Response body (`DeleteChapterResponse`):
- `courseId`, `chapterId`, `message`, `success`

## 6) CourseEnrollmentController
Base path: `/api/course`
Role guard: `TEACHER`

### 6.1 GET `/{courseId}/enrollments`
- What the endpoint does:
  - Gets all teacher/student participants in a course.
- LMS business logic:
  - Owner teacher views enrollment rosters for management.

Path fields:
- `courseId` (UUID): Course target.

Response body (`GetCourseEnrollmentsResponse`):
- `courseId` (UUID)
- `teachers` (List<UUID>): Teacher IDs participating.
- `students` (List<UUID>): Student IDs participating.

### 6.2 PUT `/{courseId}/students/add`
- What the endpoint does:
  - Adds students to course by email list.
- LMS business logic:
  - Owner teacher enrolls existing student accounts into course.

Path fields:
- `courseId` (UUID): Course target.

Request body (`AddStudentsToCourseRequest`):
- `studentEmails` (List<String>, required non-empty): Student emails to enroll.

Response body (`AddStudentsToCourseResponse`):
- `courseId`, `addedStudentEmails`, `totalAdded`, `message`

### 6.3 PUT `/{courseId}/students/remove`
- What the endpoint does:
  - Removes students from course by email list.
- LMS business logic:
  - Owner teacher unenrolls students while preserving course ownership controls.

Path fields:
- `courseId` (UUID): Course target.

Request body (`RemoveStudentsFromCourseRequest`):
- `studentEmails` (List<String>, required non-empty): Student emails to remove.

Response body (`RemoveStudentsFromCourseResponse`):
- `courseId`, `removedStudentEmails`, `totalRemoved`, `message`

## 7) LearningOutcomeController
No class-level base path. Endpoints use full paths.
Role guard:
- Read: `TEACHER` or `STUDENT`
- Write/Delete: `TEACHER`

### 7.1 POST `/api/{courseId}/learning-outcome`
- What the endpoint does:
  - Creates one or more learning outcomes in a course.
- LMS business logic:
  - Teacher defines CLOs for course-level assessment mapping.

Path fields:
- `courseId` (UUID)

Request body (`CreateLearningOutcomesRequest`):
- `learningOutcomes` (List, required non-empty): CLOs to create.
  - `code` (String, required): CLO identifier (e.g., CLO-1).
  - `description` (String, required): CLO learning target statement.

Response body (`CreateLearningOutcomesResponse`):
- `courseId`
- `learningOutcomes` (id, code, description, createdAt)
- `totalCreated`

### 7.2 GET `/api/{courseId}/learning-outcome/{learningOutcomeId}`
- What the endpoint does:
  - Retrieves one learning outcome.
- LMS business logic:
  - Used in CLO details and mapping flows.

Path fields:
- `courseId` (UUID)
- `learningOutcomeId` (UUID)

Response body (`GetLearningOutcomeResponse`):
- `id`, `code`, `description`, `courseId`, `groupLearningOutcomeId`, `createdAt`, `updatedAt`

### 7.3 GET `/api/{courseId}/learning-outcome`
- What the endpoint does:
  - Retrieves selected learning outcomes by IDs.
- LMS business logic:
  - Batch lookup for mapping endpoints (quiz/question/chapter/group operations).

Path fields:
- `courseId` (UUID)

Query fields:
- `ids` (List<UUID>): Learning outcome IDs to fetch.

Response body (`List<GetLearningOutcomeResponse>`): same item structure as above.

### 7.4 GET `/api/{courseId}/learning-outcome/grouped`
- What the endpoint does:
  - Retrieves grouped learning outcomes and their children.
- LMS business logic:
  - Supports curriculum organization and group-based mapping.

Path fields:
- `courseId` (UUID)

Response body (`List<GetGroupLearningOutcomeResponse>`):
- `id`, `name`, `description`
- `learningOutcomes` (List of `GetLearningOutcomeResponse`)

### 7.5 PUT `/api/{courseId}/learning-outcome/{learningOutcomeId}`
- What the endpoint does:
  - Updates a single learning outcome.
- LMS business logic:
  - Maintains CLO quality and revisions over course lifecycle.

Path fields:
- `courseId` (UUID)
- `learningOutcomeId` (UUID)

Request body (`ModifyLearningOutcomeRequest`):
- `code` (String, optional): New CLO code.
- `description` (String, optional): New CLO description.

Response body (`ModifyLearningOutcomeResponse`):
- `id`, `code`, `description`, `courseId`, `updatedAt`

### 7.6 PUT `/api/{courseId}/learning-outcome`
- What the endpoint does:
  - Batch updates multiple learning outcomes.
- LMS business logic:
  - Bulk CLO maintenance for syllabus revisions.

Path fields:
- `courseId` (UUID)

Request body (`ModifyLearningOutcomesRequest`):
- `learningOutcomes` (List, required non-empty)
  - `id` (UUID, required): Target learning outcome ID.
  - `code` (String, optional): Updated code.
  - `description` (String, optional): Updated description.

Response body (`ModifyLearningOutcomesResponse`):
- `courseId`
- `learningOutcomes` (id, code, description, updatedAt)
- `totalModified`

### 7.7 DELETE `/api/{courseId}/learning-outcome`
- What the endpoint does:
  - Deletes multiple learning outcomes by ID.
- LMS business logic:
  - Removes outdated CLO definitions in batch.

Path fields:
- `courseId` (UUID)

Request body (`DeleteLearningOutcomesRequest`):
- `learningOutcomeIds` (List<UUID>, required non-empty): CLO IDs to delete.

Response body (`DeleteLearningOutcomesResponse`):
- `courseId`, `deletedIds`, `totalDeleted`, `message`

### 7.8 PUT `/api/{courseId}/learning-outcome/add`
- What the endpoint does:
  - Alias-style endpoint to add learning outcomes to course (same create behavior).
- LMS business logic:
  - Supports explicit add action semantic for clients.

Path fields:
- `courseId` (UUID)

Request body (`CreateLearningOutcomesRequest`): same fields as 7.1.

Response body (`CreateLearningOutcomesResponse`): same fields as 7.1.

### 7.9 PUT `/api/{courseId}/learning-outcome/remove`
- What the endpoint does:
  - Alias-style endpoint to remove learning outcomes (same delete behavior).
- LMS business logic:
  - Supports explicit remove action semantic for clients.

Path fields:
- `courseId` (UUID)

Request body (`DeleteLearningOutcomesRequest`): same fields as 7.7.

Response body (`DeleteLearningOutcomesResponse`): same fields as 7.7.

### 7.10 PUT `/api/{courseId}/chapter/{chapterId}/learning-outcomes/add`
- What the endpoint does:
  - Adds course learning outcomes to a specific chapter.
- LMS business logic:
  - Maps chapter content to CLOs for outcome-based learning and assessment traceability.

Path fields:
- `courseId` (UUID)
- `chapterId` (UUID)

Request body (`AddLearningOutcomesToChapterRequest`):
- `learningOutcomeIds` (List<UUID>, required non-empty): CLO IDs to map to chapter.

Response body (`AddLearningOutcomesToChapterResponse`):
- `courseId`, `chapterId`, `addedLearningOutcomeIds`, `totalAdded`, `message`

### 7.11 PUT `/api/{courseId}/chapter/{chapterId}/learning-outcomes/remove`
- What the endpoint does:
  - Removes learning outcome links from a chapter.
- LMS business logic:
  - Unmaps chapter from specific CLOs when curriculum changes.

Path fields:
- `courseId` (UUID)
- `chapterId` (UUID)

Request body (`RemoveLearningOutcomesFromChapterRequest`):
- `learningOutcomeIds` (List<UUID>, required non-empty): CLO IDs to unmap.

Response body (`RemoveLearningOutcomesFromChapterResponse`):
- `courseId`, `chapterId`, `removedLearningOutcomeIds`, `totalRemoved`, `message`

## 8) GroupLearningOutcomeController
No class-level base path. Endpoints use full paths.
Role guard: `TEACHER`

### 8.1 POST `/api/{courseId}/group-learning-outcome`
- What the endpoint does:
  - Creates a group learning outcome and assigns existing learning outcomes.
- LMS business logic:
  - Builds hierarchical outcome groups for curriculum organization.
  - Prevents assigning an outcome to conflicting groups.

Path fields:
- `courseId` (UUID)

Request body (`CreateGroupLearningOutcomeRequest`):
- `name` (String, required): Group name.
- `description` (String, optional): Group description.
- `learningOutcomeIds` (List<UUID>, required non-empty): CLOs to include.

Response body (`CreateGroupLearningOutcomeResponse`):
- `id`, `name`, `description`, `courseId`, `learningOutcomes`, `createdAt`
- each `learningOutcomes` item: `id`, `code`, `description`

### 8.2 PUT `/api/{courseId}/group-learning-outcome/{groupId}/add-learning-outcomes`
- What the endpoint does:
  - Adds outcomes to an existing group.
- LMS business logic:
  - Maintains grouped CLO taxonomy over time.

Path fields:
- `courseId` (UUID)
- `groupId` (UUID)

Request body (`AddLearningOutcomesToGroupRequest`):
- `learningOutcomeIds` (List<UUID>, required non-empty): CLO IDs to add.

Response body (`AddLearningOutcomesToGroupResponse`):
- `groupId`, `name`, `courseId`, `learningOutcomes`, `totalAdded`
- each `learningOutcomes` item: `id`, `code`, `description`

### 8.3 PUT `/api/{courseId}/group-learning-outcome/{groupId}/remove-learning-outcomes`
- What the endpoint does:
  - Removes outcomes from a group (outcomes become ungrouped).
- LMS business logic:
  - Supports reorganizing CLO hierarchy without deleting CLO definitions.

Path fields:
- `courseId` (UUID)
- `groupId` (UUID)

Request body (`RemoveLearningOutcomesFromGroupRequest`):
- `learningOutcomeIds` (List<UUID>, required non-empty): CLO IDs to remove.

Response body (`RemoveLearningOutcomesFromGroupResponse`):
- `groupId`, `name`, `courseId`, `totalRemoved`, `message`

## 9) MaterialController
Base path: `/api/{courseId}/material`
Role guard:
- Read: `TEACHER` or `STUDENT`
- Write/Delete/confirm: `TEACHER`

### 9.1 GET `/`
- What the endpoint does:
  - Lists uploaded materials for a course.
- LMS business logic:
  - Returns only materials with uploaded/ready state for consumption.

Path fields:
- `courseId` (UUID)

Response body (`List<GetMaterialResponse>`): each item contains:
- `id`, `title`, `type`, `fileKey`, `isDownloadable`, `status`, `presignedUrl`, `courseId`, `createdAt`, `updatedAt`

### 9.2 GET `/{materialId}`
- What the endpoint does:
  - Retrieves one material (including access URL information).
- LMS business logic:
  - Provides secure retrieval metadata for course content.

Path fields:
- `courseId` (UUID)
- `materialId` (UUID)

Response body (`GetMaterialResponse`):
- `id`, `title`, `type`, `fileKey`, `isDownloadable`, `status`, `presignedUrl`, `courseId`, `createdAt`, `updatedAt`

### 9.3 POST `/`
- What the endpoint does:
  - Creates a general (course-level) material upload record.
- LMS business logic:
  - Generates storage file metadata and presigned upload URL for frontend direct upload.

Path fields:
- `courseId` (UUID)

Request body (`CreateMaterialRequest`):
- `title` (String, required): Material display title.
- `type` (Enum, required): Material type (`PDF`, `VIDEO`, `LINK`).
- `isDownloadable` (Boolean, optional): Whether learner download is allowed.

Response body (`CreateMaterialResponse`):
- `id`, `title`, `type`, `fileKey`, `isDownloadable`, `status`, `presignedUploadUrl`, `courseId`, `createdAt`

### 9.4 GET `/chapter/{chapterId}`
- What the endpoint does:
  - Lists materials associated with one chapter.
- LMS business logic:
  - Chapter-scoped material retrieval for structured lesson navigation.

Path fields:
- `courseId` (UUID)
- `chapterId` (UUID)

Response body (`List<GetMaterialResponse>`): same item structure as 9.1.

### 9.5 POST `/chapter/{chapterId}`
- What the endpoint does:
  - Creates a chapter-scoped material upload record.
- LMS business logic:
  - Associates newly created material with specific chapter content.

Path fields:
- `courseId` (UUID)
- `chapterId` (UUID)

Request body (`CreateMaterialRequest`): same fields as 9.3.

Response body (`CreateMaterialResponse`): same fields as 9.3.

### 9.6 PUT `/{materialId}`
- What the endpoint does:
  - Updates material metadata.
- LMS business logic:
  - Teacher can adjust title/type/downloadability while preserving file tracking.

Path fields:
- `courseId` (UUID)
- `materialId` (UUID)

Request body (`ModifyMaterialRequest`):
- `title` (String, optional): Updated title.
- `type` (String, optional): Updated type label (`PDF`, `VIDEO`, `LINK`).
- `isDownloadable` (Boolean, optional): Updated download policy.

Response body (`ModifyMaterialResponse`):
- `id`, `title`, `type`, `fileKey`, `isDownloadable`, `status`, `presignedUploadUrl`, `courseId`, `updatedAt`

### 9.7 DELETE `/{materialId}`
- What the endpoint does:
  - Deletes material.
- LMS business logic:
  - Removes material from course and related storage references.

Path fields:
- `courseId` (UUID)
- `materialId` (UUID)

Response body (`DeleteMaterialResponse`):
- `courseId`, `materialId`, `message`, `success`

### 9.8 PATCH `/{materialId}/confirm-upload`
- What the endpoint does:
  - Confirms if storage upload succeeded and updates material status.
- LMS business logic:
  - Finalizes material lifecycle from pending to uploaded/failed based on actual object existence.

Path fields:
- `courseId` (UUID)
- `materialId` (UUID)

Response body (`ConfirmUploadResponse`):
- `materialId`, `courseId`, `fileKey`, `status`, `uploaded`, `message`

## 10) QuizController
Base path: `/api/{courseId}/quiz`
Role guard:
- Mixed: some endpoints `TEACHER`+`STUDENT`, student-created list restricted to `STUDENT`

### 10.1 GET `/teacher-created`
- What the endpoint does:
  - Lists teacher-created quizzes in a course.
- LMS business logic:
  - Exposes published/visible teacher quizzes for course participants.

Path fields:
- `courseId` (UUID)

Query fields:
- `pageSize` (int, default `10`): Max quizzes returned.
- `lastCreated` (String, default `asc`): Creation ordering hint used by use case.

Response body (`GetAllQuizzesResponse`):
- `quizzes` (List): each summary has `quizId`, `quizTitle`, `unlimited`, `timeLimitMinutes`, `passScore`, `startTime`, `dueTime`, `attemptLimited`, `display`, `createdAt`
- `totalQuizzes`, `pageSize`, `lastCreated`

### 10.2 GET `/student-created`
- What the endpoint does:
  - Lists quizzes created by the authenticated student.
- LMS business logic:
  - Student can manage/view own authored quizzes only.

Path/query fields:
- same as 10.1

Response body:
- same as 10.1 (`GetAllQuizzesResponse`)

### 10.3 GET `/{quizId}`
- What the endpoint does:
  - Retrieves full quiz details, including outcomes, groups, questions, and answers.
- LMS business logic:
  - Primary quiz detail view for taking/editing logic.

Path fields:
- `courseId` (UUID)
- `quizId` (UUID)

Response body (`GetQuizResponse`):
- Quiz core: `quizId`, `quizTitle`, `unlimited`, `timeLimitMinutes`, `passScore`, `startTime`, `dueTime`, `attemptLimited`, `courseId`, `display`, `createdAt`, `updatedAt`
- CLO mapping:
  - `ungroupedLearningOutcomes` (id, code, description)
  - `groupedLearningOutcomes` (id, name, description, learningOutcomes)
- Question set:
  - `questions` each with `questionId`, `content`, `type`, `points`, `answers`
  - `answers` each with `answerNumber`, `content`, `isCorrect`, `explanation`

### 10.4 POST `/`
- What the endpoint does:
  - Creates a quiz inside a course.
- LMS business logic:
  - Supports configurable timing, pass threshold, attempt limits, and visibility policy.

Path fields:
- `courseId` (UUID)

Request body (`CreateQuizRequest`):
- `quizTitle` (String, required): Quiz title.
- `unlimited` (Boolean, optional): Unlimited time flag.
- `timeLimitMinutes` (Integer, optional): Time limit if not unlimited.
- `passScore` (Double, optional): Passing threshold.
- `startTime` (LocalDateTime, optional): Quiz opening time.
- `dueTime` (LocalDateTime, optional): Quiz closing/deadline.
- `attemptLimited` (Integer, optional): Max attempts allowed.
- `display` (Boolean, optional): Visibility/publishing flag.

Response body (`CreateQuizResponse`):
- `quizId`, `quizTitle`, `unlimited`, `timeLimitMinutes`, `passScore`, `startTime`, `dueTime`, `attemptLimited`, `courseId`, `display`, `createdAt`

### 10.5 PUT `/{quizId}`
- What the endpoint does:
  - Updates quiz settings.
- LMS business logic:
  - Creator can revise operational quiz policy and schedule.

Path fields:
- `courseId` (UUID)
- `quizId` (UUID)

Request body (`ModifyQuizRequest`):
- `quizTitle`, `unlimited`, `timeLimitMinutes`, `passScore`, `startTime`, `dueTime`, `attemptLimited`, `display` (all optional updates)

Response body (`ModifyQuizResponse`):
- `quizId`, `quizTitle`, `unlimited`, `timeLimitMinutes`, `passScore`, `startTime`, `dueTime`, `attemptLimited`, `courseId`, `display`, `updatedAt`

### 10.6 DELETE `/{quizId}`
- What the endpoint does:
  - Deletes one quiz.
- LMS business logic:
  - Removes quiz and linked entities under creator authorization.

Path fields:
- `courseId` (UUID)
- `quizId` (UUID)

Response body (`DeleteQuizResponse`):
- `quizId`, `message`

### 10.7 DELETE `/`
- What the endpoint does:
  - Deletes multiple quizzes.
- LMS business logic:
  - Batch quiz cleanup operation.

Path fields:
- `courseId` (UUID)

Request body (`DeleteQuizzesRequest`):
- `quizIds` (List<UUID>, required non-empty): Quiz IDs to delete.

Response body (`List<DeleteQuizResponse>`): each item has `quizId`, `message`

### 10.8 POST `/{quizId}/learning-outcomes`
- What the endpoint does:
  - Adds learning outcomes to quiz (direct IDs and/or group IDs).
- LMS business logic:
  - Expands group IDs to child outcomes and deduplicates final mapping set.

Path fields:
- `courseId` (UUID)
- `quizId` (UUID)

Request body (`QuizLearningOutcomeOperationRequest`):
- `learningOutcomeIds` (List<UUID>, optional): Direct CLO IDs.
- `groupLearningOutcomeIds` (List<UUID>, optional): Group IDs to expand into CLO IDs.

Response body (`QuizLearningOutcomeOperationResponse`):
- `quizId`, `courseId`, `operation`, `totalAffected`, `learningOutcomeIds`

### 10.9 DELETE `/{quizId}/learning-outcomes`
- What the endpoint does:
  - Removes learning outcomes from quiz (direct IDs and/or group IDs).
- LMS business logic:
  - Same expansion/dedup strategy used for removal.

Path fields:
- `courseId` (UUID)
- `quizId` (UUID)

Request/response fields:
- same structures as 10.8.

## 11) QuestionController
Base path: `/api/quiz/{quizId}/question`
Role guard: `TEACHER` or `STUDENT`

### 11.1 GET `/{questionId}/learning-outcomes`
- What the endpoint does:
  - Gets learning outcomes mapped to one question.
- LMS business logic:
  - Question-level CLO tracing for fine-grained assessment alignment.

Path fields:
- `quizId` (UUID)
- `questionId` (UUID)

Response body (`GetQuestionLearningOutcomesResponse`):
- `questionId`, `quizId`, `courseId`
- `learningOutcomes` (List of `id`, `code`, `description`)

### 11.2 POST `/{questionId}/learning-outcomes`
- What the endpoint does:
  - Adds learning outcomes to a question.
- LMS business logic:
  - Ensures question assessment objectives align with course outcomes.

Path fields:
- `quizId` (UUID)
- `questionId` (UUID)

Request body (`QuestionLearningOutcomeOperationRequest`):
- `learningOutcomeIds` (List<UUID>, required not null): CLO IDs to add.

Response body (`QuestionLearningOutcomeOperationResponse`):
- `questionId`, `quizId`, `courseId`, `operation`, `totalAffected`, `learningOutcomeIds`

### 11.3 DELETE `/{questionId}/learning-outcomes`
- What the endpoint does:
  - Removes learning outcomes from a question.
- LMS business logic:
  - Adjusts question-to-outcome mapping when content changes.

Path fields:
- `quizId` (UUID)
- `questionId` (UUID)

Request/response fields:
- same structures as 11.2.

### 11.4 GET `/{questionId}`
- What the endpoint does:
  - Gets a question and its answers.
- LMS business logic:
  - Used in quiz rendering and editing.

Path fields:
- `quizId` (UUID)
- `questionId` (UUID)

Response body (`GetQuestionResponse`):
- `questionId`, `content`, `type`, `points`, `quizId`, `createdAt`, `updatedAt`
- `answers` list with `answerNumber`, `content`, `isCorrect`, `explanation`

### 11.5 POST `/`
- What the endpoint does:
  - Creates one or more questions in a quiz.
- LMS business logic:
  - Supports batch question authoring with optional answers in each question payload.

Path fields:
- `quizId` (UUID)

Request body (`CreateQuestionRequest`):
- `questions` (List, required non-empty)
  - `content` (String, required): Question text.
  - `type` (String, optional): Question type.
  - `points` (Double, optional): Question score weight.
  - `answers` (List<CreateAnswerRequest>, optional): Initial answers.
    - `content` (String, required): Answer text.
    - `isCorrect` (boolean): Correctness flag.
    - `explanation` (String, optional): Rationale/feedback.

Response body (`List<CreateQuestionResponse>`): each item has:
- `questionId`, `content`, `type`, `points`, `quizId`, `createdAt`
- `answers` (`answerNumber`, `content`, `isCorrect`, `explanation`)

### 11.6 PUT `/`
- What the endpoint does:
  - Updates one or more questions in a quiz.
- LMS business logic:
  - Batch revision of question text/type/points/answer set.

Path fields:
- `quizId` (UUID)

Request body (`ModifyQuestionRequest`):
- `questions` (List, required non-empty)
  - `questionId` (UUID, required): Target question.
  - `content` (String, required): Updated content.
  - `type` (String, optional): Updated type.
  - `points` (Double, optional): Updated points.
  - `answers` (List<CreateAnswerRequest>, optional): Updated answer payload.

Response body (`List<ModifyQuestionResponse>`): each item has:
- `questionId`, `content`, `type`, `points`, `quizId`, `updatedAt`
- `answers` (`answerNumber`, `content`, `isCorrect`, `explanation`)

### 11.7 DELETE `/{questionId}`
- What the endpoint does:
  - Deletes one question.
- LMS business logic:
  - Removes target question from quiz composition.

Path fields:
- `quizId` (UUID)
- `questionId` (UUID)

Response body (`DeleteQuestionResponse`):
- `questionId`, `message`

### 11.8 DELETE `/`
- What the endpoint does:
  - Deletes multiple questions.
- LMS business logic:
  - Batch cleanup of quiz question set.

Path fields:
- `quizId` (UUID)

Request body (`DeleteQuestionsRequest`):
- `questionIds` (List<UUID>, required non-empty): Questions to delete.

Response body (`List<DeleteQuestionResponse>`): each item has `questionId`, `message`

## 12) AnswerController
Base path: `/api/quiz/{questionId}/answer`
Role guard: `TEACHER` or `STUDENT`

### 12.1 POST `/`
- What the endpoint does:
  - Creates an answer for a question.
- LMS business logic:
  - Adds selectable response option for quiz question evaluation.

Path fields:
- `questionId` (UUID)

Request body (`CreateAnswerRequest`):
- `content` (String, required): Answer text.
- `isCorrect` (boolean): Whether this option is correct.
- `explanation` (String, optional): Feedback text for review.

Response body (`CreateAnswerResponse`):
- `answerNumber`, `content`, `isCorrect`, `explanation`, `questionId`, `createdAt`

### 12.2 PUT `/{answerNumber}`
- What the endpoint does:
  - Updates a specific answer option.
- LMS business logic:
  - Maintains answer correctness/content quality after question revisions.

Path fields:
- `questionId` (UUID)
- `answerNumber` (Long): Answer sequence/identifier inside question context.

Request body (`ModifyAnswerRequest`):
- `content` (String, optional): Updated answer text.
- `isCorrect` (Boolean, optional): Updated correctness state.
- `explanation` (String, optional): Updated rationale.

Response body (`ModifyAnswerResponse`):
- `answerNumber`, `content`, `isCorrect`, `explanation`, `questionId`, `updatedAt`

### 12.3 DELETE `/{answerNumber}`
- What the endpoint does:
  - Deletes an answer from a question.
- LMS business logic:
  - Removes obsolete answer option from assessment item.

Path fields:
- `questionId` (UUID)
- `answerNumber` (Long): Target answer identifier.

Response body (`DeleteAnswerResponse`):
- `answerNumber`, `questionId`, `message`
