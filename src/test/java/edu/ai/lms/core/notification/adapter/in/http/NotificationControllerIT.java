package edu.ai.lms.core.notification.adapter.in.http;

import edu.ai.lms.config.IntegrationTestConfiguration;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Notification Controller Integration Tests")
class NotificationControllerIT extends IntegrationTestConfiguration {

    private static String teacherToken;
    private static UUID createdCourseId;

    @Test
    @Order(1)
    @DisplayName("Should register a teacher for notification tests")
    void testRegisterTeacher() {
        String requestBody = """
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

        Response response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/auth/teacher/register")
                .then()
                .statusCode(201)
                .extract().response();

        teacherToken = response.jsonPath().getString("token");
    }

    @Test
    @Order(2)
    @DisplayName("Should create a course for notification tests")
    void testCreateCourse() {
        String requestBody = """
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
                .body(requestBody)
                .when()
                .post("/api/course")
                .then()
                .statusCode(201)
                .extract().response();

        createdCourseId = UUID.fromString(response.jsonPath().getString("courseId"));
    }

    @Test
    @Order(3)
    @DisplayName("Should successfully send an announcement with type")
    void testSendAnnouncement_ShouldSucceed() {
        String requestBody = """
                {
                    "title": "Important Announcement",
                    "message": "Please review the new materials.",
                    "type": "URGENT"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/notifications/courses/" + createdCourseId + "/announce")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(4)
    @DisplayName("Should successfully get announcements for the course")
    void testGetAnnouncements_ShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/notifications/courses/" + createdCourseId + "/announcements")
                .then()
                .statusCode(200)
                .body("content", hasSize(greaterThanOrEqualTo(1)))
                .body("content[0].title", equalTo("Important Announcement"))
                .body("content[0].type", equalTo("URGENT"));
    }

    @Test
    @Order(5)
    @DisplayName("Should get unread count for current user")
    void testGetUnreadCount_ShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/notifications/me/unread-count")
                .then()
                .statusCode(200)
                .body("unreadCount", notNullValue());
    }
}
