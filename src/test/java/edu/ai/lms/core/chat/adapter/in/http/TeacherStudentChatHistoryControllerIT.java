package edu.ai.lms.core.chat.adapter.in.http;

import edu.ai.lms.config.IntegrationTestConfiguration;
import edu.ai.lms.core.auth.app.port.out.LoadStudentPort;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.chat.app.domain.ChatMessage;
import edu.ai.lms.core.chat.adapter.out.persistence.ChatMessageRepository;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@DisplayName("Chat History Controller Integration Tests")
class TeacherStudentChatHistoryControllerIT extends IntegrationTestConfiguration {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private LoadStudentPort loadStudentPort;

    @Test
    @DisplayName("Should return paginated chat history between two users")
    void testGetChatHistory() {
        // Register User A
        String requestBodyA = """
                {
                    "firstName": "Alice",
                    "lastName": "Chatter",
                    "email": "alice.chat@example.com",
                    "password": "Password123!",
                    "studentId": "STU-CHAT-001",
                    "enrollmentYear": 2024
                }
                """;

        Response registerResponseA = given()
                .contentType(ContentType.JSON)
                .body(requestBodyA)
                .when()
                .post("/api/auth/student/register")
                .then()
                .statusCode(201)
                .extract().response();

        String tokenA = registerResponseA.jsonPath().getString("token");
        UserAuth userA = loadStudentPort.findByEmail("alice.chat@example.com").orElseThrow();

        // Register User B
        String requestBodyB = """
                {
                    "firstName": "Bob",
                    "lastName": "Listener",
                    "email": "bob.chat@example.com",
                    "password": "Password123!",
                    "studentId": "STU-CHAT-002",
                    "enrollmentYear": 2024
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBodyB)
                .when()
                .post("/api/auth/student/register")
                .then()
                .statusCode(201);

        UserAuth userB = loadStudentPort.findByEmail("bob.chat@example.com").orElseThrow();

        // Seed 25 chat messages between User A and User B
        for (int i = 0; i < 25; i++) {
            UUID sender = (i % 2 == 0) ? userA.getId() : userB.getId();
            UUID recipient = (i % 2 == 0) ? userB.getId() : userA.getId();

            ChatMessage msg = ChatMessage.builder()
                    .senderId(sender)
                    .recipientId(recipient)
                    .content("Message " + i)
                    .status("SENT")
                    .createdAt(LocalDateTime.now().minusMinutes(30 - i))
                    .build();
            chatMessageRepository.save(msg);
        }

        // Register User C to ensure filtering works
        String requestBodyC = """
                {
                    "firstName": "Charlie",
                    "lastName": "Quiet",
                    "email": "charlie.chat@example.com",
                    "password": "Password123!",
                    "studentId": "STU-CHAT-003",
                    "enrollmentYear": 2024
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBodyC)
                .when()
                .post("/api/auth/student/register")
                .then()
                .statusCode(201);

        UserAuth userC = loadStudentPort.findByEmail("charlie.chat@example.com").orElseThrow();

        // Add 1 random message to User C (to ensure filtering works)
        ChatMessage randomMsg = ChatMessage.builder()
                .senderId(userA.getId())
                .recipientId(userC.getId())
                .content("Irrelevant message")
                .status("SENT")
                .createdAt(LocalDateTime.now())
                .build();
        chatMessageRepository.save(randomMsg);

        // Fetch page 0 (size 20)
        given()
                .header("Authorization", "Bearer " + tokenA)
                .when()
                .get("/api/chat/{partnerId}/history?page=0&size=20", userB.getId())
                .then()
                .statusCode(200)
                .body("content", hasSize(20))
                .body("number", equalTo(0))
                .body("totalElements", equalTo(25))
                .body("totalPages", equalTo(2));

        // Fetch page 1 (size 20)
        given()
                .header("Authorization", "Bearer " + tokenA)
                .when()
                .get("/api/chat/{partnerId}/history?page=1&size=20", userB.getId())
                .then()
                .statusCode(200)
                .body("content", hasSize(5))
                .body("number", equalTo(1))
                .body("totalElements", equalTo(25))
                .body("totalPages", equalTo(2));
    }
}
