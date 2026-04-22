package edu.ai.lms.core.history.adapter.in.http;

import edu.ai.lms.config.IntegrationTestConfiguration;
import edu.ai.lms.core.auth.app.port.out.LoadStudentPort;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.history.adapter.out.persistence.ActivityLogRepository;
import edu.ai.lms.core.history.app.domain.ActivityLog;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("History Controller Integration Tests")
class HistoryControllerIT extends IntegrationTestConfiguration {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private LoadStudentPort loadStudentPort;

    @Test
    @DisplayName("Should return paginated activity logs for authenticated user")
    void testGetMyHistory() {
        // Register a student to get a valid user ID and token
        String requestBody = """
                {
                    "firstName": "History",
                    "lastName": "Student",
                    "email": "history.student@example.com",
                    "password": "Password123!",
                    "studentId": "STU-HIST-001",
                    "enrollmentYear": 2024
                }
                """;

        Response registerResponse = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/auth/student/register")
                .then()
                .statusCode(201)
                .extract().response();

        String token = registerResponse.jsonPath().getString("token");
        
        // Load the created user to get their UUID
        UserAuth student = loadStudentPort.findByEmail("history.student@example.com").orElseThrow();

        // Seed logs in DB directly via repository
        for (int i = 0; i < 25; i++) {
            ActivityLog log = ActivityLog.builder()
                .userId(student.getId())
                .actionType("SUBMITTED")
                .entityType("QUIZ")
                .entityId(UUID.randomUUID())
                .entityName("Test Quiz " + i)
                .description("Submitted test quiz " + i)
                .build();
            activityLogRepository.insert(log);
        }

        // Test GET /api/history/me (page 0, size 20)
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/history/me?page=0&size=20")
                .then()
                .log().all()
                .statusCode(200)
                .body("content", hasSize(20))
                .body("pageNumber", equalTo(0))
                .body("pageSize", equalTo(20))
                .body("totalElements", equalTo(25))
                .body("totalPages", equalTo(2))
                .body("last", equalTo(false))
                .body("content[0].id", notNullValue())
                .body("content[0].actionType", equalTo("SUBMITTED"))
                .body("content[0].entityType", equalTo("QUIZ"));
        
        // Test GET /api/history/me (page 1, size 20)
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/history/me?page=1&size=20")
                .then()
                .log().all()
                .statusCode(200)
                .body("content", hasSize(5))
                .body("pageNumber", equalTo(1))
                .body("pageSize", equalTo(20))
                .body("totalElements", equalTo(25))
                .body("totalPages", equalTo(2))
                .body("last", equalTo(true));
    }
}