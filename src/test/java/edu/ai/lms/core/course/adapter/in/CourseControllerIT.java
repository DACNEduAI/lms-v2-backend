package edu.ai.lms.core.course.adapter.in;

import java.time.LocalDateTime;
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
@DisplayName("Course Controller Integration Tests")
class CourseControllerIT extends IntegrationTestConfiguration {

    private static String teacherToken;
    private static UUID createdCourseId;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

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
        LocalDateTime startDate = LocalDateTime.now().plusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusMonths(3);

        String requestBody = String.format("""
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
                """, startDate.format(formatter), endDate.format(formatter));

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
        .when()
                .post("/api/course")
        .then()
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
        String requestBody = String.format("""
                {
                    "courseName": "Unauthorized Course",
                    "description": "This should fail",
                    "courseCode": "CS102",
                    "active": true,
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": []
                }
                """, 
                LocalDateTime.now().plusDays(1).format(formatter), 
                LocalDateTime.now().plusMonths(3).format(formatter));

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .post("/api/course")
        .then()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(4)
    @DisplayName("Should fail to create course with duplicate course code")
    void testCreateCourseWithDuplicateCode() {
        String requestBody = String.format("""
                {
                    "courseName": "Another Java Course",
                    "description": "This should fail due to duplicate code",
                    "courseCode": "CS101",
                    "active": true,
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": []
                }
                """, 
                LocalDateTime.now().plusDays(1).format(formatter), 
                LocalDateTime.now().plusMonths(3).format(formatter));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
        .when()
                .post("/api/course")
        .then()
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
                .statusCode(400);
    }

    @Test
    @Order(6)
    @DisplayName("Should successfully modify an existing course")
    void testModifyCourse() {
        LocalDateTime newStartDate = LocalDateTime.now().plusDays(7);
        LocalDateTime newEndDate = LocalDateTime.now().plusMonths(6);

        String requestBody = String.format("""
                {
                    "courseId": "%s",
                    "courseName": "Advanced Java Programming",
                    "description": "Updated course description with advanced topics",
                    "courseCode": "CS101",
                    "active": true,
                    "coverImage": "https://example.com/advanced-java.jpg",
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": [
                        {
                            "code": "CLO-1",
                            "description": "Master advanced Java concepts"
                        }
                    ]
                }
                """, createdCourseId, 
                newStartDate.format(formatter), newEndDate.format(formatter));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
        .when()
                .put("/api/course")
        .then()
                .statusCode(200)
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("courseName", equalTo("Advanced Java Programming"))
                .body("description", equalTo("Updated course description with advanced topics"))
                .body("courseCode", equalTo("CS101"))
                .body("active", equalTo(true))
                .body("updatedAt", notNullValue());
    }

    @Test
    @Order(7)
    @DisplayName("Should fail to modify course without authentication")
    void testModifyCourseWithoutAuth() {
        String requestBody = String.format("""
                {
                    "courseId": "%s",
                    "courseName": "Unauthorized Modification",
                    "description": "This should fail",
                    "courseCode": "CS101",
                    "active": false,
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": []
                }
                """, createdCourseId,
                LocalDateTime.now().plusDays(1).format(formatter),
                LocalDateTime.now().plusMonths(3).format(formatter));

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .put("/api/course")
        .then()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(8)
    @DisplayName("Should fail to modify non-existent course")
    void testModifyNonExistentCourse() {
        UUID nonExistentCourseId = UUID.randomUUID();
        
        String requestBody = String.format("""
                {
                    "courseId": "%s",
                    "courseName": "Non-existent Course",
                    "description": "This should fail",
                    "courseCode": "CS999",
                    "active": true,
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": []
                }
                """, nonExistentCourseId,
                LocalDateTime.now().plusDays(1).format(formatter),
                LocalDateTime.now().plusMonths(3).format(formatter));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
        .when()
                .put("/api/course")
        .then()
                .statusCode(anyOf(is(404)));
    }

    @Test
    @Order(9)
    @DisplayName("Should fail to modify course by unauthorized teacher")
    void testModifyCourseByUnauthorizedTeacher() {
        // First, register another teacher
        String registerBody = """
                {
                    "firstName": "Another",
                    "lastName": "Teacher",
                    "email": "another.teacher@example.com",
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
                .statusCode(201)
                .extract().response();

        String anotherTeacherToken = response.jsonPath().getString("token");

        // Try to modify the course created by the first teacher
        String modifyBody = String.format("""
                {
                    "courseId": "%s",
                    "courseName": "Hijacked Course",
                    "description": "This should fail",
                    "courseCode": "CS101",
                    "active": false,
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": []
                }
                """, createdCourseId,
                LocalDateTime.now().plusDays(1).format(formatter),
                LocalDateTime.now().plusMonths(3).format(formatter));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + anotherTeacherToken)
                .body(modifyBody)
        .when()
                .put("/api/course")
        .then()
                .statusCode(anyOf(is(403), is(400), is(500)));
    }

    @Test
    @Order(10)
    @DisplayName("Should fail to delete course without authentication")
    void testDeleteCourseWithoutAuth() {
        given()
                .contentType(ContentType.JSON)
        .when()
                .delete("/api/course/" + createdCourseId)
        .then()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(11)
    @DisplayName("Should fail to delete non-existent course")
    void testDeleteNonExistentCourse() {
        UUID nonExistentCourseId = UUID.randomUUID();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
        .when()
                .delete("/api/course/" + nonExistentCourseId)
        .then()
                .statusCode(anyOf(is(404), is(400), is(500)));
    }

    @Test
    @Order(12)
    @DisplayName("Should successfully delete an existing course")
    void testDeleteCourse() {
        // First create a new course to delete
        String createBody = String.format("""
                {
                    "courseName": "Course To Delete",
                    "description": "This course will be deleted",
                    "courseCode": "DEL001",
                    "active": true,
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": []
                }
                """,
                LocalDateTime.now().plusDays(1).format(formatter),
                LocalDateTime.now().plusMonths(3).format(formatter));

        Response createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(createBody)
        .when()
                .post("/api/course")
        .then()
                .statusCode(201)
                .extract().response();

        UUID courseToDeleteId = UUID.fromString(createResponse.jsonPath().getString("courseId"));

        // Now delete the course
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
        .when()
                .delete("/api/course/" + courseToDeleteId)
        .then()
                .statusCode(200)
                .body("courseId", equalTo(courseToDeleteId.toString()))
                .body("message", equalTo("Course deleted successfully"))
                .body("success", equalTo(true));
    }

    @Test
    @Order(13)
    @DisplayName("Should fail to delete course by unauthorized teacher")
    void testDeleteCourseByUnauthorizedTeacher() {
        // Register a new teacher
        String registerBody = """
                {
                    "firstName": "Unauthorized",
                    "lastName": "Deleter",
                    "email": "unauthorized.deleter@example.com",
                    "password": "DeleterPass123!",
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
                .statusCode(201)
                .extract().response();

        String unauthorizedToken = response.jsonPath().getString("token");

        // Try to delete the course created by the first teacher
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + unauthorizedToken)
        .when()
                .delete("/api/course/" + createdCourseId)
        .then()
                .statusCode(anyOf(is(403), is(400), is(500)));
    }

    @Test
    @Order(14)
    @DisplayName("Should create course with multiple learning outcomes")
    void testCreateCourseWithMultipleLearningOutcomes() {
        LocalDateTime startDate = LocalDateTime.now().plusDays(10);
        LocalDateTime endDate = LocalDateTime.now().plusMonths(4);

        String requestBody = String.format("""
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
                """, startDate.format(formatter), endDate.format(formatter));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
        .when()
                .post("/api/course")
        .then()
                .statusCode(201)
                .body("courseId", notNullValue())
                .body("courseName", equalTo("Data Structures and Algorithms"))
                .body("courseCode", equalTo("CS201"));
    }

    @Test
    @Order(15)
    @DisplayName("Should create course with empty learning outcomes")
    void testCreateCourseWithEmptyLearningOutcomes() {
        LocalDateTime startDate = LocalDateTime.now().plusDays(15);
        LocalDateTime endDate = LocalDateTime.now().plusMonths(5);

        String requestBody = String.format("""
                {
                    "courseName": "Quick Course",
                    "description": "A course with no learning outcomes defined yet",
                    "courseCode": "CS301",
                    "active": true,
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": []
                }
                """, startDate.format(formatter), endDate.format(formatter));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
        .when()
                .post("/api/course")
        .then()
                .statusCode(201)
                .body("courseId", notNullValue())
                .body("courseName", equalTo("Quick Course"))
                .body("courseCode", equalTo("CS301"));
    }
}
