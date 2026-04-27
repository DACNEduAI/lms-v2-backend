package edu.ai.lms.core.quiz.adapter.in.http;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
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
@DisplayName("Answer Controller Integration Tests")
class AnswerControllerIT extends IntegrationTestConfiguration {

    private static String teacherToken;
    private static UUID createdCourseId;
    private static UUID createdQuizId;
    private static UUID createdQuestionId;
    private static Long createdAnswerNumber;
    private static Long secondAnswerNumber;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ==================== SETUP ====================

    @Test
    @Order(1)
    @DisplayName("Should register a teacher for answer tests")
    void testRegisterTeacher() {
        String requestBody = """
                {
                    "firstName": "Answer",
                    "lastName": "Teacher",
                    "email": "answer.teacher@example.com",
                    "password": "TeacherPass123!",
                    "employeeId": "EMP-ANS001",
                    "department": "Science",
                    "specialization": "Physics"
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
    @DisplayName("Should create a course for answer tests")
    void testCreateCourse() {
        String startDate = LocalDate.now().plusDays(1).format(DATE_FORMATTER);
        String endDate = LocalDate.now().plusMonths(3).format(DATE_FORMATTER);

        String requestBody = """
                {
                    "courseName": "Physics for Answer Tests",
                    "description": "Course used for answer integration tests",
                    "courseCode": "ANSW101",
                    "active": true,
                    "coverImage": "https://example.com/physics.jpg",
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": [
                        {
                            "code": "CLO-1",
                            "description": "Understand basic physics"
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
    @DisplayName("Should create a quiz for answer tests")
    void testCreateQuiz() {
        String requestBody = """
                {
                    "quizTitle": "Physics Quiz",
                    "unlimited": true,
                    "passScore": 50.0
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

    @Test
    @Order(4)
    @DisplayName("Should create a question for answer tests")
    void testCreateQuestion() {
        String requestBody = """
                {
                    "questions": [
                    {
                        "content": "What is the speed of light?",
                        "type": "MULTIPLE_CHOICE",
                        "points": 10.0
                    }
                    ]
                }
                """;

        Response response = given()
            .contentType(ContentType.MULTIPART)
            .multiPart("question", "question.json", requestBody, "application/json")
            .header("Authorization", "Bearer " + teacherToken)
            .when()
            .post("/api/quiz/" + createdQuizId + "/question")
                .then()
                .log().all()
                .statusCode(201)
                .body("[0].questionId", notNullValue())
                .extract().response();

        createdQuestionId = UUID.fromString(response.jsonPath().getString("[0].questionId"));
    }

    // ==================== CREATE ANSWER TESTS ====================

    @Test
    @Order(5)
    @DisplayName("Should successfully create an answer")
    void testCreateAnswer_ShouldSucceed() {
        String requestBody = """
                {
                    "content": "299,792,458 m/s",
                    "isCorrect": true,
                    "explanation": "The speed of light in vacuum"
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/quiz/" + createdQuestionId + "/answer")
                .then()
                .log().all()
                .statusCode(201)
                .body("answerNumber", notNullValue())
                .body("content", equalTo("299,792,458 m/s"))
                .body("explanation", equalTo("The speed of light in vacuum"))
                .body("questionId", equalTo(createdQuestionId.toString()))
                .body("createdAt", notNullValue())
                .extract().response();

        createdAnswerNumber = response.jsonPath().getLong("answerNumber");
    }

    @Test
    @Order(6)
    @DisplayName("Should create a second answer")
    void testCreateSecondAnswer_ShouldSucceed() {
        String requestBody = """
                {
                    "content": "150,000 km/s",
                    "isCorrect": false,
                    "explanation": "This is approximately half the speed of light"
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/quiz/" + createdQuestionId + "/answer")
                .then()
                .log().all()
                .statusCode(201)
                .body("answerNumber", notNullValue())
                .body("content", equalTo("150,000 km/s"))
                .extract().response();

        secondAnswerNumber = response.jsonPath().getLong("answerNumber");
    }

    @Test
    @Order(7)
    @DisplayName("Should fail to create answer without authentication")
    void testCreateAnswer_WithoutAuth() {
        String requestBody = """
                {
                    "content": "Unauthorized answer",
                    "isCorrect": false,
                    "explanation": "Should not be created"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/quiz/" + createdQuestionId + "/answer")
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(8)
    @DisplayName("Should fail to create answer with missing content")
    void testCreateAnswer_WithMissingContent() {
        String requestBody = """
                {
                    "isCorrect": true,
                    "explanation": "No content provided"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/quiz/" + createdQuestionId + "/answer")
                .then()
                .log().all()
                .statusCode(400);
    }

    // ==================== VERIFY ANSWERS VIA GET QUESTION ====================

    @Test
    @Order(9)
    @DisplayName("Should verify answers exist via get question endpoint")
    void testGetQuestion_ShouldShowAnswers() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/quiz/" + createdQuizId + "/question/" + createdQuestionId)
                .then()
                .log().all()
                .statusCode(200)
                .body("questionId", equalTo(createdQuestionId.toString()))
                .body("answers", notNullValue());
    }

    // ==================== MODIFY ANSWER TESTS ====================

    @Test
    @Order(10)
    @DisplayName("Should successfully modify an answer")
    void testModifyAnswer_ShouldSucceed() {
        String requestBody = """
                {
                    "content": "Approximately 300,000 km/s",
                    "isCorrect": true,
                    "explanation": "Updated: The speed of light is about 3x10^8 m/s"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/quiz/" + createdQuestionId + "/answer/" + createdAnswerNumber)
                .then()
                .log().all()
                .statusCode(200)
                .body("answerNumber", equalTo(createdAnswerNumber.intValue()))
                .body("content", equalTo("Approximately 300,000 km/s"))
                .body("explanation", equalTo("Updated: The speed of light is about 3x10^8 m/s"))
                .body("questionId", equalTo(createdQuestionId.toString()))
                .body("updatedAt", notNullValue());
    }

    @Test
    @Order(11)
    @DisplayName("Should fail to modify answer without authentication")
    void testModifyAnswer_WithoutAuth() {
        String requestBody = """
                {
                    "content": "Hacked answer",
                    "isCorrect": true
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/quiz/" + createdQuestionId + "/answer/" + createdAnswerNumber)
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(12)
    @DisplayName("Should fail to modify answer with non-existent answer number")
    void testModifyAnswer_WithNonExistentAnswerNumber() {
        String requestBody = """
                {
                    "content": "Ghost answer",
                    "isCorrect": false
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/quiz/" + createdQuestionId + "/answer/99999")
                .then()
                .log().all()
                .statusCode(404);
    }

    // ==================== DELETE ANSWER TESTS ====================

    @Test
    @Order(13)
    @DisplayName("Should fail to delete answer without authentication")
    void testDeleteAnswer_WithoutAuth() {
        given()
                .when()
                .delete("/api/quiz/" + createdQuestionId + "/answer/" + secondAnswerNumber)
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(14)
    @DisplayName("Should successfully delete an answer")
    void testDeleteAnswer_ShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .delete("/api/quiz/" + createdQuestionId + "/answer/" + secondAnswerNumber)
                .then()
                .log().all()
                .statusCode(200)
                .body("answerNumber", equalTo(secondAnswerNumber.intValue()))
                .body("questionId", equalTo(createdQuestionId.toString()))
                .body("message", equalTo("Answer deleted successfully"));
    }

    @Test
    @Order(15)
    @DisplayName("Should fail to delete non-existent answer")
    void testDeleteAnswer_WithNonExistentAnswerNumber() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .delete("/api/quiz/" + createdQuestionId + "/answer/99999")
                .then()
                .log().all()
                .statusCode(404);
    }

    // ==================== VERIFY STATE AFTER DELETE ====================

    @Test
    @Order(16)
    @DisplayName("Should verify the first answer still exists after deleting the second")
    void testGetQuestion_AfterAnswerDelete() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/quiz/" + createdQuizId + "/question/" + createdQuestionId)
                .then()
                .log().all()
                .statusCode(200)
                .body("questionId", equalTo(createdQuestionId.toString()))
                .body("answers", notNullValue());
    }
}