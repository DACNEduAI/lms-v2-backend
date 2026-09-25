package edu.ai.lms.core.quiz3;

import edu.ai.lms.core.quiz3.app.service.Quiz3GenerationDispatcher;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;

import edu.ai.lms.config.IntegrationTestConfiguration;
import edu.ai.lms.core.coderunner.api.CodeSubmissionCompletedEvent;
import edu.ai.lms.core.coderunner.app.port.out.CodeExecutionEngine;
import edu.ai.lms.core.coderunner.app.port.out.CodeExecutionEngine.EngineState;
import edu.ai.lms.core.coderunner.app.port.out.CodeExecutionEngine.ExecutionResult;
import edu.ai.lms.core.coderunner.app.port.out.CodeExecutionEngine.ExecutionTicket;
import edu.ai.lms.core.coderunner.app.port.out.CodeExecutionEngine.Verdict;
import io.restassured.http.ContentType;

@DisplayName("Quiz3 AI Code Runner E2E IT")
class Quiz3CodeRunnerE2EIT extends IntegrationTestConfiguration {

    @MockBean private CodeExecutionEngine engine;
    @MockBean private Quiz3GenerationDispatcher generationDispatcher;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired private ApplicationEventPublisher events;
    @Value("${quiz.generation.webhook.secret}") private String webhookSecret;

    @Test
    void aiCallbackReviewVerifyClonePublishAndStudentFinalization() throws Exception {
        String teacherToken = register("Quiz3", "Teacher", "quiz3.cr.teacher@ai.edu", "teacher");
        String studentToken = register("Quiz3", "Student", "quiz3.cr.student@ai.edu", "student");
        UUID teacherId = jdbc.queryForObject(
                "SELECT id FROM edu.users WHERE email='quiz3.cr.teacher@ai.edu'", UUID.class);
        UUID courseId = UUID.fromString(given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body("""
                        {"courseName":"Quiz3 Code Runner","courseCode":"Q3CR","active":true,
                         "startDate":"%s","endDate":"%s","learningOutcomes":[]}
                        """.formatted(date(1), date(60)))
                .post("/api/course").then().statusCode(201).extract().path("courseId"));
        given().contentType(ContentType.JSON).header("Authorization", "Bearer " + teacherToken)
                .body("{\"studentEmails\":[\"quiz3.cr.student@ai.edu\"]}")
                .put("/api/course/{courseId}/students/add", courseId).then().statusCode(200);

        UUID chapterId = UUID.randomUUID(), outcomeId = UUID.randomUUID();
        jdbc.update("INSERT INTO edu.chapters(chapter_id,title,course_id) VALUES(?,?,?)",
                chapterId, "Functions", courseId);
        jdbc.update("INSERT INTO edu.learning_outcomes(id,prefix_number,description,course_id) VALUES(?,1,?,?)",
                outcomeId, "Implement a scalar function", courseId);

        UUID matrixId = UUID.randomUUID(), rowId = UUID.randomUUID(), levelId = UUID.randomUUID();
        jdbc.update("INSERT INTO edu.exam_matrices(id,course_id,title,lifecycle_status,mode) VALUES(?,?,?,'PUBLISHED','MANUAL')",
                matrixId, courseId, "Quiz3 mixed matrix");
        jdbc.update("INSERT INTO edu.exam_matrix_custom_learning_levels(id,matrix_id,name,sort_order) VALUES(?,?,?,0)",
                levelId, matrixId, "Apply");
        jdbc.update("INSERT INTO edu.exam_matrix_rows(id,matrix_id,chapter_id,mapped_learning_outcome_id,sort_order) VALUES(?,?,?,?,0)",
                rowId, matrixId, chapterId, outcomeId);
        jdbc.update("INSERT INTO edu.exam_matrix_cells(row_id,custom_learning_level_id,question_count,question_type) VALUES(?,?,1,'CODE_RUNNER')",
                rowId, levelId);

        UUID assessmentId = UUID.randomUUID(), jobId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO edu.quiz3_assessments
                  (id,course_id,title,unlimited,variant_count,lifecycle_status,current_step,matrix_id,matrix_version,created_by)
                VALUES (?,?,?,true,2,'GENERATING',5,?,0,?)
                """, assessmentId, courseId, "Generated C++17 quiz", matrixId, teacherId);
        String snapshot = """
                {"jobId":"%s","assessmentId":"%s","contractVersion":4,
                 "course":{"id":"%s","name":"Quiz3 Code Runner"},"quizName":"Generated C++17 quiz - 001",
                 "matrix":{"id":"%s","name":"Quiz3 mixed matrix","rows":[{
                   "id":"%s","chapter":{"id":"%s","name":"Functions"},
                   "learningOutcome":{"id":"%s","name":"LO1"},"content":{"id":"%s","groupId":"functions","groupName":"Functions","detail":"Scalar functions","contentCode":"functions.apply"},"cells":[{
                     "customLearningLevelId":"%s","customLearningLevelName":"Apply",
                     "questionCount":1,"assetAllocations":[{"assetType":"TEXT","questionType":"MULTIPLE_CHOICE","questionCount":0,"effectiveAssetType":"TEXT"},{"assetType":"IMAGE","questionType":"MULTIPLE_CHOICE","questionCount":0,"effectiveAssetType":"TEXT","fallbackReason":"IMAGE_MEDIA_NOT_SUPPORTED"},{"assetType":"CODE_MCQ","questionType":"MULTIPLE_CHOICE","questionCount":0,"effectiveAssetType":"CODE_MCQ"},{"assetType":"CODE_RUNNER","questionType":"CODE_RUNNER","questionCount":1,"effectiveAssetType":"CODE_RUNNER"}]}]}]},"materialIds":[]}
                """.formatted(jobId, assessmentId, courseId, matrixId, rowId, chapterId, outcomeId, UUID.randomUUID(), levelId);
        jdbc.update("""
                INSERT INTO edu.quiz3_generation_jobs
                  (id,assessment_id,course_id,matrix_id,matrix_version,requested_by,request_snapshot,payload_hash,idempotency_key,status,attempt_count,total_question_count)
                VALUES (?,?,?,?,0,?,CAST(? AS jsonb),'hash',?,'PROCESSING',1,1)
                """, jobId, assessmentId, courseId, matrixId, teacherId, snapshot, "quiz3-cr-" + jobId);
        jdbc.update("UPDATE edu.quiz3_assessments SET base_generation_job_id=? WHERE id=?", jobId, assessmentId);

        String callback = """
                {"eventId":"%s","jobId":"%s","assessmentId":"%s","status":"COMPLETED",
                 "quiz":{"quizName":"Generated C++17 quiz - 001","questions":[{
                   "content":"Return the sum of a and b.","type":"CODE_RUNNER","matrixRowId":"%s",
                   "customLearningLevelId":"%s","learningOutcomeId":"%s","answers":[],
                   "codeRunnerContract":{"schema_version":"2.0.0","contract_id":"ai_sum_e2e",
                     "contract_version":"1.0.0","status":"READY_FOR_REVIEW","title":"Sum",
                     "statement":"Return the sum of a and b.","runtime":{
                       "execution_profile":{"id":"dsa_cpp17","version":"1.0.0"},
                       "archetype":{"id":"scalar_function","version":"1.0.0"},
                       "comparator":{"id":"exact_typed","version":"1.0.0"},"entry_point":"sum",
                       "signature":{"return_fixture":{"id":"integer","version":"1.0.0"},"arguments":[
                         {"name":"a","fixture":{"id":"integer","version":"1.0.0"},"constraints":{}},
                         {"name":"b","fixture":{"id":"integer","version":"1.0.0"},"constraints":{}}]},
                       "starter_code":"int sum(int a, int b) { return 0; }",
                       "public_tests":[{"id":"public_1","category":"example","visibility":"PUBLIC",
                         "arguments":{"a":1,"b":2},"expected_output":3,"points":0}],"banned_api_patterns":[]},
                     "private_grading":{"reference_solution":"int sum(int a, int b) { return a + b; }",
                       "hidden_tests":[{"id":"hidden_1","category":"negative","visibility":"HIDDEN",
                         "arguments":{"a":-2,"b":1},"expected_output":-1,"points":2}]}}
                 }]}}
                """.formatted(UUID.randomUUID(), jobId, assessmentId, rowId, levelId, outcomeId);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        given().contentType(ContentType.JSON).header("X-Webhook-Timestamp", timestamp)
                .header("X-Webhook-Signature", "v1=" + sign(timestamp, callback)).body(callback)
                .post("/api/internal/webhooks/quiz3-generation").then().statusCode(200);

        UUID baseQuizId = jdbc.queryForObject(
                "SELECT quiz_id FROM edu.quiz3_assessment_variants WHERE assessment_id=? AND variant_kind='BASE'",
                UUID.class, assessmentId);
        UUID baseQuestionId = jdbc.queryForObject(
                "SELECT id FROM edu.questions WHERE quiz_id=?", UUID.class, baseQuizId);
        assertThat(jdbc.queryForObject("SELECT type FROM edu.questions WHERE id=?", String.class, baseQuestionId))
                .isEqualTo("CODE_RUNNER");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM edu.answers WHERE question_id=?", Integer.class, baseQuestionId))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT contract_status FROM edu.code_runner_configs WHERE question_id=?", String.class, baseQuestionId))
                .isEqualTo("READY_FOR_REVIEW");

        when(engine.submit(any())).thenReturn(new ExecutionTicket("judge0-ticket"));
        when(engine.getResult(any())).thenReturn(
                new ExecutionResult(EngineState.FINISHED, Verdict.ACCEPTED, "I:3", "", "", 8.0, 1024L, ""),
                new ExecutionResult(EngineState.FINISHED, Verdict.ACCEPTED, "I:-1", "", "", 8.0, 1024L, ""));
        given().header("Authorization", "Bearer " + teacherToken)
                .post("/api/courses/{courseId}/quizzes/{quizId}/questions/{questionId}/code-runner/verify",
                        courseId, baseQuizId, baseQuestionId)
                .then().statusCode(200).body("passed", equalTo(true));

        given().header("Authorization", "Bearer " + teacherToken)
                .post("/api/courses/{courseId}/quiz3/assessments/{assessmentId}/base-quiz/confirm", courseId, assessmentId)
                .then().statusCode(200);
        given().contentType(ContentType.JSON).header("Authorization", "Bearer " + teacherToken)
                .body("{\"variantCount\":2}")
                .post("/api/courses/{courseId}/quiz3/assessments/{assessmentId}/variants", courseId, assessmentId)
                .then().statusCode(201);

        UUID studentQuizId = jdbc.queryForObject("""
                SELECT quiz_id FROM edu.quiz3_assessment_variants
                WHERE assessment_id=? AND variant_kind='SHUFFLED'
                """, UUID.class, assessmentId);
        UUID studentQuestionId = jdbc.queryForObject(
                "SELECT id FROM edu.questions WHERE quiz_id=?", UUID.class, studentQuizId);
        assertThat(jdbc.queryForObject("""
                SELECT contract_status FROM edu.code_runner_configs WHERE question_id=? ORDER BY revision DESC LIMIT 1
                """, String.class, studentQuestionId)).isEqualTo("VERIFIED_EXECUTION_GOLD");

        given().header("Authorization", "Bearer " + teacherToken)
                .post("/api/courses/{courseId}/quiz3/assessments/{assessmentId}/publish", courseId, assessmentId)
                .then().statusCode(200).body("status", equalTo("PUBLISHED"));

        UUID attemptId = UUID.fromString(given().header("Authorization", "Bearer " + studentToken)
                .post("/api/{courseId}/attempt/quiz/{quizId}/start", courseId, studentQuizId)
                .then().statusCode(200).extract().path("attemptId"));
        UUID submissionId = UUID.fromString(given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + studentToken)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"sourceCode\":\"int sum(int a, int b) { return a + b; }\"}")
                .post("/api/{courseId}/attempt/{attemptId}/questions/{questionId}/code-runner/submissions",
                        courseId, attemptId, studentQuestionId)
                .then().statusCode(202).extract().path("submissionId"));
        given().contentType(ContentType.JSON).header("Authorization", "Bearer " + studentToken)
                .body("""
                        {"quizId":"%s","timeSpentSeconds":10,"submittedAnswers":[
                          {"questionId":"%s","studentAnswerId":null,"codeSubmissionId":"%s"}]}
                        """.formatted(studentQuizId, studentQuestionId, submissionId))
                .post("/api/{courseId}/attempt/quiz/{quizId}/submit", courseId, studentQuizId)
                .then().statusCode(200).body("status", equalTo("FINISHED"));
        jdbc.update("UPDATE edu.code_submissions SET status='COMPLETED',verdict='ACCEPTED',score=2,max_score=2 WHERE id=?",
                submissionId);
        events.publishEvent(new CodeSubmissionCompletedEvent(submissionId, attemptId));
        assertThat(jdbc.queryForObject("SELECT status FROM edu.attempts WHERE id=?", String.class, attemptId))
                .isEqualTo("FINISHED");
        assertThat(jdbc.queryForObject("SELECT score FROM edu.attempts WHERE id=?", Double.class, attemptId))
                .isEqualTo(0.0);
    }

    @Test
    void invalidAiContractFailsCleanlyAndTeacherCanStartANewGenerationJob() throws Exception {
        String teacherToken = register("Retry", "Teacher", "quiz3.cr.retry.teacher@ai.edu", "teacher");
        UUID teacherId = jdbc.queryForObject(
                "SELECT id FROM edu.users WHERE email='quiz3.cr.retry.teacher@ai.edu'", UUID.class);
        UUID courseId = UUID.fromString(given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body("""
                        {"courseName":"Quiz3 invalid AI recovery","courseCode":"Q3CRR","active":true,
                         "startDate":"%s","endDate":"%s","learningOutcomes":[]}
                        """.formatted(date(1), date(60)))
                .post("/api/course").then().statusCode(201).extract().path("courseId"));

        UUID chapterId = UUID.randomUUID(), outcomeId = UUID.randomUUID();
        UUID matrixId = UUID.randomUUID(), rowId = UUID.randomUUID(), levelId = UUID.randomUUID();
        jdbc.update("INSERT INTO edu.chapters(chapter_id,title,course_id) VALUES(?,?,?)",
                chapterId, "Recovery", courseId);
        jdbc.update("INSERT INTO edu.learning_outcomes(id,prefix_number,description,course_id) VALUES(?,1,?,?)",
                outcomeId, "Recover invalid generation", courseId);
        jdbc.update("INSERT INTO edu.exam_matrices(id,course_id,title,lifecycle_status,mode) VALUES(?,?,?,'PUBLISHED','MANUAL')",
                matrixId, courseId, "Recovery matrix");
        jdbc.update("INSERT INTO edu.exam_matrix_custom_learning_levels(id,matrix_id,name,sort_order) VALUES(?,?,?,0)",
                levelId, matrixId, "Apply");
        jdbc.update("INSERT INTO edu.exam_matrix_rows(id,matrix_id,chapter_id,mapped_learning_outcome_id,sort_order) VALUES(?,?,?,?,0)",
                rowId, matrixId, chapterId, outcomeId);
        jdbc.update("INSERT INTO edu.exam_matrix_cells(row_id,custom_learning_level_id,question_count,question_type) VALUES(?,?,1,'CODE_RUNNER')",
                rowId, levelId);
        jdbc.update("""
                INSERT INTO edu.materials(id,title,type,file_key,status,ingestion_status,course_id)
                VALUES (?,?, 'PDF', ?, 'UPLOADED', 'READY', ?)
                """, UUID.randomUUID(), "Ready source", "quiz3-retry-" + UUID.randomUUID(), courseId);

        UUID assessmentId = UUID.randomUUID(), failedJobId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO edu.quiz3_assessments
                  (id,course_id,title,unlimited,variant_count,lifecycle_status,current_step,matrix_id,matrix_version,created_by)
                VALUES (?,?,?,true,1,'GENERATING',5,?,0,?)
                """, assessmentId, courseId, "Retry invalid AI", matrixId, teacherId);
        String snapshot = """
                {"jobId":"%s","assessmentId":"%s","contractVersion":4,
                 "course":{"id":"%s","name":"Recovery"},"quizName":"Retry invalid AI - 001",
                 "matrix":{"id":"%s","name":"Recovery matrix","rows":[{
                   "id":"%s","chapter":{"id":"%s","name":"Recovery"},
                   "learningOutcome":{"id":"%s","name":"LO1"},"content":{"id":"%s","groupId":"recovery","groupName":"Recovery","detail":"Recover invalid generation","contentCode":"recovery.apply"},"cells":[{
                     "customLearningLevelId":"%s","customLearningLevelName":"Apply",
                     "questionCount":1,"assetAllocations":[{"assetType":"TEXT","questionType":"MULTIPLE_CHOICE","questionCount":0,"effectiveAssetType":"TEXT"},{"assetType":"IMAGE","questionType":"MULTIPLE_CHOICE","questionCount":0,"effectiveAssetType":"TEXT","fallbackReason":"IMAGE_MEDIA_NOT_SUPPORTED"},{"assetType":"CODE_MCQ","questionType":"MULTIPLE_CHOICE","questionCount":0,"effectiveAssetType":"CODE_MCQ"},{"assetType":"CODE_RUNNER","questionType":"CODE_RUNNER","questionCount":1,"effectiveAssetType":"CODE_RUNNER"}]}]}]},"materialIds":[]}
                """.formatted(failedJobId, assessmentId, courseId, matrixId, rowId, chapterId, outcomeId, UUID.randomUUID(), levelId);
        jdbc.update("""
                INSERT INTO edu.quiz3_generation_jobs
                  (id,assessment_id,course_id,matrix_id,matrix_version,requested_by,request_snapshot,payload_hash,idempotency_key,status,attempt_count,total_question_count)
                VALUES (?,?,?,?,0,?,CAST(? AS jsonb),'hash',?,'PROCESSING',1,1)
                """, failedJobId, assessmentId, courseId, matrixId, teacherId, snapshot, "invalid-" + failedJobId);
        jdbc.update("UPDATE edu.quiz3_assessments SET base_generation_job_id=? WHERE id=?", failedJobId, assessmentId);

        String callback = """
                {"eventId":"%s","jobId":"%s","assessmentId":"%s","status":"COMPLETED",
                 "quiz":{"quizName":"Retry invalid AI - 001","questions":[{
                   "content":"Broken contract","type":"CODE_RUNNER","matrixRowId":"%s",
                   "customLearningLevelId":"%s","learningOutcomeId":"%s","answers":[],
                   "codeRunnerContract":{}}]}}
                """.formatted(UUID.randomUUID(), failedJobId, assessmentId, rowId, levelId, outcomeId);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        given().contentType(ContentType.JSON).header("X-Webhook-Timestamp", timestamp)
                .header("X-Webhook-Signature", "v1=" + sign(timestamp, callback)).body(callback)
                .post("/api/internal/webhooks/quiz3-generation").then().statusCode(200);

        assertThat(jdbc.queryForObject(
                "SELECT status FROM edu.quiz3_generation_jobs WHERE id=?", String.class, failedJobId))
                .isEqualTo("FAILED");
        assertThat(jdbc.queryForObject(
                "SELECT error_code FROM edu.quiz3_generation_jobs WHERE id=?", String.class, failedJobId))
                .isEqualTo("INVALID_AI_OUTPUT");
        assertThat(jdbc.queryForObject(
                "SELECT lifecycle_status FROM edu.quiz3_assessments WHERE id=?", String.class, assessmentId))
                .isEqualTo("DRAFT");

        UUID retryJobId = UUID.fromString(given().header("Authorization", "Bearer " + teacherToken)
                .post("/api/courses/{courseId}/quiz3/assessments/{assessmentId}/base-quiz/generation",
                        courseId, assessmentId)
                .then().statusCode(202).body("status", equalTo("PENDING")).extract().path("jobId"));
        assertThat(retryJobId).isNotEqualTo(failedJobId);
    }

    private String register(String first, String last, String email, String role) {
        String type = role.equals("teacher") ? "employeeId" : "studentId";
        String extra = role.equals("student") ? ",\"enrollmentYear\":2026" : "";
        return given().contentType(ContentType.JSON).body("""
                {"firstName":"%s","lastName":"%s","email":"%s","password":"Pass12345!",
                 "%s":"%s"%s}
                """.formatted(first, last, email, type, UUID.randomUUID().toString().substring(0, 8), extra))
                .post("/api/auth/" + role + "/register").then().statusCode(201).extract().path("token");
    }

    private String sign(String timestamp, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
    }

    private static String date(int days) {
        return LocalDate.now().plusDays(days).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    }
}
