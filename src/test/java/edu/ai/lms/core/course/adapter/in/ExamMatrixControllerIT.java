package edu.ai.lms.core.course.adapter.in;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import edu.ai.lms.config.IntegrationTestConfiguration;
import io.restassured.http.ContentType;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExamMatrixControllerIT extends IntegrationTestConfiguration {
    @Autowired JdbcTemplate jdbc;

    static String token;
    static UUID courseId;
    static UUID chapterId = UUID.randomUUID();
    static UUID outcomeId;
    static UUID secondOutcomeId;
    static UUID understandLevelId;
    static UUID applyLevelId;
    static UUID matrixId;
    static UUID firstRowId;
    static UUID secondRowId;

    @Test
    @Order(1)
    void setupTeacherCourseAndReferenceData() {
        token = given().contentType(ContentType.JSON).body("""
                {"firstName":"Matrix","lastName":"Teacher","email":"matrix.api@example.com",
                 "password":"TeacherPass123!","employeeId":"EMP-MATRIX-API",
                 "department":"CS","specialization":"Testing"}
                """).post("/api/auth/teacher/register").then().statusCode(201)
                .extract().path("token");

        String start = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String end = LocalDate.now().plusMonths(2).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        courseId = UUID.fromString(given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token).body("""
                {"courseName":"Matrix API","courseCode":"MATRIX-API","active":true,
                 "startDate":"%s","endDate":"%s",
                 "learningOutcomes":[
                   {"prefixNumber":1,"description":"Explain matrix concepts"},
                   {"prefixNumber":2,"description":"Apply matrix concepts"}]}
                """.formatted(start, end)).post("/api/course").then().statusCode(201)
                .extract().path("courseId"));

        List<String> outcomes = given().header("Authorization", "Bearer " + token)
                .get("/api/course/{courseId}", courseId).then().statusCode(200)
                .extract().path("ungroupedLearningOutcomes.id");
        outcomeId = UUID.fromString(outcomes.get(0));
        secondOutcomeId = UUID.fromString(outcomes.get(1));
        jdbc.update("INSERT INTO edu.chapters (chapter_id,title,course_id) VALUES (?,?,?)",
                chapterId, "Chapter A", courseId);
        jdbc.update("INSERT INTO edu.chapter_learning_outcomes (chapter_id,learning_outcome_id) VALUES (?,?)",
                chapterId, outcomeId);
        jdbc.update("INSERT INTO edu.chapter_learning_outcomes (chapter_id,learning_outcome_id) VALUES (?,?)",
                chapterId, secondOutcomeId);
    }

    @Test
    @Order(2)
    void managesCustomLearningLevels() {
        understandLevelId = UUID.fromString(given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token).body("""
                {"learningOutcomeIds":["%s","%s"],"code":"CLL1","name":"Understand",
                 "description":"Recall and explain","sortOrder":0}
                """.formatted(outcomeId, secondOutcomeId))
                .post("/api/courses/{courseId}/custom-learning-levels", courseId)
                .then().statusCode(201).body("code", equalTo("CLL1"))
                .body("learningOutcomeIds", containsInAnyOrder(
                        outcomeId.toString(), secondOutcomeId.toString()))
                .extract().path("id"));
        applyLevelId = UUID.fromString(given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token).body("""
                {"learningOutcomeIds":["%s"],"code":"CLL2","name":"Apply","sortOrder":1}
                """.formatted(outcomeId))
                .post("/api/courses/{courseId}/custom-learning-levels", courseId)
                .then().statusCode(201).extract().path("id"));
        UUID temporaryId = UUID.fromString(given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token).body("""
                {"learningOutcomeIds":["%s"],"code":"TEMP","name":"Temporary","sortOrder":2}
                """.formatted(outcomeId))
                .post("/api/courses/{courseId}/custom-learning-levels", courseId)
                .then().statusCode(201).extract().path("id"));

        given().contentType(ContentType.JSON).header("Authorization", "Bearer " + token).body("""
                {"create":[{"clientTempId":"new-level","learningOutcomeIds":["%s"],
                  "code":"CLL3","name":"Analyze","sortOrder":2}],
                 "update":[{"id":"%s","learningOutcomeIds":["%s"],"code":"CLL2","name":"Apply",
                  "description":"Apply a known method","sortOrder":1}],
                 "delete":["%s"]}
                """.formatted(outcomeId, applyLevelId, secondOutcomeId, temporaryId))
                .put("/api/courses/{courseId}/custom-learning-levels/batch", courseId)
                .then().statusCode(200)
                .body("createdLevelMappings[0].clientTempId", equalTo("new-level"))
                .body("result.levels", hasSize(3))
                .body("result.levels.find { it.id == '%s' }.learningOutcomeIds[0]"
                        .formatted(applyLevelId), equalTo(secondOutcomeId.toString()));

        given().header("Authorization", "Bearer " + token)
                .get("/api/courses/{courseId}/custom-learning-levels", courseId)
                .then().statusCode(200).body("total", equalTo(3))
                .body("levels.code", contains("CLL1", "CLL2", "CLL3"));

        given().contentType(ContentType.JSON).header("Authorization", "Bearer " + token).body("""
                {"learningOutcomeIds":["%s"],"code":" cll1 ","name":"Duplicate","sortOrder":4}
                """.formatted(outcomeId))
                .post("/api/courses/{courseId}/custom-learning-levels", courseId)
                .then().statusCode(400);
    }

    @Test
    @Order(3)
    void createsAndRetrievesNormalizedMatrix() {
        var response = given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token).body("""
                {"title":"Midterm","rows":[
                  {"chapterId":"%s","topic":"One","orderIndex":0,
                   "learningOutcomeIds":["%s"],
                   "levelCounts":[{"customLearningLevelId":"%s","questionCount":2}]},
                  {"chapterId":"%s","topic":"Two","orderIndex":1,
                   "learningOutcomeIds":["%s"],
                   "levelCounts":[{"customLearningLevelId":"%s","questionCount":3}]}
                ]}
                """.formatted(chapterId, outcomeId, understandLevelId,
                        chapterId, secondOutcomeId, applyLevelId))
                .post("/api/courses/{courseId}/exam-matrices", courseId)
                .then().statusCode(201).body("rows", hasSize(2))
                .body("rows[0].learningOutcomeIds[0]", equalTo(outcomeId.toString()))
                .body("rows[0].levelCounts[0].code", equalTo("CLL1"))
                .body("rows[0].levelCounts[0].learningOutcomeIds[0]", equalTo(outcomeId.toString()))
                .body("rows[0].levelCounts[0].name", equalTo("Understand"))
                .extract();
        matrixId = UUID.fromString(response.path("id"));
        firstRowId = UUID.fromString(response.path("rows[0].id"));
        secondRowId = UUID.fromString(response.path("rows[1].id"));

        given().header("Authorization", "Bearer " + token)
                .get("/api/courses/{courseId}/exam-matrices/{matrixId}", courseId, matrixId)
                .then().statusCode(200).body("rows", hasSize(2));
        given().header("Authorization", "Bearer " + token)
                .get("/api/courses/{courseId}/exam-matrices", courseId)
                .then().statusCode(200).body("matrices[0].rowCount", equalTo(2))
                .body("matrices[0].totalQuestionCount", equalTo(5))
                .body("matrices[0].rows", nullValue());
    }

    @Test
    @Order(4)
    void appliesCreateUpdateDeleteAsOneBatch() {
        given().contentType(ContentType.JSON).header("Authorization", "Bearer " + token).body("""
                {"metadata":{"title":"Final","description":"Saved together"},
                 "delete":["%s"],
                 "update":[{"id":"%s","chapterId":"%s","topic":"Updated","orderIndex":0,
                   "learningOutcomeIds":["%s"],
                   "levelCounts":[{"customLearningLevelId":"%s","questionCount":4}]}],
                 "create":[{"clientTempId":"tmp-new","chapterId":"%s","topic":"Created","orderIndex":1,
                   "learningOutcomeIds":["%s"],
                   "levelCounts":[{"customLearningLevelId":"%s","questionCount":1}]}]}
                """.formatted(firstRowId, secondRowId, chapterId, secondOutcomeId, applyLevelId,
                        chapterId, outcomeId, understandLevelId))
                .put("/api/courses/{courseId}/exam-matrices/{matrixId}/rows/batch", courseId, matrixId)
                .then().statusCode(200).body("createdRowMappings[0].clientTempId", equalTo("tmp-new"))
                .body("createdRowMappings[0].rowId", notNullValue())
                .body("matrix.title", equalTo("Final")).body("matrix.rows", hasSize(2))
                .body("matrix.rows[0].topic", equalTo("Updated"));
    }

    @Test
    @Order(5)
    void rejectsInvalidBatchWithoutDeletingExistingRow() {
        given().contentType(ContentType.JSON).header("Authorization", "Bearer " + token)
                .body("""
                {"delete":["%s"],"create":[
                  {"clientTempId":"bad","chapterId":"%s","orderIndex":1,
                   "learningOutcomeIds":["%s"],"levelCounts":[]}]}
                """.formatted(secondRowId, chapterId, outcomeId))
                .put("/api/courses/{courseId}/exam-matrices/{matrixId}/rows/batch", courseId, matrixId)
                .then().statusCode(400);

        given().header("Authorization", "Bearer " + token)
                .get("/api/courses/{courseId}/exam-matrices/{matrixId}", courseId, matrixId)
                .then().statusCode(200).body("rows", hasSize(2));
    }

    @Test
    @Order(6)
    void blocksDeletingUsedCustomLearningLevel() {
        given().header("Authorization", "Bearer " + token)
                .delete("/api/courses/{courseId}/custom-learning-levels/{levelId}", courseId, understandLevelId)
                .then().statusCode(400)
                .body("message", containsString("exam matrix count record"));
    }

    @Test
    @Order(7)
    void rejectsUnauthenticatedAccess() {
        given().get("/api/courses/{courseId}/exam-matrices", courseId).then().statusCode(403);
    }
}
