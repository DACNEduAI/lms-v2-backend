package edu.ai.lms.core.coderunner.adapter.in.http;

import edu.ai.lms.config.IntegrationTestConfiguration;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import edu.ai.lms.core.attempt.app.service.AttemptGradingService;
import edu.ai.lms.core.coderunner.api.CodeSubmissionCompletedEvent;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import edu.ai.lms.core.coderunner.app.port.out.CodeExecutionEngine;
import edu.ai.lms.core.coderunner.app.port.out.CodeExecutionEngine.EngineState;
import edu.ai.lms.core.coderunner.app.port.out.CodeExecutionEngine.ExecutionResult;
import edu.ai.lms.core.coderunner.app.port.out.CodeExecutionEngine.ExecutionTicket;
import edu.ai.lms.core.coderunner.app.port.out.CodeExecutionEngine.Verdict;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Code Runner E2E Lifecycle IT")
class CodeRunnerE2EIT extends IntegrationTestConfiguration {

    @MockBean
    private CodeExecutionEngine codeExecutionEngine;

    @Value("${quiz.generation.webhook.secret}")
    private String webhookSecret;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private AttemptGradingService attemptGradingService;

    private static String teacherToken;
    private static String studentToken;
    private static UUID courseId;
    private static UUID chapterId;
    private static UUID learningOutcomeId;
    private static UUID questionId;
    private static UUID quizId;
    private static UUID attemptId;
    private static UUID submissionId;
    private static UUID recoveryAttemptId;

    @Test
    @Order(1)
    void setupUsersAndCourse() {
        teacherToken = registerUser("Teacher", "E2E", "teacher.e2e@ai.edu", "TeacherPass123!", "teacher");
        studentToken = registerUser("Student", "E2E", "student.e2e@ai.edu", "StudentPass123!", "student");

        courseId = UUID.fromString(given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body("""
                        {"courseName":"E2E Course","courseCode":"E2E101","active":true,
                         "startDate":"%s","endDate":"%s","learningOutcomes":[]}
                        """.formatted(
                        LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                        LocalDate.now().plusMonths(2).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))))
                .post("/api/course")
                .then().statusCode(201)
                .extract().path("courseId"));

        // Setup chapter and LO
        chapterId = UUID.randomUUID();
        jdbc.update("INSERT INTO edu.chapters (chapter_id,title,course_id) VALUES (?,?,?)",
                chapterId, "CodeRunner E2E Chapter", courseId);
                
        learningOutcomeId = UUID.randomUUID();
        jdbc.update("INSERT INTO edu.learning_outcomes (id,prefix_number,description,course_id) VALUES (?,1,?,?)",
                learningOutcomeId, "CodeRunner LO", courseId);

        // Enroll student
        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body("{\"studentEmails\":[\"student.e2e@ai.edu\"]}")
                .put("/api/course/{courseId}/students/add", courseId)
                .then().statusCode(200);
    }

    @Test
    @Order(2)
    void createCodeRunnerDraft() {
        String draftBody = """
                {
                  "chapterId": "%s",
                  "learningOutcomeIds": ["%s"],
                  "content": "Write a function that returns true if a number is even.",
                  "questionType": "CODE_RUNNER",
                  "cognitiveLevelCode": "APPLY",
                  "answers": [],
                  "codeRunnerContract": {
                    "schema_version": "2.0.0",
                    "contract_id": "is_even",
                    "contract_version": "1.0.0",
                    "status": "READY_FOR_REVIEW",
                    "title": "Is Even",
                    "statement": "Write a function.",
                    "runtime": {
                      "execution_profile": { "id": "dsa_cpp17", "version": "1.0.0" },
                      "archetype": { "id": "scalar_function", "version": "1.0.0" },
                      "comparator": { "id": "exact_typed", "version": "1.0.0" },
                      "entry_point": "isEven",
                      "signature": {
                        "return_fixture": { "id": "boolean", "version": "1.0.0" },
                        "arguments": [
                          { "name": "value", "fixture": { "id": "integer", "version": "1.0.0" }, "constraints": {} }
                        ]
                      },
                      "starter_code": "bool isEven(int value) { return false; }",
                      "public_tests": [
                        {
                          "id": "pub1", "category": "positive", "visibility": "PUBLIC",
                          "arguments": { "value": 2 }, "expected_output": true, "points": 0.0
                        }
                      ],
                      "banned_api_patterns": []
                    },
                    "private_grading": {
                      "reference_solution": "bool isEven(int value) { return value %% 2 == 0; }",
                      "hidden_tests": [
                        {
                          "id": "hid1", "category": "negative", "visibility": "HIDDEN",
                          "arguments": { "value": 3 }, "expected_output": false, "points": 1.0
                        }
                      ]
                    }
                  }
                }
                """.formatted(chapterId, learningOutcomeId);

        questionId = UUID.fromString(given().log().all().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(draftBody)
                .post("/api/courses/{courseId}/question-drafts", courseId)
                .then().log().all().statusCode(201)
                .extract().path("id"));
    }

    @Test
    @Order(3)
    void approveQuestion() {
        questionId = UUID.fromString(given().log().all().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body("{\"version\":0}")
                .post("/api/courses/{courseId}/question-drafts/{draftId}/validate", courseId, questionId)
                .then().log().all().statusCode(200)
                .extract().path("id"));
    }

    private static UUID quizQuestionId;

    @Test
    @Order(4)
    void createQuizAndAddQuestion() {
        quizId = UUID.fromString(given().log().all().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body("""
                        {"quizTitle":"E2E Quiz","description":"Test",
                         "unlimited":true,"passScore":50.0,"learningOutcomeIds":["%s"]}
                        """.formatted(learningOutcomeId))
                .post("/api/{courseId}/quiz", courseId)
                .then().log().all().statusCode(201)
                .extract().path("quizId"));

        // Add question from bank to quiz
        quizQuestionId = UUID.fromString(given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body("""
                        {"version":0,"bankQuestionIds":["%s"]}
                        """.formatted(questionId))
                .post("/api/courses/{courseId}/quizzes/{quizId}/questions/from-bank", courseId, quizId)
                .then().statusCode(200)
                .extract().path("[0].questionId"));
    }

    @Test
    @Order(5)
    void verifyCodeRunner() {
        when(codeExecutionEngine.submit(any())).thenReturn(new ExecutionTicket("dummy-token"));
        when(codeExecutionEngine.getResult(any())).thenReturn(
                new ExecutionResult(EngineState.FINISHED, Verdict.ACCEPTED, "B:1", "", "", 10.0, 1024L, ""),
                new ExecutionResult(EngineState.FINISHED, Verdict.ACCEPTED, "B:0", "", "", 10.0, 1024L, ""),
                new ExecutionResult(EngineState.FINISHED, Verdict.ACCEPTED, "B:1", "", "", 10.0, 1024L, ""),
                new ExecutionResult(EngineState.FINISHED, Verdict.ACCEPTED, "B:0", "", "", 10.0, 1024L, ""));

        given().log().all().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .post("/api/courses/{courseId}/quizzes/{quizId}/questions/{questionId}/code-runner/verify", courseId, quizId, quizQuestionId)
                .then().log().all().statusCode(200)
                .body("passed", equalTo(true));
    }

    @Test
    @Order(6)
    void publishQuiz() {
        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body("{\"version\":1}")
                .post("/api/courses/{courseId}/quizzes/{quizId}/publish", courseId, quizId)
                .then().statusCode(200)
                .body("version", equalTo(2));
    }

    @Test
    @Order(7)
    void pendingSubmissionFinalizesWhenCompletionEventArrives() {
        attemptId = UUID.fromString(given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + studentToken)
                .post("/api/{courseId}/attempt/quiz/{quizId}/start", courseId, quizId)
                .then().statusCode(200)
                .extract().path("attemptId"));

        // Submit answer (Code Runner Submission)
        submissionId = UUID.fromString(given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + studentToken)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("""
                        {"sourceCode":"bool isEven(int value) { return value % 2 == 0; }"}
                        """)
                .post("/api/{courseId}/attempt/{attemptId}/questions/{questionId}/code-runner/submissions", courseId, attemptId, quizQuestionId)
                .then().log().all().statusCode(202)
                .extract().path("submissionId"));

        // Reload/re-login can reconstruct both the editor source and the
        // selected SUBMIT entirely from backend state.
        given().header("Authorization", "Bearer " + studentToken)
                .get("/api/{courseId}/attempt/{attemptId}/questions/{questionId}/code-runner",
                        courseId, attemptId, quizQuestionId)
                .then().statusCode(200)
                .body("activeSubmissionId", equalTo(submissionId.toString()))
                .body("latestSubmitId", equalTo(submissionId.toString()));
        given().header("Authorization", "Bearer " + studentToken)
                .get("/api/{courseId}/attempt/{attemptId}/questions/{questionId}/code-runner/submissions/{submissionId}",
                        courseId, attemptId, quizQuestionId, submissionId)
                .then().statusCode(200)
                .body("sourceCode", equalTo("bool isEven(int value) { return value % 2 == 0; }"))
                .body("scoreReleased", equalTo(false))
                .body("hiddenTestPolicy", equalTo("HIDDEN_DETAILS_NEVER_RELEASED"));

        // A selected submission that is still PENDING is permanently wrong for
        // this finished attempt; completion does not retroactively change it.
        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + studentToken)
                .body("""
                        {"quizId":"%s","timeSpentSeconds":12,"submittedAnswers":[
                          {"questionId":"%s","studentAnswerId":null,"codeSubmissionId":"%s"}
                        ]}
                        """.formatted(quizId, quizQuestionId, submissionId))
                .post("/api/{courseId}/attempt/quiz/{quizId}/submit", courseId, quizId)
                .then().statusCode(200)
                .body("status", equalTo("FINISHED"))
                .body("score", equalTo(0.0f));

        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT status FROM edu.attempts WHERE id = ?", String.class, attemptId))
                .isEqualTo("FINISHED");

        // Simulate later worker completion. The finalized attempt stays zero.
        jdbc.update("UPDATE edu.code_submissions SET status='COMPLETED', verdict='ACCEPTED', score=1, max_score=1 WHERE id=?", submissionId);
        eventPublisher.publishEvent(new CodeSubmissionCompletedEvent(submissionId, attemptId));

        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT status FROM edu.attempts WHERE id = ?", String.class, attemptId))
                .isEqualTo("FINISHED");
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT score FROM edu.attempts WHERE id = ?", Double.class, attemptId))
                .isEqualTo(0.0);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT is_correct FROM edu.attempt_records WHERE attempt_id = ? AND question_id = ?",
                Boolean.class, attemptId, quizQuestionId)).isFalse();
        given().header("Authorization", "Bearer " + studentToken)
                .get("/api/{courseId}/attempt/{attemptId}/questions/{questionId}/code-runner/submissions/{submissionId}",
                        courseId, attemptId, quizQuestionId, submissionId)
                .then().statusCode(200)
                .body("scoreReleased", equalTo(true))
                .body("hiddenTestPolicy", equalTo("HIDDEN_DETAILS_NEVER_RELEASED"));
    }

    @Test
    @Order(8)
    void teacherCanInspectAndRegradeTheSelectedSubmissionWithoutDoubleScoring() {
        given().header("Authorization", "Bearer " + teacherToken)
                .get("/api/courses/{courseId}/quizzes/{quizId}/questions/{questionId}/code-runner/submissions",
                        courseId, quizId, quizQuestionId)
                .then().statusCode(200)
                .body("[0].submissionId", equalTo(submissionId.toString()))
                .body("[0].status", equalTo("COMPLETED"))
                .body("[0].selectedForGrading", equalTo(true));

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body("{\"reason\":\"Judge runtime was upgraded\"}")
                .post("/api/courses/{courseId}/quizzes/{quizId}/questions/{questionId}/code-runner/submissions/{submissionId}/regrade",
                        courseId, quizId, quizQuestionId, submissionId)
                .then().statusCode(202)
                .body("status", equalTo("PENDING"))
                .body("selectedForGrading", equalTo(true));

        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT status FROM edu.attempts WHERE id = ?", String.class, attemptId))
                .isEqualTo("FINALIZING");
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT score FROM edu.attempts WHERE id = ?", Double.class, attemptId))
                .isEqualTo(0.0);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM edu.code_runner_submission_actions WHERE submission_id = ? AND action = 'REGRADE'",
                Integer.class, submissionId)).isEqualTo(1);

        jdbc.update("UPDATE edu.code_submissions SET status='COMPLETED', verdict='ACCEPTED', score=1, max_score=1 WHERE id=?", submissionId);
        eventPublisher.publishEvent(new CodeSubmissionCompletedEvent(submissionId, attemptId));

        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT status FROM edu.attempts WHERE id = ?", String.class, attemptId))
                .isEqualTo("FINISHED");
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT score FROM edu.attempts WHERE id = ?", Double.class, attemptId))
                .isEqualTo(10.0);
    }

    @Test
    @Order(9)
    void recoveryFinalizesWhenTheCompletionEventWasLost() {
        recoveryAttemptId = UUID.fromString(given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + studentToken)
                .post("/api/{courseId}/attempt/quiz/{quizId}/start", courseId, quizId)
                .then().statusCode(200)
                .extract().path("attemptId"));

        UUID recoverySubmissionId = UUID.fromString(given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + studentToken)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"sourceCode\":\"bool isEven(int value) { return value % 2 == 0; }\"}")
                .post("/api/{courseId}/attempt/{attemptId}/questions/{questionId}/code-runner/submissions", courseId, recoveryAttemptId, quizQuestionId)
                .then().statusCode(202)
                .extract().path("submissionId"));

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + studentToken)
                .body("""
                        {"quizId":"%s","timeSpentSeconds":9,"submittedAnswers":[
                          {"questionId":"%s","studentAnswerId":null,"codeSubmissionId":"%s"}
                        ]}
                        """.formatted(quizId, quizQuestionId, recoverySubmissionId))
                .post("/api/{courseId}/attempt/quiz/{quizId}/submit", courseId, quizId)
                .then().statusCode(200)
                .body("status", equalTo("FINISHED"));

        // Simulate an incomplete snapshot created by an older deployment.
        jdbc.update("UPDATE edu.attempts SET status='FINALIZING',score=0 WHERE id=?", recoveryAttemptId);
        jdbc.update("UPDATE edu.attempt_records SET snapshot_complete=FALSE WHERE attempt_id=?", recoveryAttemptId);

        // Deliberately do not publish CodeSubmissionCompletedEvent: this is
        // the restart/event-loss case that the periodic recovery must repair.
        jdbc.update("UPDATE edu.code_submissions SET status='COMPLETED', verdict='ACCEPTED', score=1, max_score=1 WHERE id=?", recoverySubmissionId);
        attemptGradingService.recoverFinalizingAttempts();

        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT status FROM edu.attempts WHERE id = ?", String.class, recoveryAttemptId))
                .isEqualTo("FINISHED");
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT score FROM edu.attempts WHERE id = ?", Double.class, recoveryAttemptId))
                .isEqualTo(10.0);
    }

    private String registerUser(String first, String last, String email, String password, String role) {
        String type = role.equals("teacher") ? "employeeId" : "studentId";
        String extra = role.equals("student") ? ",\"enrollmentYear\":2026" : "";
        return given().contentType(ContentType.JSON).body("""
                {"firstName":"%s","lastName":"%s","email":"%s",
                 "password":"%s","%s":"%s"%s}
                """.formatted(first, last, email, password, type, UUID.randomUUID().toString().substring(0, 8), extra))
                .post("/api/auth/" + role + "/register")
                .then().statusCode(201)
                .extract().path("token");
    }
}
