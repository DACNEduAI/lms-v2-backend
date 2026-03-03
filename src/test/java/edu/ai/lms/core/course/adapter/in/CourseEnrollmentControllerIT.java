package edu.ai.lms.core.course.adapter.in;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
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
@DisplayName("Course Enrollment Controller Integration Tests")
class CourseEnrollmentControllerIT extends IntegrationTestConfiguration {

    private static String teacherToken;
    private static String anotherTeacherToken;
    private static UUID createdCourseId;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ==================== SETUP ====================

    @Test
    @Order(1)
    @DisplayName("Should register a teacher for course-student tests")
    void testRegisterTeacher() {
        String requestBody = """
                {
                    "firstName": "CourseStudent",
                    "lastName": "Teacher",
                    "email": "cs.teacher@example.com",
                    "password": "TeacherPass123!",
                    "employeeId": "EMP-ENR001",
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
                .extract().response();

        teacherToken = response.jsonPath().getString("token");
    }

    @Test
    @Order(2)
    @DisplayName("Should register another teacher for unauthorized tests")
    void testRegisterAnotherTeacher() {
        String requestBody = """
                {
                    "firstName": "Other",
                    "lastName": "Teacher",
                    "email": "other.cs.teacher@example.com",
                    "password": "OtherPass123!",
                    "employeeId": "EMP-ENR002",
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
                .extract().response();

        anotherTeacherToken = response.jsonPath().getString("token");
    }

    @Test
    @Order(3)
    @DisplayName("Should create a course for student enrollment tests")
    void testCreateCourse() {
        String startDate = LocalDate.now().plusDays(1).format(DATE_FORMATTER);
        String endDate = LocalDate.now().plusMonths(3).format(DATE_FORMATTER);

        String requestBody = """
                {
                    "courseName": "Student Enrollment Course",
                    "description": "Course for testing student add/remove",
                    "courseCode": "CS_ENROLL_101",
                    "active": true,
                    "coverImage": "https://example.com/enroll.jpg",
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

    @Test
    @Order(4)
    @DisplayName("Should register student 1")
    void testRegisterStudent1() {
        String requestBody = """
                {
                    "firstName": "Alice",
                    "lastName": "Student",
                    "email": "alice.student@example.com",
                    "password": "StudentPass123!",
                    "studentId": "STU-ENR001",
                    "enrollmentYear": 2025
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/auth/student/register")
                .then()
                .log().all()
                .statusCode(201)
                .body("token", notNullValue());
    }

    @Test
    @Order(5)
    @DisplayName("Should register student 2")
    void testRegisterStudent2() {
        String requestBody = """
                {
                    "firstName": "Bob",
                    "lastName": "Student",
                    "email": "bob.student@example.com",
                    "password": "StudentPass123!",
                    "studentId": "STU-ENR002",
                    "enrollmentYear": 2025
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/auth/student/register")
                .then()
                .log().all()
                .statusCode(201)
                .body("token", notNullValue());
    }

    @Test
    @Order(6)
    @DisplayName("Should register student 3")
    void testRegisterStudent3() {
        String requestBody = """
                {
                    "firstName": "Charlie",
                    "lastName": "Student",
                    "email": "charlie.student@example.com",
                    "password": "StudentPass123!",
                    "studentId": "STU-ENR003",
                    "enrollmentYear": 2025
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/auth/student/register")
                .then()
                .log().all()
                .statusCode(201)
                .body("token", notNullValue());
    }

    // ==================== ADD STUDENTS TESTS ====================

    @Test
    @Order(7)
    @DisplayName("Should successfully add students to the course")
    void testAddStudentsToCourse_Success() {
        String requestBody = """
                {
                    "studentEmails": ["alice.student@example.com", "bob.student@example.com"]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/course/" + createdCourseId + "/students/add")
                .then()
                .log().all()
                .statusCode(200)
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("totalAdded", equalTo(2))
                .body("addedStudentEmails", hasSize(2))
                .body("addedStudentEmails", hasItem("alice.student@example.com"))
                .body("addedStudentEmails", hasItem("bob.student@example.com"))
                .body("message", equalTo("Students added to course successfully"));
    }

    @Test
    @Order(8)
    @DisplayName("Should skip duplicate students when adding again")
    void testAddStudentsToCourse_SkipDuplicates() {
        String requestBody = """
                {
                    "studentEmails": ["alice.student@example.com", "charlie.student@example.com"]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/course/" + createdCourseId + "/students/add")
                .then()
                .log().all()
                .statusCode(200)
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("totalAdded", equalTo(1))
                .body("addedStudentEmails", hasSize(1))
                .body("addedStudentEmails", hasItem("charlie.student@example.com"));
    }

    @Test
    @Order(9)
    @DisplayName("Should fail to add students without authentication")
    void testAddStudentsToCourse_WithoutAuth() {
        String requestBody = """
                {
                    "studentEmails": ["alice.student@example.com"]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/course/" + createdCourseId + "/students/add")
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(10)
    @DisplayName("Should fail to add students with empty email list")
    void testAddStudentsToCourse_EmptyList() {
        String requestBody = """
                {
                    "studentEmails": []
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/course/" + createdCourseId + "/students/add")
                .then()
                .log().all()
                .statusCode(400);
    }

    @Test
    @Order(11)
    @DisplayName("Should fail to add students to non-existent course")
    void testAddStudentsToCourse_CourseNotFound() {
        UUID nonExistentCourseId = UUID.randomUUID();

        String requestBody = """
                {
                    "studentEmails": ["alice.student@example.com"]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/course/" + nonExistentCourseId + "/students/add")
                .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @Order(12)
    @DisplayName("Should fail to add students by unauthorized teacher")
    void testAddStudentsToCourse_UnauthorizedTeacher() {
        String requestBody = """
                {
                    "studentEmails": ["alice.student@example.com"]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + anotherTeacherToken)
                .body(requestBody)
                .when()
                .put("/api/course/" + createdCourseId + "/students/add")
                .then()
                .log().all()
                .statusCode(403);
    }

    @Test
    @Order(13)
    @DisplayName("Should fail to add students with non-existent email")
    void testAddStudentsToCourse_StudentsNotFound() {
        String requestBody = """
                {
                    "studentEmails": ["alice.student@example.com", "nonexistent@example.com"]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/course/" + createdCourseId + "/students/add")
                .then()
                .log().all()
                .statusCode(404)
                .body("error", equalTo("Students Not Found"))
                .body("details", hasSize(1))
                .body("details", hasItem("nonexistent@example.com"));
    }

    @Test
    @Order(14)
    @DisplayName("Should fail to add students with malformed JSON")
    void testAddStudentsToCourse_MalformedJson() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body("{ invalid json }")
                .when()
                .put("/api/course/" + createdCourseId + "/students/add")
                .then()
                .log().all()
                .statusCode(400);
    }

    // ==================== REMOVE STUDENTS TESTS ====================

    @Test
    @Order(15)
    @DisplayName("Should successfully remove a single student from the course")
    void testRemoveStudentsFromCourse_SingleStudent() {
        String requestBody = """
                {
                    "studentEmails": ["charlie.student@example.com"]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/course/" + createdCourseId + "/students/remove")
                .then()
                .log().all()
                .statusCode(200)
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("totalRemoved", equalTo(1))
                .body("removedStudentEmails", hasSize(1))
                .body("removedStudentEmails", hasItem("charlie.student@example.com"))
                .body("message", equalTo("Students removed from course successfully"));
    }

    @Test
    @Order(16)
    @DisplayName("Should successfully remove multiple students from the course")
    void testRemoveStudentsFromCourse_MultipleStudents() {
        String requestBody = """
                {
                    "studentEmails": ["alice.student@example.com", "bob.student@example.com"]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/course/" + createdCourseId + "/students/remove")
                .then()
                .log().all()
                .statusCode(200)
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("totalRemoved", equalTo(2))
                .body("removedStudentEmails", hasSize(2));
    }

    @Test
    @Order(17)
    @DisplayName("Should fail to remove students without authentication")
    void testRemoveStudentsFromCourse_WithoutAuth() {
        String requestBody = """
                {
                    "studentEmails": ["alice.student@example.com"]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/course/" + createdCourseId + "/students/remove")
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(18)
    @DisplayName("Should fail to remove students with empty email list")
    void testRemoveStudentsFromCourse_EmptyList() {
        String requestBody = """
                {
                    "studentEmails": []
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/course/" + createdCourseId + "/students/remove")
                .then()
                .log().all()
                .statusCode(400);
    }

    @Test
    @Order(19)
    @DisplayName("Should fail to remove students from non-existent course")
    void testRemoveStudentsFromCourse_CourseNotFound() {
        UUID nonExistentCourseId = UUID.randomUUID();

        String requestBody = """
                {
                    "studentEmails": ["alice.student@example.com"]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/course/" + nonExistentCourseId + "/students/remove")
                .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @Order(20)
    @DisplayName("Should fail to remove students by unauthorized teacher")
    void testRemoveStudentsFromCourse_UnauthorizedTeacher() {
        // First, re-add a student so there's someone to attempt to remove
        String addBody = """
                {
                    "studentEmails": ["alice.student@example.com"]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(addBody)
                .when()
                .put("/api/course/" + createdCourseId + "/students/add")
                .then()
                .statusCode(200);

        // Try to remove as unauthorized teacher
        String requestBody = """
                {
                    "studentEmails": ["alice.student@example.com"]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + anotherTeacherToken)
                .body(requestBody)
                .when()
                .put("/api/course/" + createdCourseId + "/students/remove")
                .then()
                .log().all()
                .statusCode(403);
    }

    @Test
    @Order(21)
    @DisplayName("Should fail to remove students with non-existent email")
    void testRemoveStudentsFromCourse_StudentsNotFound() {
        String requestBody = """
                {
                    "studentEmails": ["nonexistent@example.com"]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/course/" + createdCourseId + "/students/remove")
                .then()
                .log().all()
                .statusCode(404)
                .body("error", equalTo("Students Not Found"))
                .body("details", hasSize(1))
                .body("details", hasItem("nonexistent@example.com"));
    }

    @Test
    @Order(22)
    @DisplayName("Should fail to remove students who are not enrolled in the course")
    void testRemoveStudentsFromCourse_StudentsNotEnrolled() {
        // charlie is not enrolled (was removed in test 15), try to remove
        String requestBody = """
                {
                    "studentEmails": ["charlie.student@example.com"]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/course/" + createdCourseId + "/students/remove")
                .then()
                .log().all()
                .statusCode(400)
                .body("error", equalTo("Students Not Enrolled"))
                .body("details", hasSize(1))
                .body("details", hasItem("charlie.student@example.com"));
    }

    @Test
    @Order(23)
    @DisplayName("Should fail to remove students with malformed JSON")
    void testRemoveStudentsFromCourse_MalformedJson() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body("{ invalid json }")
                .when()
                .put("/api/course/" + createdCourseId + "/students/remove")
                .then()
                .log().all()
                .statusCode(400);
    }
}
