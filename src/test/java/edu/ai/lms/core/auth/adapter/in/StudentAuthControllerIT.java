package edu.ai.lms.core.auth.adapter.in;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import edu.ai.lms.config.IntegrationTestConfiguration;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Student Auth Controller Integration Tests")
class StudentAuthControllerIT extends IntegrationTestConfiguration {

    @Test
    @Order(1)
    @DisplayName("Should successfully register a new student")
    void testStudentRegistration() {
        String requestBody = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john.doe@example.com",
                    "password": "SecurePass123!",
                    "studentId": "STU-IT001",
                    "enrollmentYear": 2024
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .post("/api/auth/student/register")
        .then()
                .statusCode(201)
                .body("token", notNullValue())
                .body("token", not(emptyString()))
                .body("type", equalTo("Bearer"))
                .body("email", equalTo("john.doe@example.com"));
    }

    @Test
    @Order(2)
    @DisplayName("Should fail registration with invalid email format")
    void testStudentRegistrationWithInvalidEmail() {
        String requestBody = """
                {
                    "firstName": "Jane",
                    "lastName": "Doe",
                    "email": "invalid-email",
                    "password": "SecurePass123!",
                    "studentId": "STU-IT002",
                    "enrollmentYear": 2024
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .post("/api/auth/student/register")
        .then()
                .statusCode(400);
    }

    @Test
    @Order(3)
    @DisplayName("Should fail registration with missing required fields")
    void testStudentRegistrationWithMissingFields() {
        String requestBody = """
                {
                    "firstName": "Jane",
                    "email": "jane.doe@example.com"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .post("/api/auth/student/register")
        .then()
                .statusCode(400);
    }

    @Test
    @Order(4)
    @DisplayName("Should fail registration with short password")
    void testStudentRegistrationWithShortPassword() {
        String requestBody = """
                {
                    "firstName": "Jane",
                    "lastName": "Doe",
                    "email": "jane.short@example.com",
                    "password": "short",
                    "studentId": "STU-IT003",
                    "enrollmentYear": 2024
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .post("/api/auth/student/register")
        .then()
                .statusCode(400);
    }

    @Test
    @Order(5)
    @DisplayName("Should successfully login with valid credentials")
    void testStudentLogin() {
        // First register a student
        String registerBody = """
                {
                    "firstName": "Alice",
                    "lastName": "Smith",
                    "email": "alice.smith@example.com",
                    "password": "AlicePass123!",
                    "studentId": "STU-IT004",
                    "enrollmentYear": 2024
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(registerBody)
        .when()
                .post("/api/auth/student/register")
        .then()
                .statusCode(201);

        // Then login
        String loginBody = """
                {
                    "email": "alice.smith@example.com",
                    "password": "AlicePass123!"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(loginBody)
        .when()
                .post("/api/auth/student/login")
        .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("token", not(emptyString()))
                .body("type", equalTo("Bearer"))
                .body("email", equalTo("alice.smith@example.com"))
                .body("firstName", equalTo("Alice"))
                .body("lastName", equalTo("Smith"))
                .body("role", notNullValue());
    }

    @Test
    @Order(6)
    @DisplayName("Should fail login with invalid credentials")
    void testStudentLoginWithInvalidCredentials() {
        String loginBody = """
                {
                    "email": "nonexistent@example.com",
                    "password": "WrongPass123!"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(loginBody)
        .when()
                .post("/api/auth/student/login")
        .then()
                .statusCode(anyOf(is(401), is(403), is(400)));
    }

    @Test
    @Order(7)
    @DisplayName("Should fail login with wrong password")
    void testStudentLoginWithWrongPassword() {
        // First register a student
        String registerBody = """
                {
                    "firstName": "Bob",
                    "lastName": "Johnson",
                    "email": "bob.johnson@example.com",
                    "password": "BobPass123!",
                    "studentId": "STU-IT005",
                    "enrollmentYear": 2024
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(registerBody)
        .when()
                .post("/api/auth/student/register")
        .then()
                .statusCode(201);

        // Try login with wrong password
        String loginBody = """
                {
                    "email": "bob.johnson@example.com",
                    "password": "WrongPassword123!"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(loginBody)
        .when()
                .post("/api/auth/student/login")
        .then()
                .statusCode(anyOf(is(401), is(403), is(400)));
    }

    @Test
    @Order(8)
    @DisplayName("Should fail login with invalid email format")
    void testStudentLoginWithInvalidEmail() {
        String loginBody = """
                {
                    "email": "invalid-email",
                    "password": "SomePass123!"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(loginBody)
        .when()
                .post("/api/auth/student/login")
        .then()
                .statusCode(400);
    }

    @Test
    @Order(9)
    @DisplayName("Should handle duplicate registration")
    void testDuplicateStudentRegistration() {
        String requestBody = """
                {
                    "firstName": "Duplicate",
                    "lastName": "User",
                    "email": "duplicate.user@example.com",
                    "password": "DuplicatePass123!",
                    "studentId": "STU-IT006",
                    "enrollmentYear": 2024
                }
                """;

        // First registration should succeed
        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .post("/api/auth/student/register")
        .then()
                .statusCode(201);

        // Second registration with same email should fail
        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .post("/api/auth/student/register")
        .then()
                .statusCode(anyOf(is(409), is(400), is(500)));
    }
}
