package edu.ai.lms.core.quiz.adapter.in.http;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import edu.ai.lms.config.IntegrationTestConfiguration;
import io.restassured.http.ContentType;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QuestionBankControllerIT extends IntegrationTestConfiguration {
    @Autowired
    private JdbcTemplate jdbc;

    private static String ownerToken;
    private static String otherToken;
    private static UUID courseId;
    private static UUID otherCourseId;
    private static UUID chapterId;
    private static UUID otherChapterId;
    private static UUID learningOutcomeId;
    private static UUID draftId;
    private static UUID bankQuestionId;

    @Test
    @Order(1)
    void setupCourseReferences() {
        ownerToken = registerTeacher("bank.owner@example.com", "EMP-BANK-OWNER");
        otherToken = registerTeacher("bank.other@example.com", "EMP-BANK-OTHER");
        courseId = createCourse(ownerToken, "BANK-OWNER");
        otherCourseId = createCourse(otherToken, "BANK-OTHER");
        chapterId = insertChapter(courseId, "Algorithms");
        otherChapterId = insertChapter(otherCourseId, "Foreign");
        learningOutcomeId = insertLearningOutcome(courseId, "Explain algorithms");
    }

    @Test
    @Order(2)
    void createsListsAndGetsDraft() {
        var response = owner().body(itemBody(chapterId, learningOutcomeId, "What is stable sorting?"))
                .post(basePath(), courseId)
                .then().statusCode(201)
                .body("version", equalTo(0))
                .body("answers", hasSize(2))
                .extract();
        draftId = UUID.fromString(response.path("id"));

        owner().queryParam("query", "stable")
                .get(basePath(), courseId)
                .then().statusCode(200)
                .body("total", equalTo(1))
                .body("items[0].id", equalTo(draftId.toString()));
        owner().get(basePath() + "/{draftId}", courseId, draftId)
                .then().statusCode(200)
                .body("learningOutcomeIds[0]", equalTo(learningOutcomeId.toString()));
    }

    @Test
    @Order(3)
    void rejectsCrossCourseReferenceAndStaleUpdate() {
        owner().body(itemBody(otherChapterId, learningOutcomeId, "Foreign chapter"))
                .post(basePath(), courseId)
                .then().statusCode(422);

        owner().queryParam("version", 7)
                .body(itemBody(chapterId, learningOutcomeId, "Changed"))
                .put(basePath() + "/{draftId}", courseId, draftId)
                .then().statusCode(409);
    }

    @Test
    @Order(4)
    void updatesAndMovesCompleteDraftToValidatedBank() {
        owner().queryParam("version", 0)
                .body(itemBody(chapterId, learningOutcomeId, "What makes sorting stable?"))
                .put(basePath() + "/{draftId}", courseId, draftId)
                .then().statusCode(200)
                .body("version", equalTo(1));

        var validated = owner().body("{\"version\":1}")
                .post(basePath() + "/{draftId}/validate", courseId, draftId)
                .then().statusCode(200)
                .body("originDraftId", equalTo(draftId.toString()))
                .body("validatedBy", notNullValue())
                .extract();
        bankQuestionId = UUID.fromString(validated.path("id"));

        owner().get(basePath() + "/{draftId}", courseId, draftId).then().statusCode(404);
        owner().get(bankPath() + "/{questionId}", courseId, bankQuestionId)
                .then().statusCode(200)
                .body("content", equalTo("What makes sorting stable?"));
    }

    @Test
    @Order(5)
    void duplicateDraftCanBeValidatedAsSecondBankQuestion() {
        UUID duplicateId = UUID.fromString(owner()
                .body(itemBody(chapterId, learningOutcomeId, "What makes sorting stable?"))
                .post(basePath(), courseId)
                .then().statusCode(201)
                .extract().path("id"));
        owner().body("{\"version\":0}")
                .post(basePath() + "/{draftId}/validate", courseId, duplicateId)
                .then().statusCode(200)
                .body("originDraftId", equalTo(duplicateId.toString()));
    }

    @Test
    @Order(6)
    void enforcesCourseOwnership() {
        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + otherToken)
                .get(basePath(), courseId)
                .then().statusCode(403);

        owner().get(bankPath(), courseId).then().statusCode(200)
                .body("items[0].id", equalTo(bankQuestionId.toString()));
    }

    private io.restassured.specification.RequestSpecification owner() {
        return given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + ownerToken);
    }

    private String registerTeacher(String email, String employeeId) {
        return given().contentType(ContentType.JSON).body("""
                {"firstName":"Bank","lastName":"Teacher","email":"%s",
                 "password":"TeacherPass123!","employeeId":"%s"}
                """.formatted(email, employeeId))
                .post("/api/auth/teacher/register")
                .then().statusCode(201)
                .extract().path("token");
    }

    private UUID createCourse(String token, String code) {
        String start = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String end = LocalDate.now().plusMonths(2).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        return UUID.fromString(given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("""
                        {"courseName":"%s","courseCode":"%s","active":true,
                         "startDate":"%s","endDate":"%s","learningOutcomes":[]}
                        """.formatted(code, code, start, end))
                .post("/api/course")
                .then().statusCode(201)
                .extract().path("courseId"));
    }

    private UUID insertChapter(UUID targetCourseId, String title) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO edu.chapters (chapter_id,title,course_id) VALUES (?,?,?)",
                id, title, targetCourseId);
        return id;
    }

    private UUID insertLearningOutcome(UUID targetCourseId, String description) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO edu.learning_outcomes (id,prefix_number,description,course_id) VALUES (?,1,?,?)",
                id, description, targetCourseId);
        return id;
    }

    private String itemBody(UUID targetChapterId, UUID targetOutcomeId, String content) {
        return """
                {"chapterId":"%s","content":"%s","questionType":"MULTIPLE_CHOICE",
                 "cognitiveLevelCode":"UNDERSTAND",
                 "answers":[
                   {"content":"Yes","correct":true,"explanation":"Correct"},
                   {"content":"No","correct":false}
                 ],"learningOutcomeIds":["%s"]}
                """.formatted(targetChapterId, content, targetOutcomeId);
    }

    private String basePath() {
        return "/api/courses/{courseId}/question-drafts";
    }

    private String bankPath() {
        return "/api/courses/{courseId}/question-bank/questions";
    }
}
