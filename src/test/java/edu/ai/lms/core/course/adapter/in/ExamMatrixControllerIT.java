package edu.ai.lms.core.course.adapter.in;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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

/**
 * Integration tests for {@link ExamMatrixController}.
 * Tests the full HTTP request-response cycle using a real PostgreSQL container.
 *
 * <p>Test order is important: teacher registration → course creation →
 * learning-outcome creation → exam-matrix CRUD.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Exam Matrix Controller Integration Tests")
class ExamMatrixControllerIT extends IntegrationTestConfiguration {

    private static String teacherToken;
    private static UUID courseId;
    private static UUID learningOutcomeId;
    private static UUID createdMatrixId;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ── Setup ─────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Register teacher for exam matrix tests")
    void testRegisterTeacher() {
        String body = """
                {
                    "firstName": "Matrix",
                    "lastName": "Teacher",
                    "email": "matrix.teacher@example.com",
                    "password": "TeacherPass123!",
                    "employeeId": "EMP-MTX001",
                    "department": "Computer Science",
                    "specialization": "Software Engineering"
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .body(body)
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
    @DisplayName("Create a course for exam matrix tests")
    void testCreateCourse() {
        String start = LocalDate.now().plusDays(1).format(DATE_FMT);
        String end   = LocalDate.now().plusMonths(4).format(DATE_FMT);

        String body = """
                {
                    "courseName": "Exam Matrix Test Course",
                    "description": "Course for testing exam matrices",
                    "courseCode": "EMT-001",
                    "active": true,
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": [
                        {"code": "CLO-1", "description": "Understand algorithms"},
                        {"code": "CLO-2", "description": "Apply data structures"}
                    ]
                }
                """.formatted(start, end);

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(body)
                .when()
                .post("/api/course")
                .then()
                .log().all()
                .statusCode(201)
                .body("courseId", notNullValue())
                .extract().response();

        courseId = UUID.fromString(response.jsonPath().getString("courseId"));
    }

    @Test
    @Order(3)
    @DisplayName("Fetch a learning outcome ID to use in rows")
    void testGetLearningOutcome() {
        Response response = given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/course/{courseId}", courseId)
                .then()
                .log().all()
                .statusCode(200)
                .extract().response();

        // Extract first ungrouped CLO id (may vary per implementation)
        String lo = response.jsonPath().getString("learningOutcomes[0].id");
        if (lo == null) {
            // Try grouped CLO
            lo = response.jsonPath().getString("groupLearningOutcomes[0].learningOutcomes[0].id");
        }
        learningOutcomeId = UUID.fromString(lo);
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("Should create an exam matrix successfully")
    void testCreateExamMatrix() {
        String body = """
                {
                    "title": "Midterm Exam Matrix",
                    "description": "Matrix for mid-semester examination",
                    "rows": [
                        {
                            "learningOutcomeId": "%s",
                            "topic": "Algorithms",
                            "understandCount": 2,
                            "applyCount": 2,
                            "applyPlusCount": 1,
                            "orderIndex": 0
                        }
                    ]
                }
                """.formatted(learningOutcomeId);

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(body)
                .when()
                .post("/api/courses/{courseId}/exam-matrices", courseId)
                .then()
                .log().all()
                .statusCode(201)
                .body("id", notNullValue())
                .body("title", equalTo("Midterm Exam Matrix"))
                .body("rows", hasSize(1))
                .body("rows[0].understandCount", equalTo(2))
                .extract().response();

        createdMatrixId = UUID.fromString(response.jsonPath().getString("id"));
    }

    @Test
    @Order(5)
    @DisplayName("Should fail to create an exam matrix without authentication")
    void testCreateExamMatrix_Unauthenticated_ShouldReturn401() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"title\": \"No auth\", \"rows\": []}")
                .when()
                .post("/api/courses/{courseId}/exam-matrices", courseId)
                .then()
                .log().all()
                .statusCode(401);
    }

    @Test
    @Order(6)
    @DisplayName("Should fail to create an exam matrix with blank title")
    void testCreateExamMatrix_BlankTitle_ShouldReturn400() {
        String body = """
                {
                    "title": "",
                    "rows": []
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(body)
                .when()
                .post("/api/courses/{courseId}/exam-matrices", courseId)
                .then()
                .log().all()
                .statusCode(400);
    }

    // ── Get all ───────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("Should list all exam matrices for a course")
    void testGetAllExamMatrices() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/courses/{courseId}/exam-matrices", courseId)
                .then()
                .log().all()
                .statusCode(200)
                .body("courseId", equalTo(courseId.toString()))
                .body("total", greaterThanOrEqualTo(1))
                .body("matrices", hasSize(greaterThanOrEqualTo(1)));
    }

    // ── Get one ───────────────────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("Should retrieve a specific exam matrix")
    void testGetExamMatrix() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/courses/{courseId}/exam-matrices/{matrixId}", courseId, createdMatrixId)
                .then()
                .log().all()
                .statusCode(200)
                .body("id", equalTo(createdMatrixId.toString()))
                .body("title", equalTo("Midterm Exam Matrix"));
    }

    @Test
    @Order(9)
    @DisplayName("Should return 404 for a non-existent exam matrix")
    void testGetExamMatrix_NotFound_ShouldReturn404() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/courses/{courseId}/exam-matrices/{matrixId}", courseId, UUID.randomUUID())
                .then()
                .log().all()
                .statusCode(404);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("Should update an exam matrix successfully")
    void testModifyExamMatrix() {
        String body = """
                {
                    "title": "Updated Final Matrix",
                    "description": "Updated for final exam",
                    "rows": [
                        {
                            "learningOutcomeId": "%s",
                            "topic": "Data Structures",
                            "understandCount": 3,
                            "applyCount": 3,
                            "applyPlusCount": 2,
                            "orderIndex": 0
                        }
                    ]
                }
                """.formatted(learningOutcomeId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(body)
                .when()
                .put("/api/courses/{courseId}/exam-matrices/{matrixId}", courseId, createdMatrixId)
                .then()
                .log().all()
                .statusCode(200)
                .body("id", equalTo(createdMatrixId.toString()))
                .body("title", equalTo("Updated Final Matrix"))
                .body("rows[0].understandCount", equalTo(3));
    }

    @Test
    @Order(11)
    @DisplayName("Should return 404 when updating a non-existent exam matrix")
    void testModifyExamMatrix_NotFound_ShouldReturn404() {
        String body = """
                {
                    "title": "Ghost Matrix",
                    "rows": []
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(body)
                .when()
                .put("/api/courses/{courseId}/exam-matrices/{matrixId}", courseId, UUID.randomUUID())
                .then()
                .log().all()
                .statusCode(404);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    @Order(12)
    @DisplayName("Should delete an exam matrix successfully")
    void testDeleteExamMatrix() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .delete("/api/courses/{courseId}/exam-matrices/{matrixId}", courseId, createdMatrixId)
                .then()
                .log().all()
                .statusCode(200)
                .body("matrixId", equalTo(createdMatrixId.toString()))
                .body("success", equalTo(true));
    }

    @Test
    @Order(13)
    @DisplayName("Should return 404 after deleting an exam matrix")
    void testGetExamMatrix_AfterDeletion_ShouldReturn404() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/courses/{courseId}/exam-matrices/{matrixId}", courseId, createdMatrixId)
                .then()
                .log().all()
                .statusCode(404);
    }
}
