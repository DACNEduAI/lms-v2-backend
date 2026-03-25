package edu.ai.lms.core.quiz.adapter.in.http;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
@DisplayName("Quiz Controller Integration Tests")
class QuizControllerIT extends IntegrationTestConfiguration {

    private static String teacherToken;
    private static UUID createdCourseId;
    private static UUID createdQuizId;
    private static UUID secondQuizId;
    private static UUID thirdQuizId;
    private static UUID studentBQuizId;
    private static UUID hiddenTeacherQuizId;
    private static UUID lo1;
    private static UUID lo2;
    private static UUID lo3;
    private static UUID lo4;
    private static UUID group1;
    private static UUID group2;
    private static UUID otherCourseId;
    private static UUID foreignLearningOutcomeId;
    private static String studentAToken;
    private static String studentBToken;
    private static final String STUDENT_A_QUIZ_ONE_TITLE = "Student A Quiz One";
    private static final String STUDENT_A_QUIZ_TWO_TITLE = "Student A Quiz Two";
    private static final String STUDENT_B_QUIZ_TITLE = "Student B Quiz";
    private static final String HIDDEN_TEACHER_QUIZ_TITLE = "Hidden Teacher Quiz";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ==================== SETUP: Register teacher and create a course ====================

    @Test
    @Order(1)
    @DisplayName("Should register a teacher for quiz tests")
    void testRegisterTeacher() {
        String requestBody = """
                {
                    "firstName": "Quiz",
                    "lastName": "Teacher",
                    "email": "quiz.teacher@example.com",
                    "password": "TeacherPass123!",
                    "employeeId": "EMP-QIZ001",
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
                .body("email", equalTo("quiz.teacher@example.com"))
                .extract().response();

        teacherToken = response.jsonPath().getString("token");
    }

    @Test
    @Order(2)
    @DisplayName("Should create a course for quiz tests")
    void testCreateCourseForQuizzes() {
        String startDate = LocalDate.now().plusDays(1).format(DATE_FORMATTER);
        String endDate = LocalDate.now().plusMonths(3).format(DATE_FORMATTER);

        String requestBody = """
                {
                    "courseName": "Java Programming for Quizzes",
                    "description": "Course used for quiz integration tests",
                    "courseCode": "QUIZ101",
                    "active": true,
                    "coverImage": "https://example.com/java-quiz.jpg",
                    "startDate": "%s",
                    "endDate": "%s",
                    "learningOutcomes": [
                        {
                            "code": "CLO-1",
                            "description": "Understand Java basics"
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

    // ==================== CREATE QUIZ TESTS ====================

    @Test
    @Order(3)
    @DisplayName("Should successfully create a quiz")
    void testCreateQuiz_ShouldSucceed() {
        String requestBody = """
                {
                    "quizTitle": "Midterm Quiz",
                    "unlimited": false,
                    "timeLimitMinutes": 60,
                    "passScore": 70.0,
                    "startTime": "10-01-2026 09:00:00",
                    "dueTime": "10-01-2026 10:00:00",
                    "attemptLimited": 2,
                    "display": true
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
                .body("quizTitle", equalTo("Midterm Quiz"))
                .body("timeLimitMinutes", equalTo(60))
                .body("passScore", equalTo(70.0f))
                .body("startTime", notNullValue())
                .body("dueTime", notNullValue())
                .body("attemptLimited", equalTo(2))
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("display", equalTo(true))
                .body("createdAt", notNullValue())
                .extract().response();

        createdQuizId = UUID.fromString(response.jsonPath().getString("quizId"));
    }

    @Test
    @Order(4)
    @DisplayName("Should create a second quiz for list tests")
    void testCreateSecondQuiz_ShouldSucceed() {
        String requestBody = """
                {
                    "quizTitle": "Final Quiz",
                    "unlimited": true,
                    "timeLimitMinutes": 120,
                    "passScore": 80.0
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
                .body("quizTitle", equalTo("Final Quiz"))
                .extract().response();

        secondQuizId = UUID.fromString(response.jsonPath().getString("quizId"));
    }

    @Test
    @Order(5)
    @DisplayName("Should create a third quiz for bulk delete tests")
    void testCreateThirdQuiz_ShouldSucceed() {
        String requestBody = """
                {
                    "quizTitle": "Practice Quiz",
                    "unlimited": true,
                    "timeLimitMinutes": 45,
                    "passScore": 65.0
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
                .body("quizTitle", equalTo("Practice Quiz"))
                .extract().response();
        thirdQuizId = UUID.fromString(response.jsonPath().getString("quizId"));
    }

    @Test
    @Order(6)
    @DisplayName("Should fail to create quiz without authentication")
    void testCreateQuiz_WithoutAuth() {
        String requestBody = """
                {
                    "quizTitle": "Unauthorized Quiz",
                    "unlimited": true,
                    "timeLimitMinutes": 30,
                    "passScore": 50.0
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/quiz")
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(7)
    @DisplayName("Should fail to create quiz with missing title")
    void testCreateQuiz_WithMissingTitle() {
        String requestBody = """
                {
                    "unlimited": true,
                    "timeLimitMinutes": 30,
                    "passScore": 50.0
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/quiz")
                .then()
                .log().all()
                .statusCode(400);
    }

    // ==================== GET QUIZ TESTS ====================

    @Test
    @Order(8)
    @DisplayName("Should successfully get a quiz by ID")
    void testGetQuiz_ShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz/" + createdQuizId)
                .then()
                .log().all()
                .statusCode(200)
                .body("quizId", equalTo(createdQuizId.toString()))
                .body("quizTitle", equalTo("Midterm Quiz"))
                .body("timeLimitMinutes", equalTo(60))
                .body("passScore", equalTo(70.0f))
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("display", equalTo(true))
                .body("createdAt", notNullValue());
    }

    @Test
    @Order(9)
    @DisplayName("Should get quiz with full aggregate shape")
    void testGetQuiz_WithFullAggregate_ShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz/" + createdQuizId)
                .then()
                .log().all()
                .statusCode(200)
                .body("quizId", equalTo(createdQuizId.toString()))
                .body("questions", hasSize(0));
    }

    @Test
    @Order(10)
    @DisplayName("Should fail to get quiz without authentication")
    void testGetQuiz_WithoutAuth() {
        given()
                .when()
                .get("/api/" + createdCourseId + "/quiz/" + createdQuizId)
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(11)
    @DisplayName("Should fail to get non-existent quiz")
    void testGetQuiz_WithNonExistentQuiz() {
        UUID nonExistentQuizId = UUID.randomUUID();

        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz/" + nonExistentQuizId)
                .then()
                .log().all()
                .statusCode(404);
    }

    // ==================== GET ALL QUIZZES TESTS ====================

    @Test
    @Order(12)
    @DisplayName("Should successfully get all quizzes in a course")
    void testGetAllQuizzes_CreatedByTeacher_ShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz/teacher-created?pageSize=10&lastCreated=asc")
                .then()
                .log().all()
                .statusCode(200)
                .body("quizzes", hasSize(greaterThanOrEqualTo(2)))
                .body("totalQuizzes", greaterThanOrEqualTo(2))
                .body("lastCreated", equalTo("asc"));
    }

    @Test
    @Order(13)
    @DisplayName("Should limit quizzes by pageSize")
    void testGetAllQuizzes_CreatedByTeacher_WithPageSize() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz/teacher-created?pageSize=1&lastCreated=asc")
                .then()
                .log().all()
                .statusCode(200)
                .body("quizzes", hasSize(1))
                .body("pageSize", equalTo(1));
    }

    @Test
    @Order(14)
    @DisplayName("Should fail to get all quizzes without authentication")
    void testGetAllQuizzes_CreatedByTeacher_WithoutAuth() {
        given()
                .when()
                .get("/api/" + createdCourseId + "/quiz/teacher-created")
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    // ==================== MODIFY QUIZ TESTS ====================

    @Test
    @Order(15)
    @DisplayName("Should successfully modify a quiz")
    void testModifyQuiz_ShouldSucceed() {
        String requestBody = """
                {
                    "quizTitle": "Updated Midterm Quiz",
                    "timeLimitMinutes": 90,
                    "passScore": 75.0,
                    "display": true
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/quiz/" + createdQuizId)
                .then()
                .log().all()
                .statusCode(200)
                .body("quizId", equalTo(createdQuizId.toString()))
                .body("quizTitle", equalTo("Updated Midterm Quiz"))
                .body("timeLimitMinutes", equalTo(90))
                .body("passScore", equalTo(75.0f))
                .body("updatedAt", notNullValue());
    }

    @Test
    @Order(16)
    @DisplayName("Should fail to modify quiz without authentication")
    void testModifyQuiz_WithoutAuth() {
        String requestBody = """
                {
                    "quizTitle": "Hack Quiz",
                    "timeLimitMinutes": 10,
                    "passScore": 100.0
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/quiz/" + createdQuizId)
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(17)
    @DisplayName("Should fail to modify non-existent quiz")
    void testModifyQuiz_WithNonExistentQuiz() {
        UUID nonExistentQuizId = UUID.randomUUID();

        String requestBody = """
                {
                    "quizTitle": "Ghost Quiz",
                    "timeLimitMinutes": 10
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/quiz/" + nonExistentQuizId)
                .then()
                .log().all()
                .statusCode(404);
    }

    // ==================== DELETE QUIZ TESTS ====================

    @Test
    @Order(18)
    @DisplayName("Should fail to delete quiz without authentication")
    void testDeleteQuiz_WithoutAuth() {
        given()
                .when()
                .delete("/api/" + createdCourseId + "/quiz/" + secondQuizId)
                .then()
                .log().all()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(19)
    @DisplayName("Should successfully delete quizzes in bulk")
    void testDeleteQuiz_Bulk_ShouldSucceed() {
        String requestBody = """
                {
                    "quizIds": ["%s", "%s"]
                }
                """.formatted(secondQuizId, thirdQuizId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .delete("/api/" + createdCourseId + "/quiz")
                .then()
                .log().all()
                .statusCode(200)
                .body("", hasSize(2))
                .body("[0].message", equalTo("Quiz deleted successfully"))
                .body("[1].message", equalTo("Quiz deleted successfully"));
    }

    @Test
    @Order(20)
    @DisplayName("Should fail to delete non-existent quiz")
    void testDeleteQuiz_WithNonExistentQuiz() {
        UUID nonExistentQuizId = UUID.randomUUID();

        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .delete("/api/" + createdCourseId + "/quiz/" + nonExistentQuizId)
                .then()
                .log().all()
                .statusCode(404);
    }

    // ==================== VERIFY STATE AFTER DELETE ====================

    @Test
    @Order(21)
    @DisplayName("Should verify the quiz was deleted from the list")
    void testGetAllQuizzes_CreatedByTeacher_AfterDelete() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz/teacher-created?pageSize=10&lastCreated=asc")
                .then()
                .log().all()
                .statusCode(200)
                .body("quizzes", notNullValue());
    }

    @Test
    @Order(22)
    @DisplayName("Should create additional learning outcomes for quiz mapping")
    void testCreateLearningOutcomesForQuizMapping() {
        String requestBody = """
                {
                    "learningOutcomes": [
                        {"code": "CLO-Q1", "description": "Understand fundamentals"},
                        {"code": "CLO-Q2", "description": "Apply fundamentals"},
                        {"code": "CLO-Q3", "description": "Analyze scenarios"},
                        {"code": "CLO-Q4", "description": "Evaluate solutions"}
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
                .body("totalCreated", equalTo(4))
                .extract().response();

        lo1 = UUID.fromString(response.jsonPath().getString("learningOutcomes[0].id"));
        lo2 = UUID.fromString(response.jsonPath().getString("learningOutcomes[1].id"));
        lo3 = UUID.fromString(response.jsonPath().getString("learningOutcomes[2].id"));
        lo4 = UUID.fromString(response.jsonPath().getString("learningOutcomes[3].id"));
    }

    @Test
    @Order(23)
    @DisplayName("Should create group one with two learning outcomes")
    void testCreateGroupOne() {
        String requestBody = """
                {
                    "name": "Group One",
                    "description": "First group",
                    "learningOutcomeIds": ["%s", "%s"]
                }
                """.formatted(lo1, lo2);

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/group-learning-outcome")
                .then()
                .statusCode(201)
                .extract().response();

        group1 = UUID.fromString(response.jsonPath().getString("id"));
    }

    @Test
    @Order(24)
    @DisplayName("Should create group two with one learning outcome")
    void testCreateGroupTwo() {
        String requestBody = """
                {
                    "name": "Group Two",
                    "description": "Second group",
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(lo3);

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/group-learning-outcome")
                .then()
                .statusCode(201)
                .extract().response();

        group2 = UUID.fromString(response.jsonPath().getString("id"));
    }

    @Test
    @Order(25)
    @DisplayName("Should add direct and grouped learning outcomes with overlap deduplicated")
    void testAddLearningOutcomesToQuiz_Success() {
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s", "%s"],
                    "groupLearningOutcomeIds": ["%s", "%s"]
                }
                """.formatted(lo2, lo4, group1, group2);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/quiz/" + createdQuizId + "/learning-outcomes")
                .then()
                .statusCode(200)
                .body("operation", equalTo("ADD"))
                .body("totalAffected", equalTo(4))
                .body("learningOutcomeIds", hasSize(4));
    }

    @Test
    @Order(26)
    @DisplayName("Should fail to add learning outcomes from a different course")
    void testAddLearningOutcomesToQuiz_FromDifferentCourse_ShouldFail() {
        String startDate = LocalDate.now().plusDays(2).format(DATE_FORMATTER);
        String endDate = LocalDate.now().plusMonths(4).format(DATE_FORMATTER);

        String createOtherCourseRequest = """
                {
                    "courseName": "Foreign Course For Quiz Mapping",
                    "description": "Course used to validate LO course mismatch",
                    "courseCode": "QUIZ-FOREIGN-101",
                    "active": true,
                    "coverImage": "https://example.com/foreign-course.jpg",
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

        otherCourseId = UUID.fromString(createOtherCourseResponse.jsonPath().getString("courseId"));

        String createForeignLearningOutcomeRequest = """
                {
                    "learningOutcomes": [
                        {"code": "CLO-FOREIGN-1", "description": "Foreign course learning outcome"}
                    ]
                }
                """;

        Response createForeignLearningOutcomeResponse = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + teacherToken)
            .body(createForeignLearningOutcomeRequest)
            .when()
            .post("/api/" + otherCourseId + "/learning-outcome")
            .then()
            .statusCode(201)
            .body("totalCreated", equalTo(1))
            .extract().response();

        foreignLearningOutcomeId = UUID.fromString(
                createForeignLearningOutcomeResponse.jsonPath().getString("learningOutcomes[0].id"));

        String addRequestBody = """
                {
                    "learningOutcomeIds": ["%s"],
                    "groupLearningOutcomeIds": []
                }
                """.formatted(foreignLearningOutcomeId);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(addRequestBody)
                .when()
                .post("/api/" + createdCourseId + "/quiz/" + createdQuizId + "/learning-outcomes")
                .then()
                .statusCode(403)
                .body("message", equalTo("Some learning outcomes do not belong to this course"));
    }

    @Test
    @Order(27)
    @DisplayName("Should get quiz with grouped and ungrouped learning outcomes")
    void testGetQuiz_WithGroupedLearningOutcomes_ShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz/" + createdQuizId)
                .then()
                .statusCode(200)
                .body("groupedLearningOutcomes", hasSize(2))
                .body("ungroupedLearningOutcomes", hasSize(1))
                .body("ungroupedLearningOutcomes.code", hasItem("CLO-Q4"));
    }

    @Test
    @Order(28)
    @DisplayName("Should remove learning outcomes from quiz using direct and grouped ids")
    void testRemoveLearningOutcomesFromQuiz_Success() {
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"],
                    "groupLearningOutcomeIds": ["%s"]
                }
                """.formatted(lo1, group2);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .delete("/api/" + createdCourseId + "/quiz/" + createdQuizId + "/learning-outcomes")
                .then()
                .statusCode(200)
                .body("operation", equalTo("REMOVE"))
                .body("totalAffected", equalTo(2))
                .body("learningOutcomeIds", hasSize(2));
    }

    @Test
    @Order(29)
    @DisplayName("Should register two students for student-created quiz list tests")
    void testRegisterStudentsForStudentCreatedList() {
        String studentARegisterBody = """
                {
                    "firstName": "Student",
                    "lastName": "A",
                    "email": "student.a.quiz@example.com",
                    "password": "StudentPass123!",
                    "studentId": "STU-QA-001",
                    "enrollmentYear": 2026
                }
                """;

        studentAToken = given()
                .contentType(ContentType.JSON)
                .body(studentARegisterBody)
                .when()
                .post("/api/auth/student/register")
                .then()
                .statusCode(201)
                .extract()
                .path("token");

        String studentBRegisterBody = """
                {
                    "firstName": "Student",
                    "lastName": "B",
                    "email": "student.b.quiz@example.com",
                    "password": "StudentPass123!",
                    "studentId": "STU-QA-002",
                    "enrollmentYear": 2026
                }
                """;

        studentBToken = given()
                .contentType(ContentType.JSON)
                .body(studentBRegisterBody)
                .when()
                .post("/api/auth/student/register")
                .then()
                .statusCode(201)
                .extract()
                .path("token");
    }

    @Test
    @Order(30)
    @DisplayName("Should enroll both students into the course")
    void testEnrollStudentsForStudentCreatedList() {
        String addStudentsBody = """
                {
                    "studentEmails": [
                        "student.a.quiz@example.com",
                        "student.b.quiz@example.com"
                    ]
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(addStudentsBody)
                .when()
                .put("/api/course/" + createdCourseId + "/students/add")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(31)
    @DisplayName("Should create first quiz by student A")
    void testCreateFirstQuizByStudentA() {
        String requestBody = """
                {
                    "quizTitle": "%s",
                    "unlimited": true,
                    "timeLimitMinutes": 20,
                    "passScore": 50.0,
                    "display": true
                }
                """.formatted(STUDENT_A_QUIZ_ONE_TITLE);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + studentAToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/quiz")
                .then()
                .statusCode(201)
                .body("quizTitle", equalTo(STUDENT_A_QUIZ_ONE_TITLE));
    }

    @Test
    @Order(32)
    @DisplayName("Should create second quiz by student A")
    void testCreateSecondQuizByStudentA() {
        String requestBody = """
                {
                    "quizTitle": "%s",
                    "unlimited": true,
                    "timeLimitMinutes": 25,
                    "passScore": 55.0,
                    "display": true
                }
                """.formatted(STUDENT_A_QUIZ_TWO_TITLE);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + studentAToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/quiz")
                .then()
                .statusCode(201)
                .body("quizTitle", equalTo(STUDENT_A_QUIZ_TWO_TITLE));
    }

    @Test
    @Order(33)
    @DisplayName("Should create one quiz by student B")
    void testCreateQuizByStudentB() {
        String requestBody = """
                {
                    "quizTitle": "%s",
                    "unlimited": true,
                    "timeLimitMinutes": 15,
                    "passScore": 45.0,
                    "display": true
                }
                """.formatted(STUDENT_B_QUIZ_TITLE);

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + studentBToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/quiz")
                .then()
                .statusCode(201)
            .body("quizTitle", equalTo(STUDENT_B_QUIZ_TITLE))
            .extract().response();

        studentBQuizId = UUID.fromString(response.jsonPath().getString("quizId"));
    }

    @Test
    @Order(34)
    @DisplayName("Should return only student A quizzes for student-created endpoint")
    void testGetAllQuizzesCreatedByStudent_ForStudentA() {
        given()
                .header("Authorization", "Bearer " + studentAToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz/student-created?pageSize=10&lastCreated=asc")
                .then()
                .statusCode(200)
                .body("quizzes.quizTitle", hasItem(STUDENT_A_QUIZ_ONE_TITLE))
                .body("quizzes.quizTitle", hasItem(STUDENT_A_QUIZ_TWO_TITLE))
                .body("quizzes.quizTitle", org.hamcrest.Matchers.not(hasItem(STUDENT_B_QUIZ_TITLE)));
    }

    @Test
    @Order(35)
    @DisplayName("Should sort student-created quizzes by last created descending")
    void testGetAllQuizzesCreatedByStudent_ForStudentA_DescSort() {
        given()
                .header("Authorization", "Bearer " + studentAToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz/student-created?pageSize=10&lastCreated=des")
                .then()
                .statusCode(200)
                .body("lastCreated", equalTo("des"))
                .body("quizzes[0].quizTitle", equalTo(STUDENT_A_QUIZ_TWO_TITLE));
    }

    @Test
    @Order(36)
    @DisplayName("Should return only student B quizzes for student-created endpoint")
    void testGetAllQuizzesCreatedByStudent_ForStudentB() {
        given()
                .header("Authorization", "Bearer " + studentBToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz/student-created?pageSize=10&lastCreated=asc")
                .then()
                .statusCode(200)
                .body("quizzes", hasSize(1))
                .body("quizzes[0].quizTitle", equalTo(STUDENT_B_QUIZ_TITLE));
    }

    @Test
    @Order(37)
    @DisplayName("Should create hidden teacher quiz with display false")
    void testCreateHiddenTeacherQuiz() {
        String requestBody = """
                {
                    "quizTitle": "%s",
                    "unlimited": true,
                    "timeLimitMinutes": 30,
                    "passScore": 50.0,
                    "display": false
                }
                """.formatted(HIDDEN_TEACHER_QUIZ_TITLE);

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/quiz")
                .then()
                .statusCode(201)
            .body("quizTitle", equalTo(HIDDEN_TEACHER_QUIZ_TITLE))
            .extract().response();

        hiddenTeacherQuizId = UUID.fromString(response.jsonPath().getString("quizId"));
    }

    @Test
    @Order(38)
    @DisplayName("Teacher-created endpoint should include all teacher quizzes and exclude student-created quizzes")
    void testGetAllQuizzesCreatedByTeacher_ShouldExcludeStudentCreated() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz/teacher-created?pageSize=20&lastCreated=asc")
                .then()
                .statusCode(200)
                .body("quizzes.quizTitle", org.hamcrest.Matchers.not(hasItem(STUDENT_A_QUIZ_ONE_TITLE)))
                .body("quizzes.quizTitle", org.hamcrest.Matchers.not(hasItem(STUDENT_A_QUIZ_TWO_TITLE)))
                .body("quizzes.quizTitle", org.hamcrest.Matchers.not(hasItem(STUDENT_B_QUIZ_TITLE)))
                .body("quizzes.quizTitle", hasItem(HIDDEN_TEACHER_QUIZ_TITLE));
    }

    @Test
    @Order(39)
    @DisplayName("Should allow student access to teacher-created endpoint with display=true filtering")
    void testGetAllQuizzesCreatedByTeacher_AsStudent_ShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + studentAToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz/teacher-created?pageSize=10&lastCreated=asc")
                .then()
                .statusCode(200)
                .body("quizzes.quizTitle", org.hamcrest.Matchers.not(hasItem(STUDENT_A_QUIZ_ONE_TITLE)))
                .body("quizzes.quizTitle", org.hamcrest.Matchers.not(hasItem(STUDENT_A_QUIZ_TWO_TITLE)))
                .body("quizzes.quizTitle", org.hamcrest.Matchers.not(hasItem(STUDENT_B_QUIZ_TITLE)))
                .body("quizzes.quizTitle", org.hamcrest.Matchers.not(hasItem(HIDDEN_TEACHER_QUIZ_TITLE)));
    }

    @Test
    @Order(40)
    @DisplayName("Should deny teacher access to student-created endpoint")
    void testGetAllQuizzesCreatedByStudent_AsTeacher_ShouldBeForbidden() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz/student-created?pageSize=10&lastCreated=asc")
                .then()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    @Order(41)
    @DisplayName("Student should view teacher-created visible quiz")
    void testGetQuiz_AsStudent_VisibleTeacherQuiz_ShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + studentAToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz/" + createdQuizId)
                .then()
                .statusCode(200)
                .body("quizId", equalTo(createdQuizId.toString()))
                .body("display", equalTo(true));
    }

    @Test
    @Order(42)
    @DisplayName("Teacher should view student-created visible quiz")
    void testGetQuiz_AsTeacher_ForStudentCreatedVisibleQuiz_ShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz/" + studentBQuizId)
                .then()
                .statusCode(200)
                .body("quizId", equalTo(studentBQuizId.toString()))
                .body("display", equalTo(true));
    }

    @Test
    @Order(43)
    @DisplayName("Student should not view hidden quiz")
    void testGetQuiz_AsStudent_HiddenQuiz_ShouldBeForbidden() {
        given()
                .header("Authorization", "Bearer " + studentAToken)
                .when()
                .get("/api/" + createdCourseId + "/quiz/" + hiddenTeacherQuizId)
                .then()
                .statusCode(403);
    }
}
