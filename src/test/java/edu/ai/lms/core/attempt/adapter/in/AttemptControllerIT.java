package edu.ai.lms.core.attempt.adapter.in;

import edu.ai.lms.config.IntegrationTestConfiguration;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Attempt Controller Integration Tests")
class gitAttemptControllerIT extends IntegrationTestConfiguration {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private static String teacherToken;
    private static String studentToken;
    private static String studentEmail = "attempt.student@example.com";

    private static UUID courseId;
    private static UUID timedQuizId;
    private static UUID untimedQuizId;
    private static UUID timedQuestionId;
    private static Long timedCorrectAnswerNumber;

    @Test
    @Order(1)
    @DisplayName("Should register teacher and student")
    void registerUsers() {
        String teacherRegisterBody = """
                {
                    "firstName": "Attempt",
                    "lastName": "Teacher",
                    "email": "attempt.teacher@example.com",
                    "password": "TeacherPass123!",
                    "employeeId": "EMP-ATT-001",
                    "department": "Computer Science",
                    "specialization": "Assessment"
                }
                """;

        teacherToken = given()
                .contentType(ContentType.JSON)
                .body(teacherRegisterBody)
                .when()
                .post("/api/auth/teacher/register")
                .then()
                .statusCode(201)
                .extract()
                .path("token");

        String studentRegisterBody = """
                {
                    "firstName": "Attempt",
                    "lastName": "Student",
                    "email": "%s",
                    "password": "StudentPass123!",
                    "studentId": "STU-ATT-001",
                    "enrollmentYear": 2026
                }
                """.formatted(studentEmail);

        studentToken = given()
                .contentType(ContentType.JSON)
                .body(studentRegisterBody)
                .when()
                .post("/api/auth/student/register")
                .then()
                .statusCode(201)
                .extract()
                .path("token");
    }

    @Test
    @Order(2)
    @DisplayName("Should create course and enroll student")
    void createCourseAndEnrollStudent() {
        String startDate = LocalDateTime.now().plusDays(1).format(DATE_FORMATTER);
        String endDate = LocalDateTime.now().plusMonths(2).format(DATE_FORMATTER);

        String createCourseBody = """
                {
                    "courseName": "Attempt Testing Course",
                    "description": "Course for attempt workflow tests",
                    "courseCode": "ATT101",
                    "active": true,
                    "coverImage": "https://example.com/attempt.jpg",
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": [
                        {
                            "code": "CLO-ATT-1",
                            "description": "Understand attempt flows"
                        }
                    ]
                }
                """.formatted(startDate, endDate);

        Response createCourseResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(createCourseBody)
                .when()
                .post("/api/course")
                .then()
                .statusCode(201)
                .body("courseId", notNullValue())
                .extract().response();

        courseId = UUID.fromString(createCourseResponse.path("courseId"));

        String addStudentBody = """
                {
                    "studentEmails": ["%s"]
                }
                """.formatted(studentEmail);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(addStudentBody)
                .when()
                .put("/api/course/" + courseId + "/students/add")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(3)
    @DisplayName("Should create timed and non-timed quizzes")
    void createQuizzes() {
        String timedDueTime = LocalDateTime.now().plusMinutes(2).format(DATE_TIME_FORMATTER);
        String timedQuizBody = """
                {
                    "quizTitle": "Timed Attempt Quiz",
                    "unlimited": false,
                    "timeLimitMinutes": 1,
                    "passScore": 5.0,
                    "dueTime": "%s",
                    "attemptLimited": 2,
                    "display": true
                }
                """.formatted(timedDueTime);

        Response timedQuizResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(timedQuizBody)
                .when()
                .post("/api/" + courseId + "/quiz")
                .then()
                .statusCode(201)
                .body("quizId", notNullValue())
                .extract().response();

        timedQuizId = UUID.fromString(timedQuizResponse.path("quizId"));

        String dueTime = LocalDateTime.now().plusSeconds(1).format(DATE_TIME_FORMATTER);
        String untimedQuizBody = """
                {
                    "quizTitle": "Untimed Attempt Quiz",
                    "unlimited": false,
                    "passScore": 5.0,
                    "attemptLimited": 2,
                    "dueTime": "%s",
                    "display": true
                }
                """.formatted(dueTime);

        Response untimedQuizResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(untimedQuizBody)
                .when()
                .post("/api/" + courseId + "/quiz")
                .then()
                .statusCode(201)
                .body("quizId", notNullValue())
                .extract().response();

        untimedQuizId = UUID.fromString(untimedQuizResponse.path("quizId"));
    }

    @Test
    @Order(4)
    @DisplayName("Should create question and answers for timed quiz")
    void createQuestionAndAnswers() {
        // Create Learning Outcome
        String loBody = """
                {
                    \"learningOutcomes\": [
                        {\"code\": \"LO-ATT\", \"description\": \"Attempt LO\"}
                    ]
                }
                """;
        Response loResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(loBody)
                .when()
                .post("/api/" + courseId + "/learning-outcome")
                .then()
                .statusCode(201)
                .extract().response();
        UUID learningOutcomeId = UUID.fromString(loResponse.path("learningOutcomes[0].id"));

        String createQuestionBody = """
                {
                    "questions": [
                        {
                            "content": "2 + 3 = ?",
                            "type": "MULTIPLE_CHOICE",
                            "learningOutcomeId": "%s",
                            "answers": [
                                {
                                    "content": "5",
                                    "isCorrect": true,
                                    "explanation": "Correct"
                                },
                                {
                                    "content": "6",
                                    "isCorrect": false,
                                    "explanation": "Incorrect"
                                }
                            ]
                        }
                    ]
                }
                """.formatted(learningOutcomeId);

        Response questionResponse = given()
            .contentType(ContentType.MULTIPART)
            .multiPart("question", "question.json", createQuestionBody, "application/json")
            .header("Authorization", "Bearer " + teacherToken)
            .when()
            .post("/api/quiz/" + timedQuizId + "/question")
                .then()
                .statusCode(201)
                .body("", hasSize(1))
                .body("[0].questionId", notNullValue())
                .body("[0].answers", hasSize(2))
                .extract().response();

        timedQuestionId = UUID.fromString(questionResponse.path("[0].questionId"));
        List<Map<String, Object>> answerItems = questionResponse.jsonPath().getList("[0].answers");
        timedCorrectAnswerNumber = ((Number) answerItems.getFirst().get("answerNumber")).longValue();
    }

    @Test
    @Order(5)
    @DisplayName("Should start and submit timed quiz on time")
    void submitTimedQuizOnTime() {
        given()
                .header("Authorization", "Bearer " + studentToken)
                .when()
                .post("/api/" + courseId + "/attempt/quiz/" + timedQuizId + "/start")
                .then()
                .statusCode(200)
                .body("attemptId", notNullValue())
                .body("status", equalTo("STARTED"));

        String submitBody = """
                {
                    "quizId": "%s",
                    "submittedAnswers": [
                        {
                            "questionId": "%s",
                            "studentAnswerId": %d
                        }
                    ],
                    "timeSpentSeconds": 3
                }
                """.formatted(timedQuizId, timedQuestionId, timedCorrectAnswerNumber);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + studentToken)
                .body(submitBody)
                .when()
                .post("/api/" + courseId + "/attempt/quiz/" + timedQuizId + "/submit")
                .then()
                .statusCode(200)
                .body("status", equalTo("FINISHED"))
                .body("score", notNullValue());
    }

    @Test
    @Order(6)
    @DisplayName("Should keep timed attempt active before timer expires")
    void submitTimedQuizTooLate() throws InterruptedException {
        given()
                .header("Authorization", "Bearer " + studentToken)
                .when()
                .post("/api/" + courseId + "/attempt/quiz/" + timedQuizId + "/start")
                .then()
                .statusCode(200)
                .body("status", equalTo("STARTED"));

        Thread.sleep(6500);

        given()
                .header("Authorization", "Bearer " + studentToken)
                .when()
                .get("/api/" + courseId + "/attempt/quiz/" + timedQuizId + "/me")
                .then()
                .statusCode(200)
                .body("attempts", hasSize(2))
                .body("attempts[0].status", equalTo("STARTED"));
    }

    @Test
    @Order(7)
    @DisplayName("Should reject new start when attempt limit reached")
    void rejectStartWhenLimitReached() {
        String submitBody = """
                {
                    "quizId": "%s",
                    "submittedAnswers": [
                        {
                            "questionId": "%s",
                            "studentAnswerId": %d
                        }
                    ],
                    "timeSpentSeconds": 4
                }
                """.formatted(timedQuizId, timedQuestionId, timedCorrectAnswerNumber);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + studentToken)
                .body(submitBody)
                .when()
                .post("/api/" + courseId + "/attempt/quiz/" + timedQuizId + "/submit")
                .then()
                .statusCode(200)
                .body("status", equalTo("FINISHED"));

        given()
                .header("Authorization", "Bearer " + studentToken)
                .when()
                .post("/api/" + courseId + "/attempt/quiz/" + timedQuizId + "/start")
                .then()
                .statusCode(409);
    }

    @Test
    @Order(8)
    @DisplayName("Should auto-finish untimed quiz with zero score after due time + grace")
    void submitUntimedQuizLate() throws InterruptedException {
        given()
                .header("Authorization", "Bearer " + studentToken)
                .when()
                .post("/api/" + courseId + "/attempt/quiz/" + untimedQuizId + "/start")
                .then()
                .statusCode(200)
                .body("status", equalTo("STARTED"));

        Thread.sleep(7000);

        given()
            .header("Authorization", "Bearer " + studentToken)
            .when()
            .get("/api/" + courseId + "/attempt/quiz/" + untimedQuizId + "/me")
            .then()
            .statusCode(200)
            .body("attempts", hasSize(1))
            .body("attempts[0].status", equalTo("FINISHED"))
            .body("attempts[0].score", equalTo(0.0f));
    }

    @Test
    @Order(9)
    @DisplayName("Should auto-finish timed quiz with zero score after due time + grace")
    void autoFinishTimedQuizAfterDueTimeAndGrace() throws InterruptedException {
        String nearDueTime = LocalDateTime.now().plusSeconds(5).format(DATE_TIME_FORMATTER);
        String timedNearDueQuizBody = """
                {
                    "quizTitle": "Timed Near Due Auto-Finish Quiz",
                    "unlimited": false,
                    "timeLimitMinutes": 2,
                    "passScore": 5.0,
                    "dueTime": "%s",
                    "attemptLimited": 1,
                    "display": true
                }
                """.formatted(nearDueTime);

        Response timedNearDueQuizResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(timedNearDueQuizBody)
                .when()
                .post("/api/" + courseId + "/quiz")
                .then()
                .statusCode(201)
                .body("quizId", notNullValue())
                .extract().response();

        UUID timedNearDueQuizId = UUID.fromString(timedNearDueQuizResponse.path("quizId"));

        given()
                .header("Authorization", "Bearer " + studentToken)
                .when()
                .post("/api/" + courseId + "/attempt/quiz/" + timedNearDueQuizId + "/start")
                .then()
                .statusCode(200)
                .body("status", equalTo("STARTED"));

        Thread.sleep(12000);

        given()
                .header("Authorization", "Bearer " + studentToken)
                .when()
                .get("/api/" + courseId + "/attempt/quiz/" + timedNearDueQuizId + "/me")
                .then()
                .statusCode(200)
                .body("attempts", hasSize(1))
                .body("attempts[0].status", equalTo("FINISHED"))
                .body("attempts[0].score", equalTo(0.0f))
                .body("attempts[0].errorReason", equalTo("Attempt expired before submission"));
    }

    @Test
    @Order(10)
    @DisplayName("Should get my attempts for a quiz")
    void getMyAttempts() {
        given()
                .header("Authorization", "Bearer " + studentToken)
                .when()
                .get("/api/" + courseId + "/attempt/quiz/" + timedQuizId + "/me")
                .then()
                .statusCode(200)
                .body("quizId", equalTo(timedQuizId.toString()))
                .body("attempts", hasSize(2));
    }

    @Test
    @Order(11)
    @DisplayName("Should reject submit when quizId in body mismatches URL")
    void rejectSubmitWhenQuizIdMismatch() {
        String submitBody = """
                {
                    "quizId": "%s",
                    "submittedAnswers": [],
                    "timeSpentSeconds": 1
                }
                """.formatted(UUID.randomUUID());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + studentToken)
                .body(submitBody)
                .when()
                .post("/api/" + courseId + "/attempt/quiz/" + timedQuizId + "/submit")
                .then()
                .statusCode(anyOf(is(400), is(404)));
    }
}
