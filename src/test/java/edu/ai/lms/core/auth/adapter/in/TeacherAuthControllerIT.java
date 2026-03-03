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
@DisplayName("Teacher Auth Controller Integration Tests")
class TeacherAuthControllerIT extends IntegrationTestConfiguration {

    // The HTTP response code in all of these tests are not entirely correct
    // will clean this soon, it's not a big problem now.

    @Test
    @Order(1)
    @DisplayName("Should successfully register a new teacher")
    void testTeacherRegistration() {
        String requestBody = """
                {
                    "firstName": "Dr. Jane",
                    "lastName": "Smith",
                    "email": "jane.smith@university.edu",
                    "password": "TeacherPass123!",
                    "employeeId": "EMP-IT001",
                    "phoneNumber": "+1234567890",
                    "department": "Computer Science",
                    "specialization": "Machine Learning"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .post("/api/auth/teacher/register")
        .then()
                .log().all()
                .statusCode(201)
                .body("token", notNullValue())
                .body("token", not(emptyString()))
                .body("type", equalTo("Bearer"))
                .body("email", equalTo("jane.smith@university.edu"));
    }

    @Test
    @Order(2)
    @DisplayName("Should successfully register teacher with minimal required fields")
    void testTeacherRegistrationMinimalFields() {
        String requestBody = """
                {
                    "firstName": "Prof. John",
                    "lastName": "Doe",
                    "email": "john.doe@university.edu",
                    "password": "ProfPass123!",
                    "employeeId": "EMP-IT002"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .post("/api/auth/teacher/register")
        .then()
                .statusCode(201)
                .body("token", notNullValue())
                .body("email", equalTo("john.doe@university.edu"));
    }

    @Test
    @Order(3)
    @DisplayName("Should fail registration with invalid email format")
    void testTeacherRegistrationWithInvalidEmail() {
        String requestBody = """
                {
                    "firstName": "Prof. Test",
                    "lastName": "User",
                    "email": "invalid-email-format",
                    "password": "TestPass123!",
                    "employeeId": "EMP-IT003"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .post("/api/auth/teacher/register")
        .then()
                .statusCode(400);
    }

    @Test
    @Order(4)
    @DisplayName("Should fail registration with missing required fields")
    void testTeacherRegistrationWithMissingFields() {
        String requestBody = """
                {
                    "firstName": "Prof. Test",
                    "email": "test@university.edu"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .post("/api/auth/teacher/register")
        .then()
                .statusCode(400);
    }

    @Test
    @Order(5)
    @DisplayName("Should successfully login with valid credentials")
    void testTeacherLogin() {
        // First register a teacher
        String registerBody = """
                {
                    "firstName": "Prof. Alice",
                    "lastName": "Johnson",
                    "email": "alice.johnson@university.edu",
                    "password": "AliceTeacher123!",
                    "employeeId": "EMP-IT004",
                    "department": "Mathematics"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(registerBody)
        .when()
                .post("/api/auth/teacher/register")
        .then()
                .statusCode(201);

        // Then login
        String loginBody = """
                {
                    "email": "alice.johnson@university.edu",
                    "password": "AliceTeacher123!"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(loginBody)
        .when()
                .post("/api/auth/teacher/login")
        .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("token", not(emptyString()))
                .body("type", equalTo("Bearer"))
                .body("email", equalTo("alice.johnson@university.edu"))
                .body("firstName", equalTo("Prof. Alice"))
                .body("lastName", equalTo("Johnson"))
                .body("role", notNullValue());
    }

    @Test
    @Order(6)
    @DisplayName("Should fail login with invalid credentials")
    void testTeacherLoginWithInvalidCredentials() {
        String loginBody = """
                {
                    "email": "nonexistent.teacher@university.edu",
                    "password": "WrongPass123!"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(loginBody)
        .when()
                .post("/api/auth/teacher/login")
        .then()
                .statusCode(anyOf(is(401), is(403), is(400)));
    }

    @Test
    @Order(7)
    @DisplayName("Should fail login with wrong password")
    void testTeacherLoginWithWrongPassword() {
        // First register a teacher
        String registerBody = """
                {
                    "firstName": "Prof. Bob",
                    "lastName": "Williams",
                    "email": "bob.williams@university.edu",
                    "password": "BobTeacher123!",
                    "employeeId": "EMP-IT005",
                    "department": "Physics"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(registerBody)
        .when()
                .post("/api/auth/teacher/register")
        .then()
                .statusCode(201);

        // Try login with wrong password
        String loginBody = """
                {
                    "email": "bob.williams@university.edu",
                    "password": "WrongPassword123!"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(loginBody)
        .when()
                .post("/api/auth/teacher/login")
        .then()
                .statusCode(anyOf(is(401), is(403), is(400)));
    }

    @Test
    @Order(8)
    @DisplayName("Should fail login with invalid email format")
    void testTeacherLoginWithInvalidEmail() {
        String loginBody = """
                {
                    "email": "not-an-email",
                    "password": "SomePass123!"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(loginBody)
        .when()
                .post("/api/auth/teacher/login")
        .then()
                .statusCode(400);
    }

    @Test
    @Order(9)
    @DisplayName("Should handle duplicate registration")
    void testDuplicateTeacherRegistration() {
        String requestBody = """
                {
                    "firstName": "Prof. Duplicate",
                    "lastName": "Test",
                    "email": "duplicate.teacher@university.edu",
                    "password": "DuplicatePass123!",
                    "employeeId": "EMP-IT006",
                    "department": "Engineering"
                }
                """;

        // First registration should succeed
        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .post("/api/auth/teacher/register")
        .then()
                .statusCode(201);

        // Second registration with same email should fail
        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .post("/api/auth/teacher/register")
        .then()
                .statusCode(is(400));
    }

    @Test
    @Order(10)
    @DisplayName("Should register teacher with all optional fields")
    void testTeacherRegistrationWithAllFields() {
        String requestBody = """
                {
                    "firstName": "Dr. Complete",
                    "lastName": "Profile",
                    "email": "complete.profile@university.edu",
                    "password": "CompletePass123!",
                    "employeeId": "EMP-IT007",
                    "phoneNumber": "+9876543210",
                    "department": "Biology",
                    "specialization": "Genetics"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .post("/api/auth/teacher/register")
        .then()
                .statusCode(201)
                .body("token", notNullValue())
                .body("email", equalTo("complete.profile@university.edu"));
    }
}
