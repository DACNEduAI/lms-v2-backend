package edu.ai.lms.core.quiz.adapter.in.http;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import edu.ai.lms.config.IntegrationTestConfiguration;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Question Controller Integration Tests")
class QuestionControllerIT extends IntegrationTestConfiguration {

    private static String teacherToken;
    private static UUID createdCourseId;
    private static UUID createdQuizId;
    private static UUID createdQuestionId;
    private static UUID secondQuestionId;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ==================== SETUP ====================

    @Test
    @Order(1)
    @DisplayName("Should register a teacher for question tests")
    void testRegisterTeacher() {
        String requestBody = """
                {
                    "firstName": "Question",
                    "lastName": "Teacher",
                    "email": "question.teacher@example.com",
                    "password": "TeacherPass123!",
                    "employeeId": "EMP-QST001",
                    "department": "Mathematics",
                    "specialization": "Algebra"
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/auth/teacher/register")
                .then()
                .log().all()
                .statusCode(201)
                .body("token", notNullValue())
                .extract().response();

        teacherToken = response.jsonPath().getString("token");
    }

    @Test
    @Order(2)
    @DisplayName("Should create a course for question tests")
    void testCreateCourse() {
        String startDate = LocalDate.now().plusDays(1).format(DATE_FORMATTER);
        String endDate = LocalDate.now().plusMonths(3).format(DATE_FORMATTER);

        String requestBody = """
                {
                    "courseName": "Math for Question Tests",
                    "description": "Course used for question integration tests",
                    "courseCode": "QSTN101",
                    "active": true,
                    "coverImage": "https://example.com/math.jpg",
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": [
                        {
                            "code": "CLO-1",
                            "description": "Understand algebra"
                        }
                    ]
                }
                """.formatted(startDate, endDate);

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/course")
                .then()
                .log().all()
                .statusCode(201)
                .body("courseId", notNullValue())
                .extract().response();

        createdCourseId = UUID.fromString(response.jsonPath().getString("courseId"));
    }

    @Test
    @Order(3)
    @DisplayName("Should create a quiz for question tests")
    void testCreateQuiz() {
        String requestBody = """
                {
                    "quizTitle": "Algebra Quiz",
                    "timeLimitMinutes": 45,
                    "passScore": 60.0
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/quiz")
                .then()
                .log().all()
                .statusCode(201)
                .body("quizId", notNullValue())
                .extract().response();

        createdQuizId = UUID.fromString(response.jsonPath().getString("quizId"));
    }

    // ==================== CREATE QUESTION TESTS ====================

    @Test
    @Order(4)
    @DisplayName("Should successfully create a question")
    void testCreateQuestion_ShouldSucceed() {
        String requestBody = """
                {
                    "content": "What is 2 + 2?",
                    "type": "MULTIPLE_CHOICE",
                    "points": 5.0
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/quiz/" + createdQuizId + "/question")
                .then()
                .log().all()
                .statusCode(201)
                .body("questionId", notNullValue())
                .body("content", equalTo("What is 2 + 2?"))
                .body("type", equalTo("MULTIPLE_CHOICE"))
                .body("points", equalTo(5.0f))
                .body("quizId", equalTo(createdQuizId.toString()))
                .body("createdAt", notNullValue())
                .extract().response();

        createdQuestionId = UUID.fromString(response.jsonPath().getString("questionId"));
    }

    @Test
    @Order(5)
    @DisplayName("Should create a second question")
    void testCreateSecondQuestion_ShouldSucceed() {
        String requestBody = """
                {
                    "content": "Is the Earth flat?",
                    "type": "TRUE_FALSE",
                    "points": 3.0
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/quiz/" + createdQuizId + "/question")
                .then()
                .log().all()
                .statusCode(201)
                .body("questionId", notNullValue())
                .body("content", equalTo("Is the Earth flat?"))
                .body("type", equalTo("TRUE_FALSE"))
                .extract().response();

        secondQuestionId = UUID.fromString(response.jsonPath().getString("questionId"));
    }

    @Test
    @Order(6)
    @DisplayName("Should fail to create question without authentication")
    void testCreateQuestion_WithoutAuth() {
        String requestBody = """
                {
                    "content": "Unauthorized question?",
                    "type": "ESSAY",
                    "points": 10.0
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/quiz/" + createdQuizId + "/question")
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(7)
    @DisplayName("Should fail to create question with missing content")
    void testCreateQuestion_WithMissingContent() {
        String requestBody = """
                {
                    "type": "MULTIPLE_CHOICE",
                    "points": 5.0
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/quiz/" + createdQuizId + "/question")
                .then()
                .log().all()
                .statusCode(400);
    }

    // ==================== GET QUESTION TESTS ====================

    @Test
    @Order(8)
    @DisplayName("Should successfully get a question by ID")
    void testGetQuestion_ShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz/" + createdQuizId + "/question/" + createdQuestionId)
                .then()
                .log().all()
                .statusCode(200)
                .body("questionId", equalTo(createdQuestionId.toString()))
                .body("content", equalTo("What is 2 + 2?"))
                .body("type", equalTo("MULTIPLE_CHOICE"))
                .body("points", equalTo(5.0f))
                .body("quizId", equalTo(createdQuizId.toString()))
                .body("answers", notNullValue())
                .body("createdAt", notNullValue());
    }

    @Test
    @Order(9)
    @DisplayName("Should fail to get question without authentication")
    void testGetQuestion_WithoutAuth() {
        given()
                .when()
                .get("/api/" + createdCourseId + "/quiz/" + createdQuizId + "/question/" + createdQuestionId)
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(10)
    @DisplayName("Should fail to get non-existent question")
    void testGetQuestion_WithNonExistentQuestion() {
        UUID nonExistentId = UUID.randomUUID();

        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz/" + createdQuizId + "/question/" + nonExistentId)
                .then()
                .log().all()
                .statusCode(404);
    }

    // ==================== MODIFY QUESTION TESTS ====================

    @Test
    @Order(11)
    @DisplayName("Should successfully modify a question")
    void testModifyQuestion_ShouldSucceed() {
        String requestBody = """
                {
                    "content": "What is 2 + 3?",
                    "type": "MULTIPLE_CHOICE",
                    "points": 8.0
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/quiz/" + createdQuizId + "/question/" + createdQuestionId)
                .then()
                .log().all()
                .statusCode(200)
                .body("questionId", equalTo(createdQuestionId.toString()))
                .body("content", equalTo("What is 2 + 3?"))
                .body("type", equalTo("MULTIPLE_CHOICE"))
                .body("points", equalTo(8.0f))
                .body("quizId", equalTo(createdQuizId.toString()))
                .body("updatedAt", notNullValue());
    }

    @Test
    @Order(12)
    @DisplayName("Should fail to modify question without authentication")
    void testModifyQuestion_WithoutAuth() {
        String requestBody = """
                {
                    "content": "Hacked question",
                    "type": "ESSAY",
                    "points": 100.0
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/quiz/" + createdQuizId + "/question/" + createdQuestionId)
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(13)
    @DisplayName("Should fail to modify non-existent question")
    void testModifyQuestion_WithNonExistentQuestion() {
        UUID nonExistentId = UUID.randomUUID();

        String requestBody = """
                {
                    "content": "Ghost question",
                    "type": "ESSAY",
                    "points": 10.0
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/quiz/" + createdQuizId + "/question/" + nonExistentId)
                .then()
                .log().all()
                .statusCode(404);
    }

    // ==================== DELETE QUESTION TESTS ====================

    @Test
    @Order(14)
    @DisplayName("Should fail to delete question without authentication")
    void testDeleteQuestion_WithoutAuth() {
        given()
                .when()
                .delete("/api/" + createdCourseId + "/quiz/" + createdQuizId + "/question/" + secondQuestionId)
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(15)
    @DisplayName("Should successfully delete a question")
    void testDeleteQuestion_ShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .delete("/api/" + createdCourseId + "/quiz/" + createdQuizId + "/question/" + secondQuestionId)
                .then()
                .log().all()
                .statusCode(200)
                .body("questionId", equalTo(secondQuestionId.toString()))
                .body("message", equalTo("Question deleted successfully"));
    }

    @Test
    @Order(16)
    @DisplayName("Should fail to delete non-existent question")
    void testDeleteQuestion_WithNonExistentQuestion() {
        UUID nonExistentId = UUID.randomUUID();

        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .delete("/api/" + createdCourseId + "/quiz/" + createdQuizId + "/question/" + nonExistentId)
                .then()
                .log().all()
                .statusCode(404);
    }

    // ==================== VERIFY STATE AFTER DELETE ====================

    @Test
    @Order(17)
    @DisplayName("Should verify the first question still exists after deleting the second")
    void testGetQuestion_AfterDelete() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz/" + createdQuizId + "/question/" + createdQuestionId)
                .then()
                .log().all()
                .statusCode(200)
                .body("questionId", equalTo(createdQuestionId.toString()))
                .body("content", equalTo("What is 2 + 3?"));
    }
}
