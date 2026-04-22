package edu.ai.lms.core.quiz.adapter.in.http;

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
@DisplayName("Question Controller Integration Tests")
class QuestionControllerIT extends IntegrationTestConfiguration {

    private static String teacherToken;
    private static UUID createdCourseId;
    private static UUID createdQuizId;
    private static UUID createdQuestionId;
    private static UUID secondQuestionId;
    private static UUID thirdQuestionId;
    private static UUID questionLo1;
    private static UUID questionLo2;
    private static UUID foreignCourseId;
    private static UUID foreignLearningOutcomeId;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ==================== SETUP ====================

    @Test
    @Order(1)
    @DisplayName("Should register a teacher for question tests")
    void testRegisterTeacher() {
        String requestBody = """
                {
                    "firstName": "Question",
                    "lastName": "Teacher",
                    "email": "question.teacher@example.com",
                    "password": "TeacherPass123!",
                    "employeeId": "EMP-QST001",
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
                .body("token", notNullValue())
                .extract().response();

        teacherToken = response.jsonPath().getString("token");
    }

    @Test
    @Order(2)
    @DisplayName("Should create a course for question tests")
    void testCreateCourse() {
        String startDate = LocalDate.now().plusDays(1).format(DATE_FORMATTER);
        String endDate = LocalDate.now().plusMonths(3).format(DATE_FORMATTER);

        String requestBody = """
                {
                    "courseName": "Math for Question Tests",
                    "description": "Course used for question integration tests",
                    "courseCode": "QSTN101",
                    "active": true,
                    "coverImage": "https://example.com/math.jpg",
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": [
                        {
                            "code": "CLO-1",
                            "description": "Understand algebra"
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
                .extract().response();

        createdCourseId = UUID.fromString(response.jsonPath().getString("courseId"));
    }

    @Test
    @Order(3)
    @DisplayName("Should create a quiz for question tests")
    void testCreateQuiz() {
        String requestBody = """
                {
                    "quizTitle": "Algebra Quiz",
                    "unlimited": true,
                    "passScore": 60.0
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/quiz")
                .then()
                .log().all()
                .statusCode(201)
                .body("quizId", notNullValue())
                .extract().response();

        createdQuizId = UUID.fromString(response.jsonPath().getString("quizId"));
    }

    // ==================== CREATE QUESTION TESTS ====================

    @Test
    @Order(4)
    @DisplayName("Should successfully create multiple questions")
    void testCreateQuestion_ShouldSucceed() {
        String requestBody = """
                {
                    "questions": [
                    {
                        "content": "What is 2 + 2?",
                        "type": "MULTIPLE_CHOICE",
                        "points": 5.0,
                        "answers": [
                            {
                                "content": "4",
                                "isCorrect": true,
                                "explanation": "Correct"
                            },
                            {
                                "content": "5",
                                "isCorrect": false,
                                "explanation": "Incorrect"
                            }
                        ]
                    },
                    {
                        "content": "Is the Earth flat?",
                        "type": "TRUE_FALSE",
                        "points": 3.0,
                        "answers": [
                            {
                                "content": "False",
                                "isCorrect": true,
                                "explanation": "Earth is not flat"
                            }
                        ]
                    }
                    ]
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/quiz/" + createdQuizId + "/question")
                .then()
                .log().all()
                .statusCode(201)
                .body("", hasSize(2))
                .body("[0].questionId", notNullValue())
                .body("[0].content", equalTo("What is 2 + 2?"))
                .body("[0].answers", hasSize(2))
                .body("[1].questionId", notNullValue())
                .body("[1].content", equalTo("Is the Earth flat?"))
                .body("[1].answers", hasSize(1))
                .extract().response();

        createdQuestionId = UUID.fromString(response.jsonPath().getString("[0].questionId"));
        secondQuestionId = UUID.fromString(response.jsonPath().getString("[1].questionId"));
    }

    @Test
    @Order(5)
    @DisplayName("Should create another question for bulk delete test")
    void testCreateThirdQuestion_ShouldSucceed() {
        String requestBody = """
                {
                    "questions": [
                    {
                        "content": "What is 10 - 4?",
                        "type": "MULTIPLE_CHOICE",
                        "points": 2.0,
                        "answers": [
                            {
                                "content": "6",
                                "isCorrect": true,
                                "explanation": "Correct"
                            }
                        ]
                    }
                    ]
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/quiz/" + createdQuizId + "/question")
                .then()
                .log().all()
                .statusCode(201)
                .body("", hasSize(1))
                .body("[0].questionId", notNullValue())
                .body("[0].content", equalTo("What is 10 - 4?"))
                .extract().response();

        thirdQuestionId = UUID.fromString(response.jsonPath().getString("[0].questionId"));
    }

    @Test
    @Order(6)
    @DisplayName("Should fail to create question without authentication")
    void testCreateQuestion_WithoutAuth() {
        String requestBody = """
                {
                    "content": "Unauthorized question?",
                    "type": "ESSAY",
                    "points": 10.0
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/quiz/" + createdQuizId + "/question")
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(7)
    @DisplayName("Should fail to create question with missing content")
    void testCreateQuestion_WithMissingContent() {
        String requestBody = """
                {
                    "questions": [
                    {
                        "type": "MULTIPLE_CHOICE",
                        "points": 5.0
                    }
                    ]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/quiz/" + createdQuizId + "/question")
                .then()
                .log().all()
                .statusCode(400);
    }

    // ==================== GET QUESTION TESTS ====================

    @Test
    @Order(8)
    @DisplayName("Should successfully get a question by ID")
    void testGetQuestion_ShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/quiz/" + createdQuizId + "/question/" + createdQuestionId)
                .then()
                .log().all()
                .statusCode(200)
                .body("questionId", equalTo(createdQuestionId.toString()))
                .body("content", equalTo("What is 2 + 2?"))
                .body("type", equalTo("MULTIPLE_CHOICE"))
                .body("points", equalTo(5.0f))
                .body("quizId", equalTo(createdQuizId.toString()))
                .body("answers", notNullValue())
                .body("createdAt", notNullValue());
    }

    @Test
    @Order(9)
    @DisplayName("Should fail to get question without authentication")
    void testGetQuestion_WithoutAuth() {
        given()
                .when()
                .get("/api/quiz/" + createdQuizId + "/question/" + createdQuestionId)
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(10)
    @DisplayName("Should fail to get non-existent question")
    void testGetQuestion_WithNonExistentQuestion() {
        UUID nonExistentId = UUID.randomUUID();

        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/quiz/" + createdQuizId + "/question/" + nonExistentId)
                .then()
                .log().all()
                .statusCode(404);
    }

    // ==================== MODIFY QUESTION TESTS ====================

    @Test
    @Order(11)
    @DisplayName("Should successfully modify a question")
    void testModifyQuestion_ShouldSucceed() {
        String requestBody = """
                {
                    "questions": [
                    {
                        "questionId": "%s",
                        "content": "What is 2 + 3?",
                        "type": "MULTIPLE_CHOICE",
                        "points": 8.0,
                        "answers": [
                            {
                                "content": "5",
                                "isCorrect": true,
                                "explanation": "Correct"
                            }
                        ]
                    }
                    ]
                }
                """.formatted(createdQuestionId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/quiz/" + createdQuizId + "/question")
                .then()
                .log().all()
                .statusCode(200)
                .body("", hasSize(1))
                .body("[0].questionId", equalTo(createdQuestionId.toString()))
                .body("[0].content", equalTo("What is 2 + 3?"))
                .body("[0].type", equalTo("MULTIPLE_CHOICE"))
                .body("[0].points", equalTo(8.0f))
                .body("[0].quizId", equalTo(createdQuizId.toString()))
                .body("[0].answers", hasSize(1))
                .body("[0].updatedAt", notNullValue());
    }

    @Test
    @Order(12)
    @DisplayName("Should fail to modify question without authentication")
    void testModifyQuestion_WithoutAuth() {
        String requestBody = """
                {
                    "questions": [
                    {
                        "questionId": "%s",
                        "content": "Hacked question",
                        "type": "ESSAY",
                        "points": 100.0,
                        "answers": []
                    }
                    ]
                }
                """.formatted(createdQuestionId);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/quiz/" + createdQuizId + "/question")
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(13)
    @DisplayName("Should fail to modify non-existent question")
    void testModifyQuestion_WithNonExistentQuestion() {
        UUID nonExistentId = UUID.randomUUID();

        String requestBody = """
                {
                    "questions": [
                    {
                        "questionId": "%s",
                        "content": "Ghost question",
                        "type": "ESSAY",
                        "points": 10.0,
                        "answers": []
                    }
                    ]
                }
                """.formatted(nonExistentId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/quiz/" + createdQuizId + "/question")
                .then()
                .log().all()
                .statusCode(404);
    }

    // ==================== DELETE QUESTION TESTS ====================

    @Test
    @Order(14)
    @DisplayName("Should fail to delete question without authentication")
    void testDeleteQuestion_WithoutAuth() {
        given()
                .when()
                .delete("/api/quiz/" + createdQuizId + "/question/" + secondQuestionId)
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(15)
    @DisplayName("Should successfully delete questions in bulk")
    void testDeleteQuestion_ShouldSucceed() {
        String requestBody = """
                {
                    "questionIds": ["%s", "%s"]
                }
                """.formatted(secondQuestionId, thirdQuestionId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .delete("/api/quiz/" + createdQuizId + "/question")
                .then()
                .log().all()
                .statusCode(200)
                .body("", hasSize(2))
                .body("[0].message", equalTo("Question deleted successfully"))
                .body("[1].message", equalTo("Question deleted successfully"));
    }

    @Test
    @Order(16)
    @DisplayName("Should fail to delete non-existent question")
    void testDeleteQuestion_WithNonExistentQuestion() {
        UUID nonExistentId = UUID.randomUUID();
        String requestBody = """
                {
                    "questionIds": ["%s"]
                }
                """.formatted(nonExistentId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .delete("/api/quiz/" + createdQuizId + "/question")
                .then()
                .log().all()
                .statusCode(404);
    }

    // ==================== VERIFY STATE AFTER DELETE ====================

    @Test
    @Order(17)
    @DisplayName("Should verify the first question still exists after deleting the second")
    void testGetQuestion_AfterDelete() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/quiz/" + createdQuizId + "/question/" + createdQuestionId)
                .then()
                .log().all()
                .statusCode(200)
                .body("questionId", equalTo(createdQuestionId.toString()))
                .body("content", equalTo("What is 2 + 3?"));
    }

    @Test
    @Order(18)
    @DisplayName("Should create learning outcomes for question mapping")
    void testCreateLearningOutcomesForQuestionMapping() {
        String requestBody = """
                {
                    "learningOutcomes": [
                        {"code": "QLO-1", "description": "Question mapped LO 1"},
                        {"code": "QLO-2", "description": "Question mapped LO 2"}
                    ]
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/learning-outcome")
                .then()
                .statusCode(201)
                .body("totalCreated", equalTo(2))
                .extract().response();

        questionLo1 = UUID.fromString(response.jsonPath().getString("learningOutcomes[0].id"));
        questionLo2 = UUID.fromString(response.jsonPath().getString("learningOutcomes[1].id"));
    }

    @Test
    @Order(19)
    @DisplayName("Should add learning outcomes to a question")
    void testAddLearningOutcomesToQuestion_ShouldSucceed() {
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s", "%s"]
                }
                """.formatted(questionLo1, questionLo2);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/quiz/" + createdQuizId + "/question/" + createdQuestionId + "/learning-outcomes")
                .then()
                .statusCode(200)
                .body("operation", equalTo("ADD"))
                .body("learningOutcomeIds", hasSize(2));
    }

    @Test
    @Order(20)
    @DisplayName("Should get all learning outcomes mapped to a question")
    void testGetQuestionLearningOutcomes_ShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/quiz/" + createdQuizId + "/question/" + createdQuestionId + "/learning-outcomes")
                .then()
                .statusCode(200)
                .body("questionId", equalTo(createdQuestionId.toString()))
                .body("learningOutcomes", hasSize(2))
                .body("learningOutcomes.code", hasItem("QLO-1"))
                .body("learningOutcomes.code", hasItem("QLO-2"));
    }

    @Test
    @Order(21)
    @DisplayName("Should remove a learning outcome from a question")
    void testRemoveLearningOutcomesFromQuestion_ShouldSucceed() {
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(questionLo1);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .delete("/api/quiz/" + createdQuizId + "/question/" + createdQuestionId + "/learning-outcomes")
                .then()
                .statusCode(200)
                .body("operation", equalTo("REMOVE"))
                .body("learningOutcomeIds", hasSize(1));
    }

    @Test
    @Order(22)
    @DisplayName("Should reject adding learning outcomes from another course")
    void testAddLearningOutcomesToQuestion_FromDifferentCourse_ShouldFail() {
        String startDate = LocalDate.now().plusDays(2).format(DATE_FORMATTER);
        String endDate = LocalDate.now().plusMonths(4).format(DATE_FORMATTER);

        String createOtherCourseRequest = """
                {
                    "courseName": "Foreign Course For Question Mapping",
                    "description": "Course used to validate question LO course mismatch",
                    "courseCode": "QST-FOREIGN-101",
                    "active": true,
                    "coverImage": "https://example.com/foreign-question-course.jpg",
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": []
                }
                """.formatted(startDate, endDate);

        Response createOtherCourseResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(createOtherCourseRequest)
                .when()
                .post("/api/course")
                .then()
                .statusCode(201)
                .extract().response();

        foreignCourseId = UUID.fromString(createOtherCourseResponse.jsonPath().getString("courseId"));

        String createForeignLearningOutcomeRequest = """
                {
                    "learningOutcomes": [
                        {"code": "QLO-FOREIGN-1", "description": "Foreign question learning outcome"}
                    ]
                }
                """;

        Response createForeignLearningOutcomeResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(createForeignLearningOutcomeRequest)
                .when()
                .post("/api/" + foreignCourseId + "/learning-outcome")
                .then()
                .statusCode(201)
                .body("totalCreated", equalTo(1))
                .extract().response();

        foreignLearningOutcomeId = UUID.fromString(createForeignLearningOutcomeResponse.jsonPath().getString("learningOutcomes[0].id"));

        String addRequestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(foreignLearningOutcomeId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(addRequestBody)
                .when()
                .post("/api/quiz/" + createdQuizId + "/question/" + createdQuestionId + "/learning-outcomes")
                .then()
                .statusCode(403)
                .body("message", equalTo("Some learning outcomes do not belong to this course"));
    }
}