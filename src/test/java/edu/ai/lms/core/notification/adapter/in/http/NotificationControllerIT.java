package edu.ai.lms.core.notification.adapter.in.http;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
@DisplayName("Notification Controller Integration Tests")
class NotificationControllerIT extends IntegrationTestConfiguration {

    private static String teacherToken;
    private static String studentToken;
    private static UUID createdCourseId;
    private static final String STUDENT_EMAIL = "notify.student@example.com";

    @Test
    @Order(1)
    @DisplayName("Should register users for notification tests")
    void testRegisterUsers() {
        // Register Teacher
        String teacherRegisterBody = """
                {
                    "firstName": "Notify",
                    "lastName": "Teacher",
                    "email": "notify.teacher@example.com",
                    "password": "TeacherPass123!",
                    "employeeId": "EMP-NOTIFY001",
                    "department": "Computer Science",
                    "specialization": "Software Engineering"
                }
                """;

        teacherToken = given()
                .contentType(ContentType.JSON)
                .body(teacherRegisterBody)
                .when()
                .post("/api/auth/teacher/register")
                .then()
                .statusCode(201)
                .extract().path("token");

        // Register Student
        String studentRegisterBody = """
                {
                    "firstName": "Notify",
                    "lastName": "Student",
                    "email": "%s",
                    "password": "StudentPass123!",
                    "studentId": "STU-NOTIFY001",
                    "enrollmentYear": 2026
                }
                """.formatted(STUDENT_EMAIL);

        studentToken = given()
                .contentType(ContentType.JSON)
                .body(studentRegisterBody)
                .when()
                .post("/api/auth/student/register")
                .then()
                .statusCode(201)
                .extract().path("token");
    }

    @Test
    @Order(2)
    @DisplayName("Should create course and enroll student")
    void testCreateCourseAndEnrollStudent() {
        // Create Course
        String createCourseBody = """
                {
                    "courseName": "Notification Test Course",
                    "description": "Course used for notification integration tests",
                    "courseCode": "NOTIFY101",
                    "active": true,
                    "startDate": "10-01-2026",
                    "endDate": "10-04-2026"
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(createCourseBody)
                .when()
                .post("/api/course")
                .then()
                .statusCode(201)
                .extract().response();

        createdCourseId = UUID.fromString(response.jsonPath().getString("courseId"));

        // Enroll Student
        String enrollBody = """
                {
                    "studentEmails": ["%s"]
                }
                """.formatted(STUDENT_EMAIL);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(enrollBody)
                .when()
                .put("/api/course/" + createdCourseId + "/students/add")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(3)
    @DisplayName("Should successfully send an announcement and verify student receives notification")
    void testSendAnnouncement_AndVerifyReception() {
        String requestBody = """
                {
                    "title": "Important Announcement",
                    "message": "Please review the new materials.",
                    "type": "URGENT"
                }
                """;

        // Teacher sends announcement
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/notifications/courses/" + createdCourseId + "/announce")
                .then()
                .statusCode(200);

        // Student checks their notifications
        given()
                .header("Authorization", "Bearer " + studentToken)
                .when()
                .get("/api/notifications/me")
                .then()
                .statusCode(200)
                .body("content", hasSize(greaterThanOrEqualTo(1)))
                .body("content[0].title", hasItem("Chào mừng khóa học mới!"))
                .body("content[0].type", hasItem("URGENT"))
                .body("content[0].read", is(false));
    }

    @Test
    @Order(4)
    @DisplayName("Should mark a notification as read and verify unread count decreases")
    void testMarkAsRead_ShouldSucceed() {
        // Get student's notification ID
        Response response = given()
                .header("Authorization", "Bearer " + studentToken)
                .when()
                .get("/api/notifications/me")
                .then()
                .statusCode(200)
                .extract().response();

        String notificationId = response.jsonPath().getString("content[0].id");

        // Get initial unread count
        int initialUnread = given()
                .header("Authorization", "Bearer " + studentToken)
                .when()
                .get("/api/notifications/me/unread-count")
                .then()
                .statusCode(200)
                .extract().path("unreadCount");

        // Mark as read
        given()
                .header("Authorization", "Bearer " + studentToken)
                .when()
                .put("/api/notifications/" + notificationId + "/read")
                .then()
                .statusCode(204);

        // Verify unread count decreased
        given()
                .header("Authorization", "Bearer " + studentToken)
                .when()
                .get("/api/notifications/me/unread-count")
                .then()
                .statusCode(200)
                .body("unreadCount", equalTo(initialUnread - 1));
    }

    @Test
    @Order(5)
    @DisplayName("Should mark all notifications as read")
    void testMarkAllAsRead_ShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + studentToken)
                .when()
                .put("/api/notifications/read-all")
                .then()
                .statusCode(204);

        given()
                .header("Authorization", "Bearer " + studentToken)
                .when()
                .get("/api/notifications/me/unread-count")
                .then()
                .statusCode(200)
                .body("unreadCount", equalTo(0));
    }

    @Test
    @Order(6)
    @DisplayName("Should reject student trying to send announcement")
    void testSecurity_StudentCannotAnnounce() {
        String requestBody = """
                {
                    "title": "I am a hacker student",
                    "message": "I want to send fake urgent news.",
                    "type": "URGENT"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + studentToken)
                .body(requestBody)
                .when()
                .post("/api/notifications/courses/" + createdCourseId + "/announce")
                .then()
                .statusCode(403);
    }

}
