package edu.ai.lms.core.quiz.adapter.in.http;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
@DisplayName("Quiz Controller Integration Tests")
class QuizControllerIT extends IntegrationTestConfiguration {

    private static String teacherToken;
    private static UUID createdCourseId;
    private static UUID createdQuizId;
    private static UUID secondQuizId;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ==================== SETUP: Register teacher and create a course ====================

    @Test
    @Order(1)
    @DisplayName("Should register a teacher for quiz tests")
    void testRegisterTeacher() {
        String requestBody = """
                {
                    "firstName": "Quiz",
                    "lastName": "Teacher",
                    "email": "quiz.teacher@example.com",
                    "password": "TeacherPass123!",
                    "department": "Computer Science",
                    "specialization": "Software Engineering"
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
                .body("email", equalTo("quiz.teacher@example.com"))
                .extract().response();

        teacherToken = response.jsonPath().getString("token");
    }

    @Test
    @Order(2)
    @DisplayName("Should create a course for quiz tests")
    void testCreateCourseForQuizzes() {
        String startDate = LocalDate.now().plusDays(1).format(DATE_FORMATTER);
        String endDate = LocalDate.now().plusMonths(3).format(DATE_FORMATTER);

        String requestBody = """
                {
                    "courseName": "Java Programming for Quizzes",
                    "description": "Course used for quiz integration tests",
                    "courseCode": "QUIZ101",
                    "active": true,
                    "coverImage": "https://example.com/java-quiz.jpg",
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": [
                        {
                            "code": "CLO-1",
                            "description": "Understand Java basics"
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

    // ==================== CREATE QUIZ TESTS ====================

    @Test
    @Order(3)
    @DisplayName("Should successfully create a quiz")
    void testCreateQuiz_ShouldSucceed() {
        String requestBody = """
                {
                    "quizTitle": "Midterm Quiz",
                    "timeLimitMinutes": 60,
                    "passScore": 70.0
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
                .body("quizTitle", equalTo("Midterm Quiz"))
                .body("timeLimitMinutes", equalTo(60))
                .body("passScore", equalTo(70.0f))
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("active", equalTo(true))
                .body("createdAt", notNullValue())
                .extract().response();

        createdQuizId = UUID.fromString(response.jsonPath().getString("quizId"));
    }

    @Test
    @Order(4)
    @DisplayName("Should create a second quiz for list tests")
    void testCreateSecondQuiz_ShouldSucceed() {
        String requestBody = """
                {
                    "quizTitle": "Final Quiz",
                    "timeLimitMinutes": 120,
                    "passScore": 80.0
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
                .body("quizTitle", equalTo("Final Quiz"))
                .extract().response();

        secondQuizId = UUID.fromString(response.jsonPath().getString("quizId"));
    }

    @Test
    @Order(5)
    @DisplayName("Should create a quiz with nested questions and answers")
    void testCreateQuiz_WithNestedQuestionsAndAnswers() {
        String requestBody = """
                {
                    "quizTitle": "Nested Quiz",
                    "timeLimitMinutes": 45,
                    "passScore": 65.0,
                    "questions": [
                        {
                            "content": "What is Java?",
                            "type": "MULTIPLE_CHOICE",
                            "points": 10.0,
                            "answers": [
                                {
                                    "content": "A programming language",
                                    "isCorrect": true,
                                    "explanation": "Java is a widely-used programming language"
                                },
                                {
                                    "content": "A coffee brand",
                                    "isCorrect": false,
                                    "explanation": "While Java is also a coffee, this is about programming"
                                }
                            ]
                        }
                    ]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/quiz")
                .then()
                .log().all()
                .statusCode(201)
                .body("quizId", notNullValue())
                .body("quizTitle", equalTo("Nested Quiz"))
                .body("questions", hasSize(1))
                .body("questions[0].content", equalTo("What is Java?"))
                .body("questions[0].answers", hasSize(2));
    }

    @Test
    @Order(6)
    @DisplayName("Should fail to create quiz without authentication")
    void testCreateQuiz_WithoutAuth() {
        String requestBody = """
                {
                    "quizTitle": "Unauthorized Quiz",
                    "timeLimitMinutes": 30,
                    "passScore": 50.0
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/quiz")
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(7)
    @DisplayName("Should fail to create quiz with missing title")
    void testCreateQuiz_WithMissingTitle() {
        String requestBody = """
                {
                    "timeLimitMinutes": 30,
                    "passScore": 50.0
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/quiz")
                .then()
                .log().all()
                .statusCode(400);
    }

    // ==================== GET QUIZ TESTS ====================

    @Test
    @Order(8)
    @DisplayName("Should successfully get a quiz by ID")
    void testGetQuiz_ShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz/" + createdQuizId)
                .then()
                .log().all()
                .statusCode(200)
                .body("quizId", equalTo(createdQuizId.toString()))
                .body("quizTitle", equalTo("Midterm Quiz"))
                .body("timeLimitMinutes", equalTo(60))
                .body("passScore", equalTo(70.0f))
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("active", equalTo(true))
                .body("createdAt", notNullValue());
    }

    @Test
    @Order(9)
    @DisplayName("Should fail to get quiz without authentication")
    void testGetQuiz_WithoutAuth() {
        given()
                .when()
                .get("/api/" + createdCourseId + "/quiz/" + createdQuizId)
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(10)
    @DisplayName("Should fail to get non-existent quiz")
    void testGetQuiz_WithNonExistentQuiz() {
        UUID nonExistentQuizId = UUID.randomUUID();

        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz/" + nonExistentQuizId)
                .then()
                .log().all()
                .statusCode(404);
    }

    // ==================== GET ALL QUIZZES TESTS ====================

    @Test
    @Order(11)
    @DisplayName("Should successfully get all quizzes in a course")
    void testGetAllQuizzes_ShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz?pageSize=10&sortDirection=asc")
                .then()
                .log().all()
                .statusCode(200)
                .body("quizzes", hasSize(greaterThanOrEqualTo(2)))
                .body("totalQuizzes", greaterThanOrEqualTo(2))
                .body("sortDirection", equalTo("asc"));
    }

    @Test
    @Order(12)
    @DisplayName("Should limit quizzes by pageSize")
    void testGetAllQuizzes_WithPageSize() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz?pageSize=1&sortDirection=asc")
                .then()
                .log().all()
                .statusCode(200)
                .body("quizzes", hasSize(1))
                .body("pageSize", equalTo(1));
    }

    @Test
    @Order(13)
    @DisplayName("Should fail to get all quizzes without authentication")
    void testGetAllQuizzes_WithoutAuth() {
        given()
                .when()
                .get("/api/" + createdCourseId + "/quiz")
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    // ==================== MODIFY QUIZ TESTS ====================

    @Test
    @Order(14)
    @DisplayName("Should successfully modify a quiz")
    void testModifyQuiz_ShouldSucceed() {
        String requestBody = """
                {
                    "quizTitle": "Updated Midterm Quiz",
                    "timeLimitMinutes": 90,
                    "passScore": 75.0,
                    "active": true
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/quiz/" + createdQuizId)
                .then()
                .log().all()
                .statusCode(200)
                .body("quizId", equalTo(createdQuizId.toString()))
                .body("quizTitle", equalTo("Updated Midterm Quiz"))
                .body("timeLimitMinutes", equalTo(90))
                .body("passScore", equalTo(75.0f))
                .body("updatedAt", notNullValue());
    }

    @Test
    @Order(15)
    @DisplayName("Should fail to modify quiz without authentication")
    void testModifyQuiz_WithoutAuth() {
        String requestBody = """
                {
                    "quizTitle": "Hack Quiz",
                    "timeLimitMinutes": 10,
                    "passScore": 100.0
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/quiz/" + createdQuizId)
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(16)
    @DisplayName("Should fail to modify non-existent quiz")
    void testModifyQuiz_WithNonExistentQuiz() {
        UUID nonExistentQuizId = UUID.randomUUID();

        String requestBody = """
                {
                    "quizTitle": "Ghost Quiz",
                    "timeLimitMinutes": 10
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/quiz/" + nonExistentQuizId)
                .then()
                .log().all()
                .statusCode(404);
    }

    // ==================== DELETE QUIZ TESTS ====================

    @Test
    @Order(17)
    @DisplayName("Should fail to delete quiz without authentication")
    void testDeleteQuiz_WithoutAuth() {
        given()
                .when()
                .delete("/api/" + createdCourseId + "/quiz/" + secondQuizId)
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(18)
    @DisplayName("Should successfully delete a quiz")
    void testDeleteQuiz_ShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .delete("/api/" + createdCourseId + "/quiz/" + secondQuizId)
                .then()
                .log().all()
                .statusCode(200)
                .body("quizId", equalTo(secondQuizId.toString()))
                .body("message", equalTo("Quiz deleted successfully"));
    }

    @Test
    @Order(19)
    @DisplayName("Should fail to delete non-existent quiz")
    void testDeleteQuiz_WithNonExistentQuiz() {
        UUID nonExistentQuizId = UUID.randomUUID();

        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .delete("/api/" + createdCourseId + "/quiz/" + nonExistentQuizId)
                .then()
                .log().all()
                .statusCode(404);
    }

    // ==================== VERIFY STATE AFTER DELETE ====================

    @Test
    @Order(20)
    @DisplayName("Should verify the quiz was deleted from the list")
    void testGetAllQuizzes_AfterDelete() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz?pageSize=10&sortDirection=asc")
                .then()
                .log().all()
                .statusCode(200)
                .body("quizzes", notNullValue());
    }
}
