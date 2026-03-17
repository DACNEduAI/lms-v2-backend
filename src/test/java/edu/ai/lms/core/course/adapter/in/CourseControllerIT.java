package edu.ai.lms.core.course.adapter.in;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.BeforeAll;
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
@DisplayName("Course Controller Integration Tests")
class CourseControllerIT extends IntegrationTestConfiguration {
    private static String teacherToken;
    private static UUID createdCourseId;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Test
    @Order(1)
    @DisplayName("Should register a teacher for course tests")
    void testRegisterTeacher() {
        String requestBody = """
                {
                    "firstName": "Course",
                    "lastName": "Teacher",
                    "email": "course.teacher@example.com",
                    "password": "TeacherPass123!",
                    "employeeId": "EMP-CRS001",
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
                .body("email", equalTo("course.teacher@example.com"))
                .extract().response();

        teacherToken = response.jsonPath().getString("token");
    }

    @Test
    @Order(2)
    @DisplayName("Should successfully create a new course")
    void testCreateCourse() {
        String startDate = LocalDate.now().plusDays(1).format(DATE_FORMATTER);
        String endDate = LocalDate.now().plusMonths(3).format(DATE_FORMATTER);

        String requestBody = """
                {
                    "courseName": "Introduction to Java Programming",
                    "description": "Learn Java programming from scratch",
                    "courseCode": "CS101",
                    "active": true,
                    "coverImage": "https://example.com/java-course.jpg",
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": [
                        {
                            "code": "CLO-1",
                            "description": "Understand basic Java syntax"
                        },
                        {
                            "code": "CLO-2",
                            "description": "Write simple Java programs"
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
                .body("courseName", equalTo("Introduction to Java Programming"))
                .body("courseCode", equalTo("CS101"))
                .body("description", equalTo("Learn Java programming from scratch"))
                .body("active", equalTo(true))
                .extract().response();

        createdCourseId = UUID.fromString(response.jsonPath().getString("courseId"));
    }

    @Test
    @Order(3)
    @DisplayName("Should fail to create course without authentication")
    void testCreateCourseWithoutAuth() {
        String startDate = LocalDate.now().plusDays(1).format(DATE_FORMATTER);
        String endDate = LocalDate.now().plusMonths(3).format(DATE_FORMATTER);

        String requestBody = """
                {
                    "courseName": "Unauthorized Course",
                    "description": "This should fail",
                    "courseCode": "CS102",
                    "active": true,
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": []
                }
                """.formatted(startDate, endDate);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/course")
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(4)
    @DisplayName("Should fail to create course with duplicate course code")
    void testCreateCourseWithDuplicateCode() {
        String startDate = LocalDate.now().plusDays(1).format(DATE_FORMATTER);
        String endDate = LocalDate.now().plusMonths(3).format(DATE_FORMATTER);

        String requestBody = """
                {
                    "courseName": "Another Java Course",
                    "description": "This should fail due to duplicate code",
                    "courseCode": "CS101",
                    "active": true,
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": []
                }
                """.formatted(startDate, endDate);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/course")
                .then()
                .log().all()
                .statusCode(anyOf(is(400), is(409), is(500)));
    }

    @Test
    @Order(5)
    @DisplayName("Should fail to create course with missing required fields")
    void testCreateCourseWithMissingFields() {
        String requestBody = """
                {
                    "courseName": "Incomplete Course"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/course")
                .then()
                .log().all()
                .statusCode(400);
    }

    @Test
    @Order(6)
    @DisplayName("Should fail to create course with invalid date format")
    void testCreateCourseWithInvalidDateFormat() {
        String requestBody = """
                {
                    "courseName": "Bad Date Course",
                    "description": "This should fail due to invalid date format",
                    "courseCode": "BADDATE",
                    "active": true,
                    "startDate": "2026-02-12",
                    "endDate": "2026-05-12",
                    "learningOutcomes": []
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/course")
                .then()
                .log().all()
                .statusCode(400);
    }

    @Test
    @Order(7)
    @DisplayName("Should successfully modify an existing course")
    void testModifyCourse() {
        LocalDate newStartDate = LocalDate.now().plusDays(7);
        LocalDate newEndDate = LocalDate.now().plusMonths(6);

        String requestBody = """
                {
                    "courseId": "%s",
                    "courseName": "Advanced Java Programming",
                    "description": "Updated course description with advanced topics",
                    "courseCode": "CS101",
                    "active": true,
                    "coverImage": "https://example.com/advanced-java.jpg",
                    "startDate": [%d, %d, %d],
                    "endDate": [%d, %d, %d],
                    "learningOutcomes": [
                        {
                            "code": "CLO-1",
                            "description": "Master advanced Java concepts"
                        }
                    ]
                }
                """.formatted(createdCourseId,
                newStartDate.getYear(), newStartDate.getMonthValue(), newStartDate.getDayOfMonth(),
                newEndDate.getYear(), newEndDate.getMonthValue(), newEndDate.getDayOfMonth());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/course")
                .then()
                .log().all()
                .statusCode(200)
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("courseName", equalTo("Advanced Java Programming"))
                .body("description", equalTo("Updated course description with advanced topics"))
                .body("courseCode", equalTo("CS101"))
                .body("active", equalTo(true))
                .body("updatedAt", notNullValue());
    }

    @Test
    @Order(8)
    @DisplayName("Should fail to modify course without authentication")
    void testModifyCourseWithoutAuth() {
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusMonths(3);

        String requestBody = """
                {
                    "courseId": "%s",
                    "courseName": "Unauthorized Modification",
                    "description": "This should fail",
                    "courseCode": "CS101",
                    "active": false,
                    "startDate": [%d, %d, %d],
                    "endDate": [%d, %d, %d],
                    "learningOutcomes": []
                }
                """.formatted(createdCourseId,
                startDate.getYear(), startDate.getMonthValue(), startDate.getDayOfMonth(),
                endDate.getYear(), endDate.getMonthValue(), endDate.getDayOfMonth());

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/course")
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(9)
    @DisplayName("Should fail to modify non-existent course")
    void testModifyNonExistentCourse() {
        UUID nonExistentCourseId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusMonths(3);

        String requestBody = """
                {
                    "courseId": "%s",
                    "courseName": "Non-existent Course",
                    "description": "This should fail",
                    "courseCode": "CS999",
                    "active": true,
                    "startDate": [%d, %d, %d],
                    "endDate": [%d, %d, %d],
                    "learningOutcomes": []
                }
                """.formatted(nonExistentCourseId,
                startDate.getYear(), startDate.getMonthValue(), startDate.getDayOfMonth(),
                endDate.getYear(), endDate.getMonthValue(), endDate.getDayOfMonth());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/course")
                .then()
                .log().all()
                .statusCode(anyOf(is(404)));
    }

    @Test
    @Order(10)
    @DisplayName("Should fail to modify course by unauthorized teacher")
    void testModifyCourseByUnauthorizedTeacher() {
        // First, register another teacher
        String registerBody = """
                {
                    "firstName": "Another",
                    "lastName": "Teacher",
                    "email": "another.teacher@example.com",
                    "password": "AnotherPass123!",
                    "employeeId": "EMP-CRS002",
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

        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusMonths(3);

        // Try to modify the course created by the first teacher
        String modifyBody = """
                {
                    "courseId": "%s",
                    "courseName": "Hijacked Course",
                    "description": "This should fail",
                    "courseCode": "CS101",
                    "active": false,
                    "startDate": [%d, %d, %d],
                    "endDate": [%d, %d, %d],
                    "learningOutcomes": []
                }
                """.formatted(createdCourseId,
                startDate.getYear(), startDate.getMonthValue(), startDate.getDayOfMonth(),
                endDate.getYear(), endDate.getMonthValue(), endDate.getDayOfMonth());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + anotherTeacherToken)
                .body(modifyBody)
                .when()
                .put("/api/course")
                .then()
                .log().all()
                .statusCode(anyOf(is(403), is(400), is(500)));
    }

    @Test
    @Order(11)
    @DisplayName("Should fail to delete course without authentication")
    void testDeleteCourseWithoutAuth() {
        given()
                .when()
                .delete("/api/course/" + createdCourseId)
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(12)
    @DisplayName("Should fail to delete non-existent course")
    void testDeleteNonExistentCourse() {
        UUID nonExistentCourseId = UUID.randomUUID();

        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .delete("/api/course/" + nonExistentCourseId)
                .then()
                .log().all()
                .statusCode(anyOf(is(404), is(400), is(500)));
    }

    @Test
    @Order(13)
    @DisplayName("Should successfully delete an existing course")
    void testDeleteCourse() {
        // First create a new course to delete
        String startDate = LocalDate.now().plusDays(1).format(DATE_FORMATTER);
        String endDate = LocalDate.now().plusMonths(3).format(DATE_FORMATTER);

        String createBody = """
                {
                    "courseName": "Course To Delete",
                    "description": "This course will be deleted",
                    "courseCode": "DEL001",
                    "active": true,
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": []
                }
                """.formatted(startDate, endDate);

        Response createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(createBody)
                .when()
                .post("/api/course")
                .then()
                .log().all()
                .statusCode(201)
                .extract().response();

        UUID courseToDeleteId = UUID.fromString(createResponse.jsonPath().getString("courseId"));

        // Now delete the course
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .delete("/api/course/" + courseToDeleteId)
                .then()
                .log().all()
                .statusCode(200)
                .body("courseId", equalTo(courseToDeleteId.toString()))
                .body("message", equalTo("Course deleted successfully"))
                .body("success", equalTo(true));
    }

    @Test
    @Order(14)
    @DisplayName("Should fail to delete course by unauthorized teacher")
    void testDeleteCourseByUnauthorizedTeacher() {
        // Register a new teacher
        String registerBody = """
                {
                    "firstName": "Unauthorized",
                    "lastName": "Deleter",
                    "email": "unauthorized.deleter@example.com",
                    "password": "DeleterPass123!",
                    "employeeId": "EMP-CRS003",
                    "department": "Physics",
                    "specialization": "Quantum Mechanics"
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

        // Try to delete the course created by the first teacher
        given()
                .header("Authorization", "Bearer " + unauthorizedToken)
                .when()
                .delete("/api/course/" + createdCourseId)
                .then()
                .log().all()
                .statusCode(anyOf(is(403), is(400), is(500)));
    }

    @Test
    @Order(15)
    @DisplayName("Should successfully create course with multiple learning outcomes")
    void testCreateCourseWithMultipleLearningOutcomes() {
        String startDate = LocalDate.now().plusDays(10).format(DATE_FORMATTER);
        String endDate = LocalDate.now().plusMonths(4).format(DATE_FORMATTER);

        String requestBody = """
                {
                    "courseName": "Data Structures and Algorithms",
                    "description": "Comprehensive course on DSA",
                    "courseCode": "CS201",
                    "active": true,
                    "coverImage": "https://example.com/dsa-course.jpg",
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": [
                        {
                            "code": "CLO-1",
                            "description": "Understand arrays and linked lists"
                        },
                        {
                            "code": "CLO-2",
                            "description": "Implement sorting algorithms"
                        },
                        {
                            "code": "CLO-3",
                            "description": "Apply graph algorithms"
                        },
                        {
                            "code": "CLO-4",
                            "description": "Analyze algorithm complexity"
                        }
                    ]
                }
                """.formatted(startDate, endDate);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/course")
                .then()
                .log().all()
                .statusCode(201)
                .body("courseId", notNullValue())
                .body("courseName", equalTo("Data Structures and Algorithms"))
                .body("courseCode", equalTo("CS201"));
    }

    @Test
    @Order(16)
    @DisplayName("Should successfully create course with empty learning outcomes")
    void testCreateCourseWithEmptyLearningOutcomes() {
        String startDate = LocalDate.now().plusDays(15).format(DATE_FORMATTER);
        String endDate = LocalDate.now().plusMonths(5).format(DATE_FORMATTER);

        String requestBody = """
                {
                    "courseName": "Quick Course",
                    "description": "A course with no learning outcomes defined yet",
                    "courseCode": "CS301",
                    "active": true,
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": []
                }
                """.formatted(startDate, endDate);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/course")
                .then()
                .log().all()
                .statusCode(201)
                .body("courseId", notNullValue())
                .body("courseName", equalTo("Quick Course"))
                .body("courseCode", equalTo("CS301"));
    }

    // ==================== GET COURSE TESTS ====================

    @Test
    @Order(17)
    @DisplayName("Should successfully get a specific course by ID")
    void testGetCourseById() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/course/" + createdCourseId)
                .then()
                .log().all()
                .statusCode(200)
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("courseName", notNullValue())
                .body("courseCode", equalTo("CS101"))
                .body("description", notNullValue())
                .body("teacherId", notNullValue())
                .body("active", notNullValue())
                .body("startDate", notNullValue())
                .body("endDate", notNullValue())
                .body("groupLearningOutcomes", notNullValue())
                .body("ungroupedLearningOutcomes", notNullValue());
    }

    @Test
    @Order(18)
    @DisplayName("Should fail to get course without authentication")
    void testGetCourseWithoutAuth() {
        given()
                .when()
                .get("/api/course/" + createdCourseId)
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(19)
    @DisplayName("Should fail to get non-existent course")
    void testGetNonExistentCourse() {
        UUID nonExistentCourseId = UUID.randomUUID();

        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/course/" + nonExistentCourseId)
                .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @Order(20)
    @DisplayName("Should fail to get course by unauthorized teacher")
    void testGetCourseByUnauthorizedTeacher() {
        // Register a new teacher
        String registerBody = """
                {
                    "firstName": "Unauthorized",
                    "lastName": "Getter",
                    "email": "unauthorized.getter@example.com",
                    "password": "GetterPass123!",
                    "employeeId": "EMP-CRS004",
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

        // Try to get the course created by the first teacher
        given()
                .header("Authorization", "Bearer " + unauthorizedToken)
                .when()
                .get("/api/course/" + createdCourseId)
                .then()
                .log().all()
                .statusCode(403);
    }

    @Test
    @Order(21)
    @DisplayName("Should get course with learning outcomes")
    void testGetCourseWithLearningOutcomes() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/course/" + createdCourseId)
                .then()
                .log().all()
                .statusCode(200)
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("groupLearningOutcomes", notNullValue())
                .body("ungroupedLearningOutcomes", notNullValue());
    }

    // ==================== GET ALL COURSES TESTS ====================

    @Test
    @Order(22)
    @DisplayName("Should successfully get all courses with default pagination")
    void testGetAllCoursesWithDefaultPagination() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/course")
                .then()
                .log().all()
                .statusCode(200)
                .body("courses", notNullValue())
                .body("totalCourses", greaterThan(0))
                .body("pageSize", equalTo(5))
                .body("sortDirection", equalTo("asc"));
    }

    @Test
    @Order(23)
    @DisplayName("Should get all courses with custom page size")
    void testGetAllCoursesWithCustomPageSize() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .queryParam("pageSize", 2)
                .when()
                .get("/api/course")
                .then()
                .log().all()
                .statusCode(200)
                .body("courses", notNullValue())
                .body("pageSize", equalTo(2))
                .body("courses", hasSize(2));
    }

    @Test
    @Order(24)
    @DisplayName("Should get all courses with descending sort order")
    void testGetAllCoursesWithDescendingSort() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .queryParam("sortDirection", "desc")
                .when()
                .get("/api/course")
                .then()
                .log().all()
                .statusCode(200)
                .body("courses", notNullValue())
                .body("sortDirection", equalTo("desc"));
    }

    @Test
    @Order(25)
    @DisplayName("Should get all courses with custom page size and sort direction")
    void testGetAllCoursesWithCustomPaginationAndSort() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .queryParam("pageSize", 3)
                .queryParam("sortDirection", "desc")
                .when()
                .get("/api/course")
                .then()
                .log().all()
                .statusCode(200)
                .body("courses", notNullValue())
                .body("pageSize", equalTo(3))
                .body("sortDirection", equalTo("desc"));
    }

    @Test
    @Order(26)
    @DisplayName("Should fail to get all courses without authentication")
    void testGetAllCoursesWithoutAuth() {
        given()
                .when()
                .get("/api/course")
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(27)
    @DisplayName("Should return courses with correct summary fields")
    void testGetAllCoursesReturnCorrectFields() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/course")
                .then()
                .log().all()
                .statusCode(200)
                .body("courses[0].courseId", notNullValue())
                .body("courses[0].courseName", notNullValue())
                .body("courses[0].courseCode", notNullValue())
                .body("courses[0].description", notNullValue())
                .body("courses[0].active", notNullValue())
                .body("courses[0].startDate", notNullValue())
                .body("courses[0].endDate", notNullValue());
    }

    @Test
    @Order(28)
    @DisplayName("Should return empty courses for new teacher with no courses")
    void testGetAllCoursesForNewTeacher() {
        // Register a new teacher with no courses
        String registerBody = """
                {
                    "firstName": "New",
                    "lastName": "Teacher",
                    "email": "new.nocourses@example.com",
                    "password": "NewTeacherPass123!",
                    "employeeId": "EMP-CRS005",
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

        String newTeacherToken = response.jsonPath().getString("token");

        // Get all courses for this new teacher (should be empty)
        given()
                .header("Authorization", "Bearer " + newTeacherToken)
                .when()
                .get("/api/course")
                .then()
                .log().all()
                .statusCode(200)
                .body("courses", hasSize(0))
                .body("totalCourses", equalTo(0));
    }

    @Test
    @Order(29)
    @DisplayName("Should get all courses and verify total count matches actual")
    void testGetAllCoursesVerifyTotalCount() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .queryParam("pageSize", 100)
                .when()
                .get("/api/course")
                .then()
                .log().all()
                .statusCode(200)
                .body("totalCourses", greaterThanOrEqualTo(1)); // Verify at least 1 course exists
    }
}
