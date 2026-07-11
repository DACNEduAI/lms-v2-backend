package edu.ai.lms.core.course.adapter.in;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
@DisplayName("Chapter Controller Integration Tests")
class ChapterControllerIT extends IntegrationTestConfiguration {
    private static String teacherToken;
    private static UUID createdCourseId;
    private static UUID createdChapterId;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ==================== SETUP: Register teacher and create a course ====================

    @Test
    @Order(1)
    @DisplayName("Should register a teacher for chapter tests")
    void testRegisterTeacher() {
        String requestBody = """
                {
                    "firstName": "Chapter",
                    "lastName": "Teacher",
                    "email": "chapter.teacher@example.com",
                    "password": "TeacherPass123!",
                    "employeeId": "EMP-CHAP001",
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
                .body("email", equalTo("chapter.teacher@example.com"))
                .extract().response();

        teacherToken = response.jsonPath().getString("token");
    }

    @Test
    @Order(2)
    @DisplayName("Should create a course for chapter tests")
    void testCreateCourseForChapters() {
        String startDate = LocalDate.now().plusDays(1).format(DATE_FORMATTER);
        String endDate = LocalDate.now().plusMonths(3).format(DATE_FORMATTER);

        String requestBody = """
                {
                    "courseName": "Java Programming for Chapters",
                    "description": "Course used for chapter integration tests",
                    "courseCode": "CHAP101",
                    "active": true,
                    "coverImage": "https://example.com/java-chapters.jpg",
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": [
                        {
                            "prefixNumber": 1,
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

    // ==================== CREATE CHAPTER TESTS ====================

    @Test
    @Order(3)
    @DisplayName("Should successfully create a chapter")
    void testCreateChapter_WithoutMaterials() {
        String requestBody = """
                {
                    "title": "Chapter 1: Introduction",
                    "description": "Introduction to Java programming"
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/chapter")
                .then()
                .log().all()
                .statusCode(201)
                .body("chapterId", notNullValue())
                .body("title", equalTo("Chapter 1: Introduction"))
                .body("description", equalTo("Introduction to Java programming"))
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("createdAt", notNullValue())
                .extract().response();

        createdChapterId = UUID.fromString(response.jsonPath().getString("chapterId"));
    }

    // ==================== GET ALL CHAPTERS TESTS ====================

    @Test
    @Order(4)
    @DisplayName("Should return all chapters for a course")
    void testGetAllChapters_WithValidCourseId() {
        // At this point we have 1 chapter created in Order 3
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/chapter")
                .then()
                .log().all()
                .statusCode(200)
                .body("$.size()", greaterThanOrEqualTo(1))
                .body("[0].chapterId", notNullValue())
                .body("[0].title", notNullValue())
                .body("[0].courseId", equalTo(createdCourseId.toString()))
                .body("[0].createdAt", notNullValue());
    }

    // ==================== CREATE CHAPTER VALIDATION TESTS ====================

    @Test
    @Order(5)
    @DisplayName("Should fail to create chapter without authentication")
    void testCreateChapter_WithoutAuth() {
        String requestBody = """
                {
                    "title": "Unauthorized Chapter",
                    "description": "This should fail"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/chapter")
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(6)
    @DisplayName("Should fail to create chapter with missing title")
    void testCreateChapter_WithMissingTitle() {
        String requestBody = """
                {
                    "description": "Missing title chapter"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/chapter")
                .then()
                .log().all()
                .statusCode(400);
    }

    @Test
    @Order(7)
    @DisplayName("Should fail to create chapter for non-existent course")
    void testCreateChapter_WithNonExistentCourse() {
        UUID nonExistentCourseId = UUID.randomUUID();

        String requestBody = """
                {
                    "title": "Orphan Chapter",
                    "description": "This should fail - course does not exist"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + nonExistentCourseId + "/chapter")
                .then()
                .log().all()
                .statusCode(anyOf(is(404), is(400), is(500)));
    }

    @Test
    @Order(8)
    @DisplayName("Should fail to create chapter by unauthorized teacher")
    void testCreateChapter_ByUnauthorizedTeacher() {
        // Register another teacher
        String registerBody = """
                {
                    "firstName": "Another",
                    "lastName": "ChapterTeacher",
                    "email": "another.chapter.teacher@example.com",
                    "password": "AnotherPass123!",
                    "employeeId": "EMP-CHAP002",
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

        String anotherTeacherToken = response.jsonPath().getString("token");

        // Try to create a chapter in a course owned by the first teacher
        String requestBody = """
                {
                    "title": "Hijacked Chapter",
                    "description": "This should fail - not the course owner"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + anotherTeacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/chapter")
                .then()
                .log().all()
                .statusCode(anyOf(is(403), is(400), is(500)));
    }

    // ==================== GET CHAPTER TESTS ====================

    @Test
    @Order(9)
    @DisplayName("Should successfully get a chapter by composite key")
    void testGetChapter_WithValidKey() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/chapter/" + createdChapterId)
                .then()
                .log().all()
                .statusCode(200)
                .body("chapterId", equalTo(createdChapterId.toString()))
                .body("title", equalTo("Chapter 1: Introduction"))
                .body("description", equalTo("Introduction to Java programming"))
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue());
    }

    @Test
    @Order(10)
    @DisplayName("Should fail to get chapter without authentication")
    void testGetChapter_WithoutAuth() {
        given()
                .when()
                .get("/api/" + createdCourseId + "/chapter/" + createdChapterId)
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(11)
    @DisplayName("Should fail to get non-existent chapter")
    void testGetChapter_WithNonExistentKey() {
        UUID nonExistentChapterId = UUID.randomUUID();

        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/chapter/" + nonExistentChapterId)
                .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @Order(12)
    @DisplayName("Should allow another teacher to get a chapter (no ownership check on get)")
    void testGetChapter_ByAnotherTeacher() {
        // Register another teacher
        String registerBody = """
                {
                    "firstName": "Viewer",
                    "lastName": "Teacher",
                    "email": "viewer.chapter.teacher@example.com",
                    "password": "ViewerPass123!",
                    "employeeId": "EMP-CHAP003",
                    "department": "Physics",
                    "specialization": "Optics"
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

        String anotherTeacherToken = response.jsonPath().getString("token");

        // Another teacher should be able to get the chapter (no ownership check)
        given()
                .header("Authorization", "Bearer " + anotherTeacherToken)
                .when()
                .get("/api/" + createdCourseId + "/chapter/" + createdChapterId)
                .then()
                .log().all()
                .statusCode(200)
                .body("chapterId", equalTo(createdChapterId.toString()))
                .body("title", equalTo("Chapter 1: Introduction"));
    }

    @Test
    @Order(13)
    @DisplayName("Should successfully create a chapter with only a title")
    void testCreateChapter_WithMinimalFields() {
        String requestBody = """
                {
                    "title": "Minimal Chapter"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/chapter")
                .then()
                .log().all()
                .statusCode(201)
                .body("chapterId", notNullValue())
                .body("title", equalTo("Minimal Chapter"))
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("createdAt", notNullValue());
    }

    @Test
    @Order(14)
    @DisplayName("Should successfully create multiple chapters in the same course")
    void testCreateChapter_MultipleChaptersInSameCourse() {
        String requestBody = """
                {
                    "title": "Chapter 4: Loops and Iteration",
                    "description": "For loops, while loops, do-while"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/chapter")
                .then()
                .log().all()
                .statusCode(201)
                .body("chapterId", notNullValue())
                .body("title", equalTo("Chapter 4: Loops and Iteration"))
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("createdAt", notNullValue());
    }

    // ==================== GET ALL CHAPTERS - EXTENDED TESTS ====================

    @Test
    @Order(15)
    @DisplayName("Should return multiple chapters after creating several")
    void testGetAllChapters_ReturnsMultipleChapters() {
        // At this point, Orders 3, 13, 14 created chapters (Order 13 = Minimal Chapter, Order 14 = Chapter 4: Loops)
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/chapter")
                .then()
                .log().all()
                .statusCode(200)
                .body("$.size()", greaterThanOrEqualTo(3));
    }

    @Test
    @Order(16)
    @DisplayName("Should return empty list for a course with no chapters")
    void testGetAllChapters_EmptyCourse() {
        // Create a fresh course with no chapters
        String startDate = LocalDate.now().plusDays(1).format(DATE_FORMATTER);
        String endDate = LocalDate.now().plusMonths(3).format(DATE_FORMATTER);

        String courseBody = """
                {
                    "courseName": "Empty Chapters Course",
                    "description": "Course with no chapters",
                    "courseCode": "CHAP-EMPTY-001",
                    "active": true,
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": []
                }
                """.formatted(startDate, endDate);

        UUID emptyCourseId = UUID.fromString(
                given()
                        .contentType(ContentType.JSON)
                        .header("Authorization", "Bearer " + teacherToken)
                        .body(courseBody)
                        .when()
                        .post("/api/course")
                        .then()
                        .statusCode(201)
                        .extract().jsonPath().getString("courseId"));

        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + emptyCourseId + "/chapter")
                .then()
                .log().all()
                .statusCode(200)
                .body("$.size()", equalTo(0));
    }

    @Test
    @Order(17)
    @DisplayName("Should fail to get all chapters without authentication")
    void testGetAllChapters_WithoutAuth() {
        given()
                .when()
                .get("/api/" + createdCourseId + "/chapter")
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    // ==================== MODIFY CHAPTER TESTS ====================

    @Test
    @Order(18)
    @DisplayName("Should successfully modify a chapter")
    void testModifyChapter_WithValidDetails() {
        String requestBody = """
                {
                    "title": "Chapter 1: Updated Introduction",
                    "description": "Updated introduction to Java programming"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/chapter/" + createdChapterId)
                .then()
                .log().all()
                .statusCode(200)
                .body("chapterId", equalTo(createdChapterId.toString()))
                .body("title", equalTo("Chapter 1: Updated Introduction"))
                .body("description", equalTo("Updated introduction to Java programming"))
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("updatedAt", notNullValue());
    }

    @Test
    @Order(20)
    @DisplayName("Should fail to modify chapter without authentication")
    void testModifyChapter_WithoutAuth() {
        String requestBody = """
                {
                    "title": "Unauthorized Update",
                    "description": "This should fail"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/chapter/" + createdChapterId)
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(21)
    @DisplayName("Should fail to modify chapter with missing title")
    void testModifyChapter_WithMissingTitle() {
        String requestBody = """
                {
                    "description": "Missing title - should fail"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/chapter/" + createdChapterId)
                .then()
                .log().all()
                .statusCode(400);
    }

    @Test
    @Order(22)
    @DisplayName("Should fail to modify non-existent chapter")
    void testModifyChapter_WithNonExistentChapter() {
        UUID nonExistentChapterId = UUID.randomUUID();

        String requestBody = """
                {
                    "title": "Update Non-Existent",
                    "description": "This should fail"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/chapter/" + nonExistentChapterId)
                .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @Order(23)
    @DisplayName("Should fail to modify chapter by unauthorized teacher")
    void testModifyChapter_ByUnauthorizedTeacher() {
        // Register another teacher
        String registerBody = """
                {
                    "firstName": "Modify",
                    "lastName": "Unauthorized",
                    "email": "modify.unauthorized@example.com",
                    "password": "UnauthorizedPass123!",
                    "employeeId": "EMP-CHAP004",
                    "department": "Chemistry",
                    "specialization": "Organic Chemistry"
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

        String unauthorizedToken = response.jsonPath().getString("token");

        // Try to modify a chapter in a course owned by the first teacher
        String requestBody = """
                {
                    "title": "Hijacked Update",
                    "description": "This should fail - not the course owner"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + unauthorizedToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/chapter/" + createdChapterId)
                .then()
                .log().all()
                .statusCode(anyOf(is(403), is(400), is(500)));
    }

    // ==================== DELETE CHAPTER TESTS ====================

    @Test
    @Order(24)
    @DisplayName("Should fail to delete chapter without authentication")
    void testDeleteChapter_WithoutAuth() {
        given()
                .when()
                .delete("/api/" + createdCourseId + "/chapter/" + createdChapterId)
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(25)
    @DisplayName("Should fail to delete non-existent chapter")
    void testDeleteChapter_WithNonExistentChapter() {
        UUID nonExistentChapterId = UUID.randomUUID();

        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .delete("/api/" + createdCourseId + "/chapter/" + nonExistentChapterId)
                .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @Order(26)
    @DisplayName("Should fail to delete chapter by unauthorized teacher")
    void testDeleteChapter_ByUnauthorizedTeacher() {
        // Register another teacher
        String registerBody = """
                {
                    "firstName": "Delete",
                    "lastName": "Unauthorized",
                    "email": "delete.unauthorized@example.com",
                    "password": "DeleteUnauthorizedPass123!",
                    "employeeId": "EMP-CHAP005",
                    "department": "Biology",
                    "specialization": "Genetics"
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

        String unauthorizedToken = response.jsonPath().getString("token");

        // Try to delete a chapter in a course owned by the first teacher
        given()
                .header("Authorization", "Bearer " + unauthorizedToken)
                .when()
                .delete("/api/" + createdCourseId + "/chapter/" + createdChapterId)
                .then()
                .log().all()
                .statusCode(anyOf(is(403), is(400), is(500)));
    }

    @Test
    @Order(27)
    @DisplayName("Should successfully delete a chapter")
    void testDeleteChapter_WithValidDetails() {
        // First create a chapter to delete
        String createRequestBody = """
                {
                    "title": "Chapter To Delete",
                    "description": "This chapter will be deleted"
                }
                """;

        Response createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(createRequestBody)
                .when()
                .post("/api/" + createdCourseId + "/chapter")
                .then()
                .log().all()
                .statusCode(201)
                .extract().response();

        UUID chapterToDelete = UUID.fromString(createResponse.jsonPath().getString("chapterId"));

        // Now delete the chapter
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .delete("/api/" + createdCourseId + "/chapter/" + chapterToDelete)
                .then()
                .log().all()
                .statusCode(200)
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("chapterId", equalTo(chapterToDelete.toString()))
                .body("message", equalTo("Chapter deleted successfully"))
                .body("success", equalTo(true));

        // Verify the chapter is actually deleted by trying to get it
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/chapter/" + chapterToDelete)
                .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @Order(28)
    @DisplayName("Should fail to delete chapter from non-existent course")
    void testDeleteChapter_WithNonExistentCourse() {
        UUID nonExistentCourseId = UUID.randomUUID();

        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .delete("/api/" + nonExistentCourseId + "/chapter/" + createdChapterId)
                .then()
                .log().all()
                .statusCode(anyOf(is(404), is(400), is(500)));
    }
}
