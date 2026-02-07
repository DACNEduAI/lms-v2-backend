package edu.ai.lms.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/**
 * Utility class for common test operations in integration tests.
 * Provides helper methods for REST API testing with REST Assured.
 */
public class TestHelper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converts an object to JSON string
     *
     * @param object the object to convert
     * @return JSON string representation
     */
    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }

    /**
     * Performs a POST request and returns the response
     *
     * @param endpoint the API endpoint
     * @param requestBody the request body as JSON string
     * @return Response object
     */
    public static Response post(String endpoint, String requestBody) {
        return given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post(endpoint);
    }

    /**
     * Performs a POST request with authentication token
     *
     * @param endpoint the API endpoint
     * @param requestBody the request body as JSON string
     * @param token the JWT token for authentication
     * @return Response object
     */
    public static Response postWithAuth(String endpoint, String requestBody, String token) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(requestBody)
                .when()
                .post(endpoint);
    }

    /**
     * Performs a GET request
     *
     * @param endpoint the API endpoint
     * @return Response object
     */
    public static Response get(String endpoint) {
        return given()
                .when()
                .get(endpoint);
    }

    /**
     * Performs a GET request with authentication token
     *
     * @param endpoint the API endpoint
     * @param token the JWT token for authentication
     * @return Response object
     */
    public static Response getWithAuth(String endpoint, String token) {
        return given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(endpoint);
    }

    /**
     * Registers a student and returns the authentication token
     *
     * @param email student email
     * @param password student password
     * @param firstName student first name
     * @param lastName student last name
     * @param enrollmentYear enrollment year
     * @return JWT token
     */
    public static String registerStudentAndGetToken(String email, String password, 
                                                     String firstName, String lastName, 
                                                     Long enrollmentYear) {
        String requestBody = String.format("""
                {
                    "firstName": "%s",
                    "lastName": "%s",
                    "email": "%s",
                    "password": "%s",
                    "enrollmentYear": %d
                }
                """, firstName, lastName, email, password, enrollmentYear);

        return post("/api/auth/student/register", requestBody)
                .then()
                .statusCode(201)
                .extract()
                .path("token");
    }

    /**
     * Registers a teacher and returns the authentication token
     *
     * @param email teacher email
     * @param password teacher password
     * @param firstName teacher first name
     * @param lastName teacher last name
     * @return JWT token
     */
    public static String registerTeacherAndGetToken(String email, String password, 
                                                     String firstName, String lastName) {
        String requestBody = String.format("""
                {
                    "firstName": "%s",
                    "lastName": "%s",
                    "email": "%s",
                    "password": "%s"
                }
                """, firstName, lastName, email, password);

        return post("/api/auth/teacher/register", requestBody)
                .then()
                .statusCode(201)
                .extract()
                .path("token");
    }

    /**
     * Student login and returns the authentication token
     *
     * @param email student email
     * @param password student password
     * @return JWT token
     */
    public static String loginStudentAndGetToken(String email, String password) {
        String requestBody = String.format("""
                {
                    "email": "%s",
                    "password": "%s"
                }
                """, email, password);

        return post("/api/auth/student/login", requestBody)
                .then()
                .statusCode(200)
                .extract()
                .path("token");
    }

    /**
     * Teacher login and returns the authentication token
     *
     * @param email teacher email
     * @param password teacher password
     * @return JWT token
     */
    public static String loginTeacherAndGetToken(String email, String password) {
        String requestBody = String.format("""
                {
                    "email": "%s",
                    "password": "%s"
                }
                """, email, password);

        return post("/api/auth/teacher/login", requestBody)
                .then()
                .statusCode(200)
                .extract()
                .path("token");
    }

    /**
     * Generates a unique email address for testing
     *
     * @param prefix email prefix
     * @return unique email address
     */
    public static String generateUniqueEmail(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "@example.com";
    }

    /**
     * Generates a random student email
     *
     * @return unique student email
     */
    public static String generateStudentEmail() {
        return generateUniqueEmail("student");
    }

    /**
     * Generates a random teacher email
     *
     * @return unique teacher email
     */
    public static String generateTeacherEmail() {
        return generateUniqueEmail("teacher");
    }
}
