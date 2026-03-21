package edu.ai.lms.core.quiz.adapter.in.http;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
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
@DisplayName("Quiz Learning Outcome Controller Integration Tests")
class QuizLearningOutcomeControllerIT extends IntegrationTestConfiguration {

    private static String teacherToken;
    private static UUID createdCourseId;
    private static UUID createdQuizId;
    private static UUID lo1;
    private static UUID lo2;
    private static UUID lo3;
    private static UUID lo4;
    private static UUID group1;
    private static UUID group2;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Test
    @Order(1)
    @DisplayName("Should register teacher")
    void testRegisterTeacher() {
        String requestBody = """
                {
                    "firstName": "QuizLO",
                    "lastName": "Teacher",
                    "email": "quiz.lo.teacher@example.com",
                    "password": "TeacherPass123!",
                    "employeeId": "EMP-QL001",
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
                .statusCode(201)
                .body("token", notNullValue())
                .extract().response();

        teacherToken = response.jsonPath().getString("token");
    }

    @Test
    @Order(2)
    @DisplayName("Should create course")
    void testCreateCourse() {
        String startDate = LocalDate.now().plusDays(1).format(DATE_FORMATTER);
        String endDate = LocalDate.now().plusMonths(3).format(DATE_FORMATTER);

        String requestBody = """
                {
                    "courseName": "Quiz LO Course",
                    "description": "Course for quiz-learning-outcome IT",
                    "courseCode": "QLO101",
                    "active": true,
                    "coverImage": "https://example.com/qlo.jpg",
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": []
                }
                """.formatted(startDate, endDate);

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/course")
                .then()
                .statusCode(201)
                .body("courseId", notNullValue())
                .extract().response();

        createdCourseId = UUID.fromString(response.jsonPath().getString("courseId"));
    }

    @Test
    @Order(3)
    @DisplayName("Should create learning outcomes")
    void testCreateLearningOutcomes() {
        String requestBody = """
                {
                    "learningOutcomes": [
                        {"code": "CLO-Q1", "description": "Understand fundamentals"},
                        {"code": "CLO-Q2", "description": "Apply fundamentals"},
                        {"code": "CLO-Q3", "description": "Analyze scenarios"},
                        {"code": "CLO-Q4", "description": "Evaluate solutions"}
                    ]
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .statusCode(201)
                .body("totalCreated", equalTo(4))
                .extract().response();

        lo1 = UUID.fromString(response.jsonPath().getString("learningOutcomes[0].id"));
        lo2 = UUID.fromString(response.jsonPath().getString("learningOutcomes[1].id"));
        lo3 = UUID.fromString(response.jsonPath().getString("learningOutcomes[2].id"));
        lo4 = UUID.fromString(response.jsonPath().getString("learningOutcomes[3].id"));
    }

    @Test
    @Order(4)
    @DisplayName("Should create group one with two learning outcomes")
    void testCreateGroupOne() {
        String requestBody = """
                {
                    "name": "Group One",
                    "description": "First group",
                    "learningOutcomeIds": ["%s", "%s"]
                }
                """.formatted(lo1, lo2);

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/group-learning-outcome")
                .then()
                .statusCode(201)
                .extract().response();

        group1 = UUID.fromString(response.jsonPath().getString("id"));
    }

    @Test
    @Order(5)
    @DisplayName("Should create group two with one learning outcome")
    void testCreateGroupTwo() {
        String requestBody = """
                {
                    "name": "Group Two",
                    "description": "Second group",
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(lo3);

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/group-learning-outcome")
                .then()
                .statusCode(201)
                .extract().response();

        group2 = UUID.fromString(response.jsonPath().getString("id"));
    }

    @Test
    @Order(6)
    @DisplayName("Should create quiz")
    void testCreateQuiz() {
        String requestBody = """
                {
                    "quizTitle": "Quiz for LO Mapping",
                    "timeLimitMinutes": 60,
                    "passScore": 70.0,
                    "startTime": "20-03-2026 09:00:00",
                    "dueTime": "20-03-2026 10:00:00",
                    "attemptLimited": 1,
                    "display": true
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/quiz")
                .then()
                .statusCode(201)
                .extract().response();

        createdQuizId = UUID.fromString(response.jsonPath().getString("quizId"));
    }

    @Test
    @Order(7)
    @DisplayName("Should add direct and grouped learning outcomes with overlap deduplicated")
    void testAddLearningOutcomesToQuiz_Success() {
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s", "%s"],
                    "groupLearningOutcomeIds": ["%s", "%s"]
                }
                """.formatted(lo2, lo4, group1, group2);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/quiz/" + createdQuizId + "/learning-outcomes")
                .then()
                .log().all()
                .statusCode(200)
                .body("operation", equalTo("ADD"))
                .body("totalAffected", equalTo(4))
                .body("learningOutcomeIds", hasSize(4));
    }

    @Test
    @Order(8)
    @DisplayName("Should remove learning outcomes from quiz using direct and grouped ids")
    void testRemoveLearningOutcomesFromQuiz_Success() {
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"],
                    "groupLearningOutcomeIds": ["%s"]
                }
                """.formatted(lo1, group2);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .delete("/api/" + createdCourseId + "/quiz/" + createdQuizId + "/learning-outcomes")
                .then()
                .log().all()
                .statusCode(200)
                .body("operation", equalTo("REMOVE"))
                .body("totalAffected", equalTo(2))
                .body("learningOutcomeIds", hasSize(2));
    }
}
