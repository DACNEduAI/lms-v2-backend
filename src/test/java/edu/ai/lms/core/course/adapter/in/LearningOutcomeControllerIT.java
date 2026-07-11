package edu.ai.lms.core.course.adapter.in;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
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
@DisplayName("Learning Outcome Controller Integration Tests")
class LearningOutcomeControllerIT extends IntegrationTestConfiguration {

    private static String teacherToken;
    private static String anotherTeacherToken;
    private static UUID createdCourseId;
    private static UUID createdLoId1;
    private static UUID createdLoId2;
    private static UUID createdLoId3;
    private static UUID createdChapterId1;
    private static UUID createdChapterId2;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ==================== SETUP: Register teacher and create a course ====================

    @Test
    @Order(1)
    @DisplayName("Should register a teacher for CLO tests")
    void testRegisterTeacher() {
        String requestBody = """
                {
                    "firstName": "CLO",
                    "lastName": "Teacher",
                    "email": "clo.teacher@example.com",
                    "password": "TeacherPass123!",
                    "employeeId": "EMP-CLO001",
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
                .body("email", equalTo("clo.teacher@example.com"))
                .extract().response();

        teacherToken = response.jsonPath().getString("token");
    }

    @Test
    @Order(2)
    @DisplayName("Should create a course for CLO tests")
    void testCreateCourseForCLO() {
        String startDate = LocalDate.now().plusDays(1).format(DATE_FORMATTER);
        String endDate = LocalDate.now().plusMonths(3).format(DATE_FORMATTER);

        String requestBody = """
                {
                    "courseName": "Java Programming for CLO Tests",
                    "description": "Course used for CLO integration tests",
                    "courseCode": "CLO101",
                    "active": true,
                    "coverImage": "https://example.com/java-clo.jpg",
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
                .log().all()
                .statusCode(201)
                .body("courseId", notNullValue())
                .extract().response();

        createdCourseId = UUID.fromString(response.jsonPath().getString("courseId"));
    }

    // ==================== CREATE LEARNING OUTCOMES TESTS ====================

    @Test
    @Order(3)
    @DisplayName("Should successfully create multiple learning outcomes")
    void testCreateLearningOutcomes_WithValidDetails() {
        String requestBody = """
                {
                    "learningOutcomes": [
                        {
                            "prefixNumber": 1,
                            "description": "Understand basic Java syntax"
                        },
                        {
                            "prefixNumber": 2,
                            "description": "Write simple Java programs"
                        },
                        {
                            "prefixNumber": 3,
                            "description": "Apply OOP concepts"
                        }
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
                .log().all()
                .statusCode(201)
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("totalCreated", equalTo(3))
                .body("learningOutcomes", hasSize(3))
                .body("learningOutcomes[0].prefixNumber", equalTo(1))
                .body("learningOutcomes[0].description", equalTo("Understand basic Java syntax"))
                .body("learningOutcomes[0].id", notNullValue())
                .body("learningOutcomes[0].createdAt", notNullValue())
                .body("learningOutcomes[1].prefixNumber", equalTo(2))
                .body("learningOutcomes[2].prefixNumber", equalTo(3))
                .extract().response();

        createdLoId1 = UUID.fromString(response.jsonPath().getString("learningOutcomes[0].id"));
        createdLoId2 = UUID.fromString(response.jsonPath().getString("learningOutcomes[1].id"));
        createdLoId3 = UUID.fromString(response.jsonPath().getString("learningOutcomes[2].id"));
    }

    @Test
    @Order(4)
    @DisplayName("Should successfully create a single learning outcome")
    void testCreateLearningOutcomes_WithSingleItem() {
        String requestBody = """
                {
                    "learningOutcomes": [
                        {
                            "prefixNumber": 4,
                            "description": "Debug Java applications"
                        }
                    ]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(201)
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("totalCreated", equalTo(1))
                .body("learningOutcomes", hasSize(1))
                .body("learningOutcomes[0].prefixNumber", equalTo(4));
    }

    @Test
    @Order(5)
    @DisplayName("Should fail to create learning outcomes without authentication")
    void testCreateLearningOutcomes_WithoutAuth() {
        String requestBody = """
                {
                    "learningOutcomes": [
                        {
                            "prefixNumber": 99,
                            "description": "Unauthorized CLO"
                        }
                    ]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(6)
    @DisplayName("Should fail to create learning outcomes with empty list")
    void testCreateLearningOutcomes_WithEmptyList() {
        String requestBody = """
                {
                    "learningOutcomes": []
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(400);
    }

    @Test
    @Order(7)
    @DisplayName("Should fail to create learning outcomes with missing description")
    void testCreateLearningOutcomes_WithBlankCode() {
        String requestBody = """
                {
                    "learningOutcomes": [
                        {
                            "prefixNumber": 99
                        }
                    ]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(400);
    }

    @Test
    @Order(8)
    @DisplayName("Should fail to create learning outcomes with blank description")
    void testCreateLearningOutcomes_WithBlankDescription() {
        String requestBody = """
                {
                    "learningOutcomes": [
                        {
                            "prefixNumber": 99,
                            "description": ""
                        }
                    ]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(400);
    }

    @Test
    @Order(9)
    @DisplayName("Should fail to create learning outcomes for non-existent course")
    void testCreateLearningOutcomes_WithNonExistentCourse() {
        UUID nonExistentCourseId = UUID.randomUUID();

        String requestBody = """
                {
                    "learningOutcomes": [
                        {
                            "prefixNumber": 99,
                            "description": "Orphan CLO"
                        }
                    ]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + nonExistentCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @Order(10)
    @DisplayName("Should fail to create learning outcomes by unauthorized teacher")
    void testCreateLearningOutcomes_ByUnauthorizedTeacher() {
        // Register another teacher
        String registerBody = """
                {
                    "firstName": "Another",
                    "lastName": "CLOTeacher",
                    "email": "another.clo.teacher@example.com",
                    "password": "AnotherPass123!",
                    "employeeId": "EMP-CLO002",
                    "department": "Mathematics",
                    "specialization": "Algebra"
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .body(registerBody)
                .when()
                .post("/api/auth/teacher/register")
                .then()
                .log().all()
                .statusCode(201)
                .extract().response();

        anotherTeacherToken = response.jsonPath().getString("token");

        String requestBody = """
                {
                    "learningOutcomes": [
                        {
                            "prefixNumber": 99,
                            "description": "Unauthorized CLO"
                        }
                    ]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + anotherTeacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(403);
    }

    // ==================== MODIFY SINGLE LEARNING OUTCOME TESTS ====================

    @Test
    @Order(11)
    @DisplayName("Should successfully modify a single learning outcome")
    void testModifySingleLearningOutcome_WithValidDetails() {
        String requestBody = """
                {
                    "prefixNumber": 1,
                    "description": "Updated: Understand basic Java syntax and semantics"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/learning-outcome/" + createdLoId1)
                .then()
                .log().all()
                .statusCode(200)
                .body("id", equalTo(createdLoId1.toString()))
                .body("prefixNumber", equalTo(1))
                .body("description", equalTo("Updated: Understand basic Java syntax and semantics"))
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("updatedAt", notNullValue());
    }

    @Test
    @Order(12)
    @DisplayName("Should fail to modify learning outcome without authentication")
    void testModifySingleLearningOutcome_WithoutAuth() {
        String requestBody = """
                {
                    "prefixNumber": 99,
                    "description": "No auth update"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/learning-outcome/" + createdLoId1)
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(13)
    @DisplayName("Should fail to modify non-existent learning outcome")
    void testModifySingleLearningOutcome_WithNonExistentLo() {
        UUID nonExistentLoId = UUID.randomUUID();

        String requestBody = """
                {
                    "prefixNumber": 99,
                    "description": "Ghost CLO"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/learning-outcome/" + nonExistentLoId)
                .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @Order(14)
    @DisplayName("Should fail to modify learning outcome by unauthorized teacher")
    void testModifySingleLearningOutcome_ByUnauthorizedTeacher() {
        String requestBody = """
                {
                    "prefixNumber": 99,
                    "description": "Hijacked update"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + anotherTeacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/learning-outcome/" + createdLoId1)
                .then()
                .log().all()
                .statusCode(403);
    }

    @Test
    @Order(15)
    @DisplayName("Should fail to modify learning outcome for non-existent course")
    void testModifySingleLearningOutcome_WithNonExistentCourse() {
        UUID nonExistentCourseId = UUID.randomUUID();

        String requestBody = """
                {
                    "prefixNumber": 99,
                    "description": "Orphan course update"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + nonExistentCourseId + "/learning-outcome/" + createdLoId1)
                .then()
                .log().all()
                .statusCode(404);
    }

    // ==================== MODIFY BATCH LEARNING OUTCOMES TESTS ====================

    @Test
    @Order(16)
    @DisplayName("Should successfully modify multiple learning outcomes in batch")
    void testModifyBatchLearningOutcomes_WithValidDetails() {
        String requestBody = """
                {
                    "learningOutcomes": [
                        {
                            "id": "%s",
                            "prefixNumber": 2,
                            "description": "Batch updated: Write Java programs"
                        },
                        {
                            "id": "%s",
                            "prefixNumber": 3,
                            "description": "Batch updated: Apply OOP"
                        }
                    ]
                }
                """.formatted(createdLoId2, createdLoId3);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(200)
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("totalModified", equalTo(2))
                .body("learningOutcomes", hasSize(2))
                .body("learningOutcomes[0].prefixNumber", equalTo(2))
                .body("learningOutcomes[0].description", equalTo("Batch updated: Write Java programs"))
                .body("learningOutcomes[0].updatedAt", notNullValue())
                .body("learningOutcomes[1].prefixNumber", equalTo(3));
    }

    @Test
    @Order(17)
    @DisplayName("Should fail to batch modify learning outcomes without authentication")
    void testModifyBatchLearningOutcomes_WithoutAuth() {
        String requestBody = """
                {
                    "learningOutcomes": [
                        {
                            "id": "%s",
                            "prefixNumber": 99,
                            "description": "No auth"
                        }
                    ]
                }
                """.formatted(createdLoId2);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(18)
    @DisplayName("Should fail to batch modify learning outcomes with empty list")
    void testModifyBatchLearningOutcomes_WithEmptyList() {
        String requestBody = """
                {
                    "learningOutcomes": []
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(400);
    }

    @Test
    @Order(19)
    @DisplayName("Should fail to batch modify with non-existent learning outcome ID")
    void testModifyBatchLearningOutcomes_WithNonExistentLoId() {
        UUID nonExistentLoId = UUID.randomUUID();

        String requestBody = """
                {
                    "learningOutcomes": [
                        {
                            "id": "%s",
                            "prefixNumber": 99,
                            "description": "Ghost CLO"
                        }
                    ]
                }
                """.formatted(nonExistentLoId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @Order(20)
    @DisplayName("Should fail to batch modify learning outcomes by unauthorized teacher")
    void testModifyBatchLearningOutcomes_ByUnauthorizedTeacher() {
        String requestBody = """
                {
                    "learningOutcomes": [
                        {
                            "id": "%s",
                            "prefixNumber": 99,
                            "description": "Hijacked batch"
                        }
                    ]
                }
                """.formatted(createdLoId2);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + anotherTeacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(403);
    }

    @Test
    @Order(21)
    @DisplayName("Should fail to batch modify with missing learning outcome ID")
    void testModifyBatchLearningOutcomes_WithMissingLoId() {
        String requestBody = """
                {
                    "learningOutcomes": [
                        {
                            "prefixNumber": 99,
                            "description": "Missing ID"
                        }
                    ]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(400);
    }

    // ==================== DELETE LEARNING OUTCOMES TESTS ====================

    @Test
    @Order(22)
    @DisplayName("Should fail to delete learning outcomes without authentication")
    void testDeleteLearningOutcomes_WithoutAuth() {
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId3);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .delete("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(23)
    @DisplayName("Should fail to delete learning outcomes with empty IDs list")
    void testDeleteLearningOutcomes_WithEmptyList() {
        String requestBody = """
                {
                    "learningOutcomeIds": []
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .delete("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(400);
    }

    @Test
    @Order(24)
    @DisplayName("Should fail to delete learning outcomes with non-existent ID")
    void testDeleteLearningOutcomes_WithNonExistentId() {
        UUID nonExistentLoId = UUID.randomUUID();

        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(nonExistentLoId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .delete("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @Order(25)
    @DisplayName("Should fail to delete learning outcomes by unauthorized teacher")
    void testDeleteLearningOutcomes_ByUnauthorizedTeacher() {
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId3);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + anotherTeacherToken)
                .body(requestBody)
                .when()
                .delete("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(403);
    }

    @Test
    @Order(26)
    @DisplayName("Should fail to delete learning outcomes for non-existent course")
    void testDeleteLearningOutcomes_WithNonExistentCourse() {
        UUID nonExistentCourseId = UUID.randomUUID();

        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId3);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .delete("/api/" + nonExistentCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @Order(27)
    @DisplayName("Should successfully delete a single learning outcome")
    void testDeleteLearningOutcomes_SingleItem() {
        // First create a CLO specifically for deletion
        String createBody = """
                {
                    "learningOutcomes": [
                        {
                            "prefixNumber": 99,
                            "description": "To be deleted"
                        }
                    ]
                }
                """;

        Response createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(createBody)
                .when()
                .post("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(201)
                .extract().response();

        UUID loToDelete = UUID.fromString(createResponse.jsonPath().getString("learningOutcomes[0].id"));

        // Now delete it
        String deleteBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(loToDelete);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(deleteBody)
                .when()
                .delete("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(200)
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("totalDeleted", equalTo(1))
                .body("deletedIds", hasSize(1))
                .body("deletedIds[0]", equalTo(loToDelete.toString()))
                .body("message", equalTo("Learning outcomes deleted successfully"));
    }

    @Test
    @Order(28)
    @DisplayName("Should successfully delete multiple learning outcomes")
    void testDeleteLearningOutcomes_MultipleItems() {
        // Create two CLOs for deletion
        String createBody = """
                {
                    "learningOutcomes": [
                        {
                            "prefixNumber": 99,
                            "description": "Batch delete A"
                        },
                        {
                            "prefixNumber": 99,
                            "description": "Batch delete B"
                        }
                    ]
                }
                """;

        Response createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(createBody)
                .when()
                .post("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(201)
                .extract().response();

        UUID delId1 = UUID.fromString(createResponse.jsonPath().getString("learningOutcomes[0].id"));
        UUID delId2 = UUID.fromString(createResponse.jsonPath().getString("learningOutcomes[1].id"));

        // Delete both
        String deleteBody = """
                {
                    "learningOutcomeIds": ["%s", "%s"]
                }
                """.formatted(delId1, delId2);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(deleteBody)
                .when()
                .delete("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(200)
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("totalDeleted", equalTo(2))
                .body("deletedIds", hasSize(2))
                .body("message", equalTo("Learning outcomes deleted successfully"));
    }

    @Test
    @Order(29)
    @DisplayName("Should fail to delete already deleted learning outcome")
    void testDeleteLearningOutcomes_AlreadyDeleted() {
        // Create and immediately delete a CLO
        String createBody = """
                {
                    "learningOutcomes": [
                        {
                            "prefixNumber": 99,
                            "description": "Temporary"
                        }
                    ]
                }
                """;

        Response createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(createBody)
                .when()
                .post("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .statusCode(201)
                .extract().response();

        UUID tempId = UUID.fromString(createResponse.jsonPath().getString("learningOutcomes[0].id"));

        // Delete it
        String deleteBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(tempId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(deleteBody)
                .when()
                .delete("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .statusCode(200);

        // Try to delete again — should fail with 404
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(deleteBody)
                .when()
                .delete("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(404);
    }

    // ==================== MALFORMED REQUEST TESTS ====================

    @Test
    @Order(30)
    @DisplayName("Should fail to create learning outcomes with malformed JSON")
    void testCreateLearningOutcomes_WithMalformedJson() {
        String malformedBody = "{ invalid json }";

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(malformedBody)
                .when()
                .post("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(400);
    }

    @Test
    @Order(31)
    @DisplayName("Should fail to batch modify with malformed JSON")
    void testModifyBatchLearningOutcomes_WithMalformedJson() {
        String malformedBody = "{ invalid json }";

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(malformedBody)
                .when()
                .put("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(400);
    }

    // ==================== CHAPTER-LEVEL: SETUP ====================

    @Test
    @Order(32)
    @DisplayName("Should create chapters for chapter-LO tests")
    void testCreateChaptersForChapterLOTests() {
        String requestBody1 = """
                {
                    "title": "Chapter 1: Introduction",
                    "description": "Introduction to Java programming"
                }
                """;

        Response response1 = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody1)
                .when()
                .post("/api/" + createdCourseId + "/chapter")
                .then()
                .log().all()
                .statusCode(201)
                .extract().response();

        createdChapterId1 = UUID.fromString(response1.jsonPath().getString("chapterId"));

        String requestBody2 = """
                {
                    "title": "Chapter 2: Variables",
                    "description": "Understanding variables in Java"
                }
                """;

        Response response2 = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody2)
                .when()
                .post("/api/" + createdCourseId + "/chapter")
                .then()
                .log().all()
                .statusCode(201)
                .extract().response();

        createdChapterId2 = UUID.fromString(response2.jsonPath().getString("chapterId"));
    }

    // ==================== CHAPTER-LEVEL: ADD LEARNING OUTCOMES TO CHAPTER ====================

    @Test
    @Order(33)
    @DisplayName("Should successfully add learning outcomes to a chapter")
    void testAddLearningOutcomesToChapter_Success() {
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s", "%s"]
                }
                """.formatted(createdLoId1, createdLoId2);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/chapter/" + createdChapterId1 + "/learning-outcomes/add")
                .then()
                .log().all()
                .statusCode(200)
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("chapterId", equalTo(createdChapterId1.toString()))
                .body("totalAdded", equalTo(2))
                .body("addedLearningOutcomeIds", hasSize(2))
                .body("message", equalTo("Learning outcomes added to chapter successfully"));
    }

    @Test
    @Order(34)
    @DisplayName("Should skip duplicate learning outcomes when adding again")
    void testAddLearningOutcomesToChapter_SkipDuplicates() {
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s", "%s"]
                }
                """.formatted(createdLoId1, createdLoId3);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/chapter/" + createdChapterId1 + "/learning-outcomes/add")
                .then()
                .log().all()
                .statusCode(200)
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("chapterId", equalTo(createdChapterId1.toString()))
                .body("totalAdded", equalTo(1))
                .body("addedLearningOutcomeIds", hasSize(1))
                .body("addedLearningOutcomeIds", hasItem(createdLoId3.toString()));
    }

    @Test
    @Order(35)
    @DisplayName("Should verify chapter now returns learning outcome IDs")
    void testGetChapter_ReturnsLearningOutcomeIds() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/chapter/" + createdChapterId1)
                .then()
                .log().all()
                .statusCode(200)
                .body("chapterId", equalTo(createdChapterId1.toString()))
                .body("learningOutcomeIds", notNullValue())
                .body("learningOutcomeIds", hasSize(3))
                .body("learningOutcomeIds", hasItem(createdLoId1.toString()))
                .body("learningOutcomeIds", hasItem(createdLoId2.toString()))
                .body("learningOutcomeIds", hasItem(createdLoId3.toString()));
    }

    @Test
    @Order(36)
    @DisplayName("Should verify chapter without LOs returns empty list")
    void testGetChapter_WithoutLOs_ReturnsEmptyList() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/chapter/" + createdChapterId2)
                .then()
                .log().all()
                .statusCode(200)
                .body("chapterId", equalTo(createdChapterId2.toString()))
                .body("learningOutcomeIds", notNullValue())
                .body("learningOutcomeIds", hasSize(0));
    }

    @Test
    @Order(37)
    @DisplayName("Should fail to add learning outcomes to chapter without authentication")
    void testAddLearningOutcomesToChapter_WithoutAuth() {
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId1);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/chapter/" + createdChapterId1 + "/learning-outcomes/add")
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(38)
    @DisplayName("Should fail to add learning outcomes to chapter with empty list")
    void testAddLearningOutcomesToChapter_EmptyList() {
        String requestBody = """
                {
                    "learningOutcomeIds": []
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/chapter/" + createdChapterId1 + "/learning-outcomes/add")
                .then()
                .log().all()
                .statusCode(400);
    }

    @Test
    @Order(39)
    @DisplayName("Should fail to add learning outcomes to chapter of non-existent course")
    void testAddLearningOutcomesToChapter_CourseNotFound() {
        UUID nonExistentCourseId = UUID.randomUUID();

        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId1);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + nonExistentCourseId + "/chapter/" + createdChapterId1 + "/learning-outcomes/add")
                .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @Order(40)
    @DisplayName("Should fail to add learning outcomes to non-existent chapter")
    void testAddLearningOutcomesToChapter_ChapterNotFound() {
        UUID nonExistentChapterId = UUID.randomUUID();

        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId1);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/chapter/" + nonExistentChapterId + "/learning-outcomes/add")
                .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @Order(41)
    @DisplayName("Should fail to add learning outcomes to chapter by unauthorized teacher")
    void testAddLearningOutcomesToChapter_UnauthorizedTeacher() {
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId1);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + anotherTeacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/chapter/" + createdChapterId1 + "/learning-outcomes/add")
                .then()
                .log().all()
                .statusCode(anyOf(is(403), is(400), is(500)));
    }

    @Test
    @Order(42)
    @DisplayName("Should fail to add learning outcomes that don't belong to the course")
    void testAddLearningOutcomesToChapter_LONotInCourse() {
        UUID nonExistentLoId = UUID.randomUUID();

        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(nonExistentLoId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/chapter/" + createdChapterId1 + "/learning-outcomes/add")
                .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @Order(43)
    @DisplayName("Should successfully add learning outcomes to a second chapter")
    void testAddLearningOutcomesToChapter2_Success() {
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId1);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/chapter/" + createdChapterId2 + "/learning-outcomes/add")
                .then()
                .log().all()
                .statusCode(200)
                .body("totalAdded", equalTo(1))
                .body("addedLearningOutcomeIds", hasSize(1))
                .body("addedLearningOutcomeIds", hasItem(createdLoId1.toString()));
    }

    // ==================== CHAPTER-LEVEL: REMOVE LEARNING OUTCOMES FROM CHAPTER ====================

    @Test
    @Order(44)
    @DisplayName("Should successfully remove a learning outcome from a chapter")
    void testRemoveLearningOutcomesFromChapter_SingleLO() {
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId3);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/chapter/" + createdChapterId1 + "/learning-outcomes/remove")
                .then()
                .log().all()
                .statusCode(200)
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("chapterId", equalTo(createdChapterId1.toString()))
                .body("totalRemoved", equalTo(1))
                .body("removedLearningOutcomeIds", hasSize(1))
                .body("removedLearningOutcomeIds", hasItem(createdLoId3.toString()))
                .body("message", equalTo("Learning outcomes removed from chapter successfully"));
    }

    @Test
    @Order(45)
    @DisplayName("Should verify chapter has correct LOs after removal")
    void testGetChapter_AfterRemoval() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/chapter/" + createdChapterId1)
                .then()
                .log().all()
                .statusCode(200)
                .body("learningOutcomeIds", hasSize(2))
                .body("learningOutcomeIds", hasItem(createdLoId1.toString()))
                .body("learningOutcomeIds", hasItem(createdLoId2.toString()));
    }

    @Test
    @Order(46)
    @DisplayName("Should successfully remove multiple learning outcomes from a chapter")
    void testRemoveLearningOutcomesFromChapter_MultipleLOs() {
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s", "%s"]
                }
                """.formatted(createdLoId1, createdLoId2);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/chapter/" + createdChapterId1 + "/learning-outcomes/remove")
                .then()
                .log().all()
                .statusCode(200)
                .body("totalRemoved", equalTo(2))
                .body("removedLearningOutcomeIds", hasSize(2));
    }

    @Test
    @Order(47)
    @DisplayName("Should verify chapter has no LOs after removing all")
    void testGetChapter_AfterRemovingAll() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/chapter/" + createdChapterId1)
                .then()
                .log().all()
                .statusCode(200)
                .body("learningOutcomeIds", hasSize(0));
    }

    @Test
    @Order(48)
    @DisplayName("Should fail to remove learning outcomes from chapter without authentication")
    void testRemoveLearningOutcomesFromChapter_WithoutAuth() {
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId1);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/chapter/" + createdChapterId1 + "/learning-outcomes/remove")
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(49)
    @DisplayName("Should fail to remove learning outcomes from chapter with empty list")
    void testRemoveLearningOutcomesFromChapter_EmptyList() {
        String requestBody = """
                {
                    "learningOutcomeIds": []
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/chapter/" + createdChapterId1 + "/learning-outcomes/remove")
                .then()
                .log().all()
                .statusCode(400);
    }

    @Test
    @Order(50)
    @DisplayName("Should fail to remove learning outcomes from chapter of non-existent course")
    void testRemoveLearningOutcomesFromChapter_CourseNotFound() {
        UUID nonExistentCourseId = UUID.randomUUID();

        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId1);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + nonExistentCourseId + "/chapter/" + createdChapterId1 + "/learning-outcomes/remove")
                .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @Order(51)
    @DisplayName("Should fail to remove learning outcomes from non-existent chapter")
    void testRemoveLearningOutcomesFromChapter_ChapterNotFound() {
        UUID nonExistentChapterId = UUID.randomUUID();

        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId1);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/chapter/" + nonExistentChapterId + "/learning-outcomes/remove")
                .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @Order(52)
    @DisplayName("Should fail to remove learning outcomes from chapter by unauthorized teacher")
    void testRemoveLearningOutcomesFromChapter_UnauthorizedTeacher() {
        // First re-add an LO so there's something to attempt to remove
        String addBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId1);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(addBody)
                .when()
                .put("/api/" + createdCourseId + "/chapter/" + createdChapterId1 + "/learning-outcomes/add")
                .then()
                .statusCode(200);

        // Try to remove as unauthorized teacher
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId1);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + anotherTeacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/chapter/" + createdChapterId1 + "/learning-outcomes/remove")
                .then()
                .log().all()
                .statusCode(anyOf(is(403), is(400), is(500)));
    }

    @Test
    @Order(53)
    @DisplayName("Should fail to remove learning outcomes not associated with the chapter")
    void testRemoveLearningOutcomesFromChapter_LONotAssociated() {
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId2);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/chapter/" + createdChapterId1 + "/learning-outcomes/remove")
                .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @Order(54)
    @DisplayName("Should verify getAllChapters returns learning outcome IDs")
    void testGetAllChapters_ReturnsLearningOutcomeIds() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/chapter")
                .then()
                .log().all()
                .statusCode(200)
                .body("$.size()", equalTo(2))
                .body("[0].learningOutcomeIds", notNullValue())
                .body("[1].learningOutcomeIds", notNullValue());
    }

    @Test
    @Order(55)
    @DisplayName("Should get a single learning outcome by id")
    void testGetLearningOutcomeById_ShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/learning-outcome/" + createdLoId1)
                .then()
                .log().all()
                .statusCode(200)
                .body("id", equalTo(createdLoId1.toString()))
                .body("prefixNumber", notNullValue())
                .body("description", notNullValue());
    }

    @Test
    @Order(56)
    @DisplayName("Should get learning outcomes by ids")
    void testGetLearningOutcomesByIds_ShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .queryParam("ids", createdLoId1, createdLoId2)
                .when()
                .get("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .log().all()
                .statusCode(200)
                .body("$", hasSize(2));
    }

    @Test
    @Order(57)
    @DisplayName("Should get grouped learning outcomes with children")
    void testGetGroupedLearningOutcomes_ShouldSucceed() {
        String createLoBody = """
                {
                    "learningOutcomes": [
                        {"prefixNumber": 1, "description": "Group GET child 1"},
                        {"prefixNumber": 2, "description": "Group GET child 2"}
                    ]
                }
                """;

        Response createLoResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(createLoBody)
                .when()
                .post("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .statusCode(201)
                .extract().response();

        UUID groupLo1 = UUID.fromString(createLoResponse.jsonPath().getString("learningOutcomes[0].id"));
        UUID groupLo2 = UUID.fromString(createLoResponse.jsonPath().getString("learningOutcomes[1].id"));

        String createGroupBody = """
                {
                    "code": "Grouping for GET",
                    "description": "Group for read endpoint",
                    "learningOutcomeIds": ["%s", "%s"]
                }
                """.formatted(groupLo1, groupLo2);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(createGroupBody)
                .when()
                .post("/api/" + createdCourseId + "/group-learning-outcome")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/learning-outcome/grouped")
                .then()
                .log().all()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].learningOutcomes", notNullValue());
    }

    @Test
    @Order(58)
    @DisplayName("Should return grouped and ungrouped learning outcomes in chapter get")
    void testGetChapter_WithGroupedAndUngroupedLearningOutcomes_ShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/chapter/" + createdChapterId1)
                .then()
                .log().all()
                .statusCode(200)
                .body("groupedLearningOutcomes", notNullValue())
                .body("ungroupedLearningOutcomes", notNullValue());
    }
}
