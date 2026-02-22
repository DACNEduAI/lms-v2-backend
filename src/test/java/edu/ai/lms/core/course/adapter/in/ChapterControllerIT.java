package edu.ai.lms.core.course.adapter.in;

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

    // ==================== CREATE CHAPTER TESTS ====================

    @Test
    @Order(3)
    @DisplayName("Should successfully create a chapter without materials")
    void testCreateChapter_WithoutMaterials() {
        String requestBody = """
                {
                    "title": "Chapter 1: Introduction",
                    "description": "Introduction to Java programming",
                    "orderIndex": 1,
                    "courseId": "%s",
                    "materials": []
                }
                """.formatted(createdCourseId);

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/chapter")
                .then()
                .log().all()
                .statusCode(201)
                .body("chapterId", notNullValue())
                .body("title", equalTo("Chapter 1: Introduction"))
                .body("description", equalTo("Introduction to Java programming"))
                .body("orderIndex", equalTo(1))
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("createdAt", notNullValue())
                .extract().response();

        createdChapterId = UUID.fromString(response.jsonPath().getString("chapterId"));
    }

    @Test
    @Order(4)
    @DisplayName("Should successfully create a chapter with materials")
    void testCreateChapter_WithMaterials() {
        String requestBody = """
                {
                    "title": "Chapter 2: Variables and Data Types",
                    "description": "Learn about Java variables and data types",
                    "orderIndex": 2,
                    "courseId": "%s",
                    "materials": [
                        {
                            "title": "Variables Tutorial PDF",
                            "type": "PDF",
                            "sourceUrl": "https://example.com/variables.pdf",
                            "downloadable": true
                        },
                        {
                            "title": "Variables Video Lecture",
                            "type": "VIDEO",
                            "sourceUrl": "https://example.com/variables-video.mp4",
                            "downloadable": false
                        }
                    ]
                }
                """.formatted(createdCourseId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/chapter")
                .then()
                .log().all()
                .statusCode(201)
                .body("chapterId", notNullValue())
                .body("title", equalTo("Chapter 2: Variables and Data Types"))
                .body("description", equalTo("Learn about Java variables and data types"))
                .body("orderIndex", equalTo(2))
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("materials", hasSize(2))
                .body("createdAt", notNullValue());
    }

    @Test
    @Order(5)
    @DisplayName("Should fail to create chapter without authentication")
    void testCreateChapter_WithoutAuth() {
        String requestBody = """
                {
                    "title": "Unauthorized Chapter",
                    "description": "This should fail",
                    "orderIndex": 1,
                    "courseId": "%s",
                    "materials": []
                }
                """.formatted(createdCourseId);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/chapter")
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
                    "description": "Missing title chapter",
                    "orderIndex": 1,
                    "courseId": "%s",
                    "materials": []
                }
                """.formatted(createdCourseId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/chapter")
                .then()
                .log().all()
                .statusCode(400);
    }

    @Test
    @Order(7)
    @DisplayName("Should fail to create chapter with missing courseId")
    void testCreateChapter_WithMissingCourseId() {
        String requestBody = """
                {
                    "title": "No Course Id Chapter",
                    "description": "This should fail",
                    "orderIndex": 1,
                    "materials": []
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/chapter")
                .then()
                .log().all()
                .statusCode(400);
    }

    @Test
    @Order(8)
    @DisplayName("Should fail to create chapter for non-existent course")
    void testCreateChapter_WithNonExistentCourse() {
        UUID nonExistentCourseId = UUID.randomUUID();

        String requestBody = """
                {
                    "title": "Orphan Chapter",
                    "description": "This should fail - course does not exist",
                    "orderIndex": 1,
                    "courseId": "%s",
                    "materials": []
                }
                """.formatted(nonExistentCourseId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/chapter")
                .then()
                .log().all()
                .statusCode(anyOf(is(404), is(400), is(500)));
    }

    @Test
    @Order(9)
    @DisplayName("Should fail to create chapter by unauthorized teacher")
    void testCreateChapter_ByUnauthorizedTeacher() {
        // Register another teacher
        String registerBody = """
                {
                    "firstName": "Another",
                    "lastName": "ChapterTeacher",
                    "email": "another.chapter.teacher@example.com",
                    "password": "AnotherPass123!",
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
                    "description": "This should fail - not the course owner",
                    "orderIndex": 1,
                    "courseId": "%s",
                    "materials": []
                }
                """.formatted(createdCourseId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + anotherTeacherToken)
                .body(requestBody)
                .when()
                .post("/api/chapter")
                .then()
                .log().all()
                .statusCode(anyOf(is(403), is(400), is(500)));
    }

    // ==================== GET CHAPTER TESTS ====================

    @Test
    @Order(10)
    @DisplayName("Should successfully get a chapter by ID")
    void testGetChapter_WithValidId() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/chapter/" + createdChapterId)
                .then()
                .log().all()
                .statusCode(200)
                .body("chapterId", equalTo(createdChapterId.toString()))
                .body("title", equalTo("Chapter 1: Introduction"))
                .body("description", equalTo("Introduction to Java programming"))
                .body("orderIndex", equalTo(1))
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue());
    }

    @Test
    @Order(11)
    @DisplayName("Should fail to get chapter without authentication")
    void testGetChapter_WithoutAuth() {
        given()
                .when()
                .get("/api/chapter/" + createdChapterId)
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(12)
    @DisplayName("Should fail to get non-existent chapter")
    void testGetChapter_WithNonExistentId() {
        UUID nonExistentChapterId = UUID.randomUUID();

        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/chapter/" + nonExistentChapterId)
                .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @Order(13)
    @DisplayName("Should allow another teacher to get a chapter (no ownership check on get)")
    void testGetChapter_ByAnotherTeacher() {
        // Register another teacher
        String registerBody = """
                {
                    "firstName": "Viewer",
                    "lastName": "Teacher",
                    "email": "viewer.chapter.teacher@example.com",
                    "password": "ViewerPass123!",
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
                .get("/api/chapter/" + createdChapterId)
                .then()
                .log().all()
                .statusCode(200)
                .body("chapterId", equalTo(createdChapterId.toString()))
                .body("title", equalTo("Chapter 1: Introduction"));
    }

    @Test
    @Order(14)
    @DisplayName("Should successfully create a chapter with only a title and courseId")
    void testCreateChapter_WithMinimalFields() {
        String requestBody = """
                {
                    "title": "Minimal Chapter",
                    "courseId": "%s"
                }
                """.formatted(createdCourseId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/chapter")
                .then()
                .log().all()
                .statusCode(201)
                .body("chapterId", notNullValue())
                .body("title", equalTo("Minimal Chapter"))
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("createdAt", notNullValue());
    }

    @Test
    @Order(15)
    @DisplayName("Should successfully create multiple chapters in the same course")
    void testCreateChapter_MultipleChaptersInSameCourse() {
        String requestBody = """
                {
                    "title": "Chapter 4: Loops and Iteration",
                    "description": "For loops, while loops, do-while",
                    "orderIndex": 4,
                    "courseId": "%s",
                    "materials": [
                        {
                            "title": "Loops Reference",
                            "type": "LINK",
                            "sourceUrl": "https://example.com/loops-ref",
                            "downloadable": false
                        }
                    ]
                }
                """.formatted(createdCourseId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/chapter")
                .then()
                .log().all()
                .statusCode(201)
                .body("chapterId", notNullValue())
                .body("title", equalTo("Chapter 4: Loops and Iteration"))
                .body("orderIndex", equalTo(4))
                .body("materials", hasSize(1));
    }
}
