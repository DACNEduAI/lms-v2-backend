package edu.ai.lms.core.course.adapter.in;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.containsString;
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
@DisplayName("Group Learning Outcome Controller Integration Tests")
class GroupLearningOutcomeControllerIT extends IntegrationTestConfiguration {

    private static String teacherToken;
    private static String anotherTeacherToken;
    private static UUID createdCourseId;
    private static UUID createdLoId1;
    private static UUID createdLoId2;
    private static UUID createdLoId3;
    private static UUID createdLoId4;
    private static UUID createdGroupId;
    private static UUID createdGroupId2;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ==================== SETUP ====================

    @Test
    @Order(1)
    @DisplayName("Should register a teacher for group LO tests")
    void testRegisterTeacher() {
        String requestBody = """
                {
                    "firstName": "GroupLO",
                    "lastName": "Teacher",
                    "email": "grouplo.teacher@example.com",
                    "password": "TeacherPass123!",
                    "employeeId": "EMP-GLO001",
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
    @DisplayName("Should register another teacher for authorization tests")
    void testRegisterAnotherTeacher() {
        String requestBody = """
                {
                    "firstName": "Another",
                    "lastName": "Teacher",
                    "email": "another.grouplo.teacher@example.com",
                    "password": "TeacherPass123!",
                    "employeeId": "EMP-GLO002",
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

        anotherTeacherToken = response.jsonPath().getString("token");
    }

    @Test
    @Order(3)
    @DisplayName("Should create a course for group LO tests")
    void testCreateCourse() {
        String startDate = LocalDate.now().plusDays(1).format(DATE_FORMATTER);
        String endDate = LocalDate.now().plusMonths(3).format(DATE_FORMATTER);

        String requestBody = """
                {
                    "courseName": "Java Programming for Group LO Tests",
                    "description": "Course used for group learning outcome integration tests",
                    "courseCode": "GLO101",
                    "active": true,
                    "coverImage": "https://example.com/java-glo.jpg",
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
    @DisplayName("Should create learning outcomes for group LO tests")
    void testCreateLearningOutcomes() {
        String requestBody = """
                {
                    "learningOutcomes": [
                        {
                            "code": "CLO-1",
                            "description": "Understand basic Java syntax"
                        },
                        {
                            "code": "CLO-2",
                            "description": "Write simple Java programs"
                        },
                        {
                            "code": "CLO-3",
                            "description": "Apply OOP concepts"
                        },
                        {
                            "code": "CLO-4",
                            "description": "Design software architecture"
                        }
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
                .log().all()
                .statusCode(201)
                .body("totalCreated", equalTo(4))
                .extract().response();

        createdLoId1 = UUID.fromString(response.jsonPath().getString("learningOutcomes[0].id"));
        createdLoId2 = UUID.fromString(response.jsonPath().getString("learningOutcomes[1].id"));
        createdLoId3 = UUID.fromString(response.jsonPath().getString("learningOutcomes[2].id"));
        createdLoId4 = UUID.fromString(response.jsonPath().getString("learningOutcomes[3].id"));
    }

    // ==================== CREATE GROUP LEARNING OUTCOME TESTS ====================

    @Test
    @Order(5)
    @DisplayName("Should successfully create a group learning outcome with learning outcomes")
    void testCreateGroupLearningOutcome_Success() {
        String requestBody = """
                {
                    "name": "Fundamentals",
                    "description": "Basic programming fundamentals",
                    "learningOutcomeIds": ["%s", "%s"]
                }
                """.formatted(createdLoId1, createdLoId2);

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/group-learning-outcome")
                .then()
                .log().all()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Fundamentals"))
                .body("description", equalTo("Basic programming fundamentals"))
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("learningOutcomes", hasSize(2))
                .body("learningOutcomes[0].code", equalTo("CLO-1"))
                .body("learningOutcomes[1].code", equalTo("CLO-2"))
                .body("createdAt", notNullValue())
                .extract().response();

        createdGroupId = UUID.fromString(response.jsonPath().getString("id"));
    }

    @Test
    @Order(6)
    @DisplayName("Should fail to create group when LOs are already in another group")
    void testCreateGroupLearningOutcome_LOsAlreadyInGroup() {
        String requestBody = """
                {
                    "name": "Duplicate Group",
                    "description": "Trying to use already grouped LOs",
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId1);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/group-learning-outcome")
                .then()
                .log().all()
                .statusCode(400)
                .body("message", containsString("already assigned to a group"));
    }

    @Test
    @Order(7)
    @DisplayName("Should fail to create group without authentication")
    void testCreateGroupLearningOutcome_Unauthorized() {
        String requestBody = """
                {
                    "name": "Unauthorized Group",
                    "description": "No auth",
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId3);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/group-learning-outcome")
                .then()
                .log().all()
                .statusCode(403);
    }

    @Test
    @Order(8)
    @DisplayName("Should fail to create group when teacher is not authorized for the course")
    void testCreateGroupLearningOutcome_TeacherNotAuthorized() {
        String requestBody = """
                {
                    "name": "Unauthorized Group",
                    "description": "Wrong teacher",
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId3);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + anotherTeacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/group-learning-outcome")
                .then()
                .log().all()
                .statusCode(403);
    }

    @Test
    @Order(9)
    @DisplayName("Should fail to create group with non-existent course")
    void testCreateGroupLearningOutcome_CourseNotFound() {
        UUID nonExistentCourseId = UUID.randomUUID();
        String requestBody = """
                {
                    "name": "Ghost Group",
                    "description": "Non-existent course",
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(UUID.randomUUID());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + nonExistentCourseId + "/group-learning-outcome")
                .then()
                .log().all()
                .statusCode(404);
    }

    @Test
    @Order(10)
    @DisplayName("Should successfully create a second group with one learning outcome")
    void testCreateGroupLearningOutcome_SecondGroup() {
        String requestBody = """
                {
                    "name": "Advanced Topics",
                    "description": "A group for advanced LOs",
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId4);

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .post("/api/" + createdCourseId + "/group-learning-outcome")
                .then()
                .log().all()
                .statusCode(201)
                .body("name", equalTo("Advanced Topics"))
                .body("learningOutcomes", hasSize(1))
                .extract().response();

        createdGroupId2 = UUID.fromString(response.jsonPath().getString("id"));
    }

    // ==================== GET COURSE WITH GROUPED LOS TESTS ====================

    @Test
    @Order(11)
    @DisplayName("Should get course with grouped and ungrouped learning outcomes")
    void testGetCourseWithGroupedLOs() {
        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/course/" + createdCourseId)
                .then()
                .log().all()
                .statusCode(200)
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("groupLearningOutcomes", notNullValue())
                .body("groupLearningOutcomes", hasSize(2))
                .body("ungroupedLearningOutcomes", notNullValue())
                .body("ungroupedLearningOutcomes", hasSize(1));
    }

    // ==================== ADD LEARNING OUTCOMES TO GROUP TESTS ====================

    @Test
    @Order(12)
    @DisplayName("Should successfully add learning outcomes to a group")
    void testAddLearningOutcomesToGroup_Success() {
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId3);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/group-learning-outcome/" + createdGroupId + "/add-learning-outcomes")
                .then()
                .log().all()
                .statusCode(200)
                .body("groupId", equalTo(createdGroupId.toString()))
                .body("name", equalTo("Fundamentals"))
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("totalAdded", equalTo(1))
                .body("learningOutcomes", hasSize(3));
    }

    @Test
    @Order(13)
    @DisplayName("Should fail to add LO that is in another group")
    void testAddLearningOutcomesToGroup_LOAlreadyInAnotherGroup() {
        // createdLoId3 was added to createdGroupId (Fundamentals) in Order 12
        // Try to add it to createdGroupId2 (Advanced Topics)
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId3);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/group-learning-outcome/" + createdGroupId2 + "/add-learning-outcomes")
                .then()
                .log().all()
                .statusCode(400)
                .body("message", containsString("already assigned to another group"));
    }

    @Test
    @Order(14)
    @DisplayName("Should fail to add LO without authentication")
    void testAddLearningOutcomesToGroup_Unauthorized() {
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId4);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/group-learning-outcome/" + createdGroupId + "/add-learning-outcomes")
                .then()
                .log().all()
                .statusCode(403);
    }

    @Test
    @Order(15)
    @DisplayName("Should fail to add LO to non-existent group")
    void testAddLearningOutcomesToGroup_GroupNotFound() {
        UUID nonExistentGroupId = UUID.randomUUID();
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId4);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/group-learning-outcome/" + nonExistentGroupId + "/add-learning-outcomes")
                .then()
                .log().all()
                .statusCode(404);
    }

    // ==================== REMOVE LEARNING OUTCOMES FROM GROUP TESTS ====================

    @Test
    @Order(16)
    @DisplayName("Should successfully remove learning outcomes from a group")
    void testRemoveLearningOutcomesFromGroup_Success() {
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId3);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/group-learning-outcome/" + createdGroupId + "/remove-learning-outcomes")
                .then()
                .log().all()
                .statusCode(200)
                .body("groupId", equalTo(createdGroupId.toString()))
                .body("name", equalTo("Fundamentals"))
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("totalRemoved", equalTo(1))
                .body("message", containsString("Successfully removed 1 learning outcomes"));
    }

    @Test
    @Order(17)
    @DisplayName("Should fail to remove LO not in the specified group")
    void testRemoveLearningOutcomesFromGroup_LONotInGroup() {
        // createdLoId4 is in createdGroupId2, not in createdGroupId
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId4);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/group-learning-outcome/" + createdGroupId + "/remove-learning-outcomes")
                .then()
                .log().all()
                .statusCode(403);
    }

    @Test
    @Order(18)
    @DisplayName("Should fail to remove LO without authentication")
    void testRemoveLearningOutcomesFromGroup_Unauthorized() {
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId1);

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/group-learning-outcome/" + createdGroupId + "/remove-learning-outcomes")
                .then()
                .log().all()
                .statusCode(403);
    }

    @Test
    @Order(19)
    @DisplayName("Should fail to remove LO from non-existent group")
    void testRemoveLearningOutcomesFromGroup_GroupNotFound() {
        UUID nonExistentGroupId = UUID.randomUUID();
        String requestBody = """
                {
                    "learningOutcomeIds": ["%s"]
                }
                """.formatted(createdLoId1);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + teacherToken)
                .body(requestBody)
                .when()
                .put("/api/" + createdCourseId + "/group-learning-outcome/" + nonExistentGroupId + "/remove-learning-outcomes")
                .then()
                .log().all()
                .statusCode(404);
    }

    // ==================== VERIFY FINAL STATE ====================

    @Test
    @Order(20)
    @DisplayName("Should verify final course state with correct grouped and ungrouped LOs")
    void testVerifyFinalCourseState() {
        // After all operations:
        // createdGroupId (Fundamentals) has [loId1, loId2] (loId3 was added in Order 12, removed in Order 16)
        // createdGroupId2 (Advanced Topics) has [loId4]
        // loId3 is ungrouped

        given()
                .header("Authorization", "Bearer " + teacherToken)
                .when()
                .get("/api/course/" + createdCourseId)
                .then()
                .log().all()
                .statusCode(200)
                .body("courseId", equalTo(createdCourseId.toString()))
                .body("groupLearningOutcomes", notNullValue())
                .body("groupLearningOutcomes", hasSize(2))
                .body("ungroupedLearningOutcomes", notNullValue())
                .body("ungroupedLearningOutcomes", hasSize(1));
    }
}
