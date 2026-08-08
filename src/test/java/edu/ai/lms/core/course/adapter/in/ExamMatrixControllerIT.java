package edu.ai.lms.core.course.adapter.in;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void createsMatrixWithItsLevelsAndRowsAtomically() {
        var response = given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token).body("""
                {"title":"Midterm",
                 "customLearningLevels":[
                  {"clientTempId":"level-understand","learningOutcomeIds":["%s","%s"],
                   "code":"CLL1","name":"Understand","description":"Recall and explain","sortOrder":0},
                  {"clientTempId":"level-apply","learningOutcomeIds":["%s","%s"],
                   "code":"CLL2","name":"Apply","sortOrder":1},
                  {"clientTempId":"level-analyze","learningOutcomeIds":["%s","%s"],
                   "code":"CLL3","name":"Analyze","sortOrder":2}],
                 "rows":[
                   {"chapterId":"%s","topic":"Teacher topic one","orderIndex":0,
                    "learningOutcomeIds":["%s"],
                    "levelCounts":[{"customLearningLevelClientTempId":"level-understand",
                      "questionType":"MULTIPLE_CHOICE","questionCount":2}]},
                   {"chapterId":"%s","topic":"Teacher topic two","orderIndex":1,
                    "learningOutcomeIds":["%s"],
                    "levelCounts":[{"customLearningLevelClientTempId":"level-apply",
                      "questionType":"MULTIPLE_CHOICE","questionCount":3}]}
                 ]}
                """.formatted(
                        outcomeId, secondOutcomeId,
                        outcomeId, secondOutcomeId,
                        outcomeId, secondOutcomeId,
                        chapterId, outcomeId,
                        chapterId, secondOutcomeId))
                .post("/api/courses/{courseId}/exam-matrices", courseId)
                .then().statusCode(201).body("rows", hasSize(2))
                .body("customLearningLevels", hasSize(3))
                 .body("configurationStatus", equalTo("COMPLETE"))
                 .body("rows[0].topic", equalTo("Teacher topic one"))
                .body("rows[0].learningOutcomeIds[0]", equalTo(outcomeId.toString()))
                .body("rows[0].levelCounts[0].code", equalTo("CLL1"))
                .body("rows[0].levelCounts[0].learningOutcomeIds[0]", equalTo(outcomeId.toString()))
                .body("rows[0].levelCounts[0].name", equalTo("Understand"))
                .body("rows[0].levelCounts[0].active", equalTo(true))
                .extract();
        matrixId = UUID.fromString(response.path("id"));
        firstRowId = UUID.fromString(response.path("rows[0].id"));
        secondRowId = UUID.fromString(response.path("rows[1].id"));
        understandLevelId = UUID.fromString(response.path(
                "customLearningLevels.find { it.code == 'CLL1' }.id"));
        applyLevelId = UUID.fromString(response.path(
                "customLearningLevels.find { it.code == 'CLL2' }.id"));

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
    @Order(3)
    void synchronizesMatrixRowCandidatesWhenChapterOutcomesChange() {
         UUID unrelatedChapterId = UUID.randomUUID();
         UUID unrelatedRowId = UUID.randomUUID();
        jdbc.update("INSERT INTO edu.chapters (chapter_id,title,course_id) VALUES (?,?,?)",
                unrelatedChapterId, "Unrelated chapter", courseId);
        jdbc.update("""
                INSERT INTO edu.chapter_learning_outcomes (chapter_id,learning_outcome_id)
                VALUES (?,?)
                """, unrelatedChapterId, secondOutcomeId);
         jdbc.update("""
                 INSERT INTO edu.exam_matrix_rows
                   (id,matrix_id,chapter_id,topic,order_index)
                 VALUES (?,?,?,?,?)
                 """, unrelatedRowId, matrixId, unrelatedChapterId, "Unrelated row", 2);
        jdbc.update("""
                INSERT INTO edu.exam_matrix_row_learning_outcomes
                  (exam_matrix_row_id,learning_outcome_id,is_selected)
                VALUES (?,?,TRUE)
                """, unrelatedRowId, secondOutcomeId);

        given().contentType(ContentType.JSON).header("Authorization", "Bearer " + token)
                .body("""
                {"learningOutcomeIds":["%s"]}
                """.formatted(secondOutcomeId))
                .put("/api/{courseId}/chapter/{chapterId}/learning-outcomes/remove",
                        courseId, chapterId)
                .then().statusCode(200)
                .body("totalRemoved", equalTo(1));

        Integer mappingsAfterRemoval = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM edu.exam_matrix_row_learning_outcomes mapping
                JOIN edu.exam_matrix_rows matrix_row
                  ON matrix_row.id = mapping.exam_matrix_row_id
                WHERE matrix_row.matrix_id = ?
                  AND matrix_row.chapter_id = ?
                  AND mapping.learning_outcome_id = ?
                """, Integer.class, matrixId, chapterId, secondOutcomeId);
        assertEquals(0, mappingsAfterRemoval);
        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM edu.exam_matrix_row_learning_outcomes
                WHERE exam_matrix_row_id = ? AND learning_outcome_id = ?
                """, Integer.class, unrelatedRowId, secondOutcomeId));
        assertEquals(3, jdbc.queryForObject("""
                SELECT question_count
                FROM edu.exam_matrix_row_learning_level_counts
                WHERE exam_matrix_row_id = ? AND custom_learning_level_id = ?
                """, Integer.class, secondRowId, applyLevelId));

        given().header("Authorization", "Bearer " + token)
                .get("/api/courses/{courseId}/exam-matrices/{matrixId}", courseId, matrixId)
                .then().statusCode(200)
                .body("rows.find { it.id == '%s' }.levelCounts.find { it.customLearningLevelId == '%s' }.active"
                        .formatted(secondRowId, applyLevelId), equalTo(false))
                .body("invalidRowIds", hasItem(secondRowId.toString()));

        given().contentType(ContentType.JSON).header("Authorization", "Bearer " + token)
                .body("""
                {"learningOutcomeIds":["%s"]}
                """.formatted(secondOutcomeId))
                .put("/api/{courseId}/chapter/{chapterId}/learning-outcomes/add",
                        courseId, chapterId)
                .then().statusCode(200)
                .body("totalAdded", equalTo(1));

        assertEquals(2, jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM edu.exam_matrix_row_learning_outcomes mapping
                JOIN edu.exam_matrix_rows matrix_row
                  ON matrix_row.id = mapping.exam_matrix_row_id
                WHERE matrix_row.matrix_id = ?
                  AND matrix_row.chapter_id = ?
                  AND mapping.learning_outcome_id = ?
                  AND mapping.is_selected = FALSE
                """, Integer.class, matrixId, chapterId, secondOutcomeId));

        given().contentType(ContentType.JSON).header("Authorization", "Bearer " + token)
                .body("""
                {"learningOutcomeIds":["%s"]}
                """.formatted(secondOutcomeId))
                .put("/api/{courseId}/chapter/{chapterId}/learning-outcomes/add",
                        courseId, chapterId)
                .then().statusCode(200)
                .body("totalAdded", equalTo(0));

        assertEquals(2, jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM edu.exam_matrix_row_learning_outcomes mapping
                JOIN edu.exam_matrix_rows matrix_row
                  ON matrix_row.id = mapping.exam_matrix_row_id
                WHERE matrix_row.matrix_id = ?
                  AND matrix_row.chapter_id = ?
                  AND mapping.learning_outcome_id = ?
                  AND mapping.is_selected = FALSE
                """, Integer.class, matrixId, chapterId, secondOutcomeId));
        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM edu.exam_matrix_row_learning_outcomes
                WHERE exam_matrix_row_id = ?
                  AND learning_outcome_id = ?
                  AND is_selected = TRUE
                """, Integer.class, firstRowId, outcomeId));

        given().contentType(ContentType.JSON).header("Authorization", "Bearer " + token).body("""
                {"update":[{"id":"%s","chapterId":"%s","topic":"Teacher topic two","orderIndex":1,
                  "learningOutcomeIds":["%s"],
                  "levelCounts":[{"customLearningLevelId":"%s",
                    "questionType":"MULTIPLE_CHOICE","questionCount":3}]}]}
                """.formatted(secondRowId, chapterId,
                        secondOutcomeId, applyLevelId))
                .put("/api/courses/{courseId}/exam-matrices/{matrixId}/configuration/batch",
                        courseId, matrixId)
                .then().statusCode(200)
                .body("matrix.configurationStatus", equalTo("COMPLETE"))
                .body("matrix.rows.find { it.id == '%s' }.levelCounts[0].active"
                        .formatted(secondRowId), equalTo(true));

         jdbc.update("DELETE FROM edu.exam_matrix_rows WHERE id = ?", unrelatedRowId);
         jdbc.update("DELETE FROM edu.chapters WHERE chapter_id = ?", unrelatedChapterId);
    }

    @Test
    @Order(4)
    void rollsBackChapterAssignmentWhenMatrixCandidateSynchronizationFails() {
        UUID rejectedOutcomeId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO edu.learning_outcomes
                  (id,prefix_number,description,sort_order,course_id)
                VALUES (?,?,?,?,?)
                """, rejectedOutcomeId, 99, "Rollback synchronization test", 99, courseId);
        jdbc.execute("""
                ALTER TABLE edu.exam_matrix_row_learning_outcomes
                ADD CONSTRAINT reject_sync_test_outcome
                CHECK (learning_outcome_id <> '%s'::uuid) NOT VALID
                """.formatted(rejectedOutcomeId));

        try {
            given().contentType(ContentType.JSON).header("Authorization", "Bearer " + token)
                    .body("""
                    {"learningOutcomeIds":["%s"]}
                    """.formatted(rejectedOutcomeId))
                    .put("/api/{courseId}/chapter/{chapterId}/learning-outcomes/add",
                            courseId, chapterId)
                    .then().statusCode(greaterThanOrEqualTo(500));

            assertEquals(0, jdbc.queryForObject("""
                    SELECT COUNT(*)
                    FROM edu.chapter_learning_outcomes
                    WHERE chapter_id = ? AND learning_outcome_id = ?
                    """, Integer.class, chapterId, rejectedOutcomeId));
        } finally {
            jdbc.execute("""
                    ALTER TABLE edu.exam_matrix_row_learning_outcomes
                    DROP CONSTRAINT IF EXISTS reject_sync_test_outcome
                    """);
            jdbc.update("DELETE FROM edu.learning_outcomes WHERE id = ?", rejectedOutcomeId);
        }
    }

    @Test
    @Order(5)
    void unchecksUsedLevelOutcomeMappingWithoutDeletingItsCount() {
        given().contentType(ContentType.JSON).header("Authorization", "Bearer " + token).body("""
                {"customLearningLevels":{"update":[
                  {"id":"%s","learningOutcomeIds":["%s"],"code":"CLL2","name":"Apply",
                   "description":"Apply a known method","sortOrder":1}]}}
                """.formatted(applyLevelId, outcomeId))
                .put("/api/courses/{courseId}/exam-matrices/{matrixId}/configuration/batch",
                        courseId, matrixId)
                .then().statusCode(200)
                .body("matrix.configurationStatus", equalTo("COMPLETE"))
                .body("matrix.rows.find { it.id == '%s' }.levelCounts.find { it.customLearningLevelId == '%s' }.questionCount"
                        .formatted(secondRowId, applyLevelId), equalTo(3))
                .body("matrix.rows.find { it.id == '%s' }.levelCounts.find { it.customLearningLevelId == '%s' }.active"
                        .formatted(secondRowId, applyLevelId), equalTo(false));

        given().header("Authorization", "Bearer " + token)
                .get("/api/courses/{courseId}/exam-matrices/{matrixId}/custom-learning-levels",
                        courseId, matrixId)
                .then().statusCode(200).body("total", equalTo(3))
                .body("levels.code", contains("CLL1", "CLL2", "CLL3"))
                .body("levels.find { it.id == '%s' }.learningOutcomeIds"
                        .formatted(applyLevelId), contains(outcomeId.toString()));

        jdbc.update("""
                UPDATE edu.exam_matrix_row_learning_level_counts
                SET question_type = NULL
                WHERE exam_matrix_row_id = ? AND custom_learning_level_id = ?
                """, secondRowId, applyLevelId);

        given().contentType(ContentType.JSON).header("Authorization", "Bearer " + token).body("""
                {"metadata":{"title":"Midterm","description":"Dormant legacy allocation ignored"}}
                """)
                .put("/api/courses/{courseId}/exam-matrices/{matrixId}/configuration/batch",
                        courseId, matrixId)
                .then().statusCode(200)
                .body("matrix.configurationStatus", equalTo("COMPLETE"));

        given().header("Authorization", "Bearer " + token)
                .get("/api/courses/{courseId}/exam-matrices", courseId)
                .then().statusCode(200)
                .body("matrices.find { it.id == '%s' }.configurationStatus".formatted(matrixId),
                        equalTo("COMPLETE"));
    }

    @Test
    @Order(6)
    void appliesCreateUpdateDeleteAsOneBatch() {
        given().contentType(ContentType.JSON).header("Authorization", "Bearer " + token).body("""
                {"metadata":{"title":"Final","description":"Saved together"},
                 "delete":["%s"],
                 "update":[{"id":"%s","chapterId":"%s","topic":"Updated independent topic","orderIndex":0,
                   "learningOutcomeIds":["%s"],
                   "levelCounts":[{"customLearningLevelId":"%s",
                     "questionType":"MULTIPLE_CHOICE","questionCount":4}]}],
                 "create":[{"clientTempId":"tmp-new","chapterId":"%s",
                    "topic":"New independent topic","orderIndex":1,
                   "learningOutcomeIds":["%s"],
                   "levelCounts":[{"customLearningLevelId":"%s",
                     "questionType":"MULTIPLE_CHOICE","questionCount":1}]}]}
                """.formatted(firstRowId, secondRowId, chapterId,
                        secondOutcomeId, understandLevelId,
                        chapterId, outcomeId, understandLevelId))
                .put("/api/courses/{courseId}/exam-matrices/{matrixId}/configuration/batch",
                        courseId, matrixId)
                .then().statusCode(200).body("createdRowMappings[0].clientTempId", equalTo("tmp-new"))
                .body("createdRowMappings[0].rowId", notNullValue())
                .body("matrix.title", equalTo("Final")).body("matrix.rows", hasSize(2))
                 .body("matrix.rows[0].topic", equalTo("Updated independent topic"))
                 .body("matrix.rows[1].topic", equalTo("New independent topic"));
    }

    @Test
    @Order(7)
    void rejectsInvalidBatchWithoutDeletingExistingRow() {
        given().contentType(ContentType.JSON).header("Authorization", "Bearer " + token)
                .body("""
                {"delete":["%s"],"create":[
                  {"clientTempId":"bad","chapterId":"%s","topic":" ","orderIndex":1,
                   "learningOutcomeIds":["%s"],"levelCounts":[]}]}
                """.formatted(secondRowId, chapterId, outcomeId))
                .put("/api/courses/{courseId}/exam-matrices/{matrixId}/configuration/batch",
                        courseId, matrixId)
                .then().statusCode(400);

        given().header("Authorization", "Bearer " + token)
                .get("/api/courses/{courseId}/exam-matrices/{matrixId}", courseId, matrixId)
                .then().statusCode(200).body("rows", hasSize(2));
    }

    @Test
    @Order(8)
    void rejectsDeletingALevelWhenItWouldLeaveFewerThanThree() {
        given().header("Authorization", "Bearer " + token)
                .delete("/api/courses/{courseId}/exam-matrices/{matrixId}/custom-learning-levels/{levelId}",
                        courseId, matrixId, understandLevelId)
                .then().statusCode(400)
                .body("message", containsString("at least three"));
    }

    @Test
    @Order(9)
    void rejectsUnauthenticatedAccess() {
        given().get("/api/courses/{courseId}/exam-matrices", courseId).then().statusCode(403);
    }
}
