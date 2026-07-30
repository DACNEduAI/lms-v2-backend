package edu.ai.lms.core.course.adapter.in;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

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
class CcctProfileControllerIT extends IntegrationTestConfiguration {
    @Autowired
    private JdbcTemplate jdbc;

    private static String ownerToken;
    private static String otherTeacherToken;
    private static UUID courseId;
    private static UUID otherCourseId;
    private static UUID chapterId;
    private static UUID otherChapterId;
    private static UUID draftId;
    private static UUID publishedId;
    private static UUID cloneId;

    @Test
    @Order(1)
    void setupTeachersCoursesAndChapters() {
        ownerToken = registerTeacher("ccct.owner@example.com", "EMP-CCCT-OWNER");
        otherTeacherToken = registerTeacher("ccct.other@example.com", "EMP-CCCT-OTHER");
        courseId = createCourse(ownerToken, "CCCT-OWNER-COURSE");
        otherCourseId = createCourse(otherTeacherToken, "CCCT-OTHER-COURSE");
        chapterId = insertChapter(courseId, "Algorithms");
        otherChapterId = insertChapter(otherCourseId, "Other chapter");
    }

    @Test
    @Order(2)
    void rejectsCrossCourseChapterAndMalformedEnum() {
        givenOwner().body(createBody("Wrong mapping", otherChapterId, "LEARN_AND_EXAM", "Sort"))
                .post(basePath(), courseId)
                .then().statusCode(400)
                .body("error", equalTo("Invalid CCCT"));

        givenOwner().body(createBody("Bad enum", chapterId, "EXAM", "Sort"))
                .post(basePath(), courseId)
                .then().statusCode(400);
    }

    @Test
    @Order(3)
    void createsListsAndGetsDraft() {
        var response = givenOwner()
                .body(createBody("Midterm CCCT", chapterId, "LEARN_AND_EXAM", "Sorting"))
                .post(basePath(), courseId)
                .then().statusCode(201)
                .body("courseId", equalTo(courseId.toString()))
                .body("status", equalTo("DRAFT"))
                .body("version", equalTo(0))
                .body("template.chapters", hasSize(1))
                .extract();
        draftId = UUID.fromString(response.path("id"));

        givenOwner().get(basePath(), courseId)
                .then().statusCode(200)
                .body("total", equalTo(1))
                .body("profiles[0].id", equalTo(draftId.toString()));
        givenOwner().get(basePath() + "/{profileId}", courseId, draftId)
                .then().statusCode(200)
                .body("template.chapters[0].topics[0].items[0].content", equalTo("Sorting"));
    }

    @Test
    @Order(4)
    void rejectsDuplicateNameAndStaleUpdateThenUpdatesDraft() {
        givenOwner().body(createBody("  midterm ccct  ", chapterId, "LEARN_ONLY", "Duplicate"))
                .post(basePath(), courseId)
                .then().statusCode(409);

        givenOwner().body(updateBody(9, "Midterm CCCT", chapterId, "Updated"))
                .put(basePath() + "/{profileId}", courseId, draftId)
                .then().statusCode(409);

        givenOwner().body(updateBody(0, "Midterm CCCT", chapterId, "Updated sorting"))
                .put(basePath() + "/{profileId}", courseId, draftId)
                .then().statusCode(200)
                .body("version", equalTo(1))
                .body("template.chapters[0].topics[0].items[0].content",
                        equalTo("Updated sorting"));
    }

    @Test
    @Order(5)
    void rejectsPublishWithoutExamEligibleItem() {
        UUID learnOnlyDraft = UUID.fromString(givenOwner()
                .body(createBody("Learning only", null, "LEARN_ONLY", "Reading"))
                .post(basePath(), courseId)
                .then().statusCode(201)
                .extract().path("id"));

        givenOwner().body("{\"version\":0}")
                .post(basePath() + "/{profileId}/publish", courseId, learnOnlyDraft)
                .then().statusCode(400)
                .body("error", equalTo("Invalid CCCT"));
    }

    @Test
    @Order(6)
    void publishesDraftAndProtectsPublishedProfileFromMutation() {
        givenOwner().body("{\"version\":1}")
                .post(basePath() + "/{profileId}/publish", courseId, draftId)
                .then().statusCode(200)
                .body("status", equalTo("PUBLISHED"))
                .body("version", equalTo(2));
        publishedId = draftId;

        givenOwner().get(basePath() + "/published", courseId)
                .then().statusCode(200)
                .body("id", equalTo(publishedId.toString()));
        givenOwner().body(updateBody(2, "Cannot edit", chapterId, "Mutation"))
                .put(basePath() + "/{profileId}", courseId, publishedId)
                .then().statusCode(409);
        givenOwner().delete(basePath() + "/{profileId}", courseId, publishedId)
                .then().statusCode(409);
    }

    @Test
    @Order(7)
    void clonesWithNewNodeIdsAndPublishingCloneArchivesPreviousProfile() {
        var cloneResponse = givenOwner().body("{\"name\":\"Final CCCT\"}")
                .post(basePath() + "/{profileId}/clone", courseId, publishedId)
                .then().statusCode(201)
                .body("status", equalTo("DRAFT"))
                .body("version", equalTo(0))
                .body("template.chapters[0].id",
                        not(equalTo(getChapterNodeId(publishedId))))
                .extract();
        cloneId = UUID.fromString(cloneResponse.path("id"));

        givenOwner().body("{\"version\":0}")
                .post(basePath() + "/{profileId}/publish", courseId, cloneId)
                .then().statusCode(200)
                .body("status", equalTo("PUBLISHED"));
        givenOwner().get(basePath() + "/{profileId}", courseId, publishedId)
                .then().statusCode(200)
                .body("status", equalTo("ARCHIVED"));
        givenOwner().get(basePath() + "/published", courseId)
                .then().statusCode(200)
                .body("id", equalTo(cloneId.toString()));
    }

    @Test
    @Order(8)
    void enforcesTeacherOwnershipAndHidesCrossCourseProfile() {
        given().header("Authorization", "Bearer " + otherTeacherToken)
                .get(basePath(), courseId)
                .then().statusCode(403);

        given().header("Authorization", "Bearer " + otherTeacherToken)
                .get(basePath() + "/{profileId}", otherCourseId, cloneId)
                .then().statusCode(404);
    }

    @Test
    @Order(9)
    void deletesOnlyDraftAndReturnsNoContent() {
        UUID disposable = UUID.fromString(givenOwner()
                .body(createBody("Disposable", null, "SKIP", "Out of scope"))
                .post(basePath(), courseId)
                .then().statusCode(201)
                .extract().path("id"));

        givenOwner().delete(basePath() + "/{profileId}", courseId, disposable)
                .then().statusCode(204);
        givenOwner().get(basePath() + "/{profileId}", courseId, disposable)
                .then().statusCode(404);
    }

    private io.restassured.specification.RequestSpecification givenOwner() {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + ownerToken);
    }

    private String registerTeacher(String email, String employeeId) {
        return given().contentType(ContentType.JSON).body("""
                {"firstName":"CCCT","lastName":"Teacher","email":"%s",
                 "password":"TeacherPass123!","employeeId":"%s"}
                """.formatted(email, employeeId))
                .post("/api/auth/teacher/register")
                .then().statusCode(201)
                .extract().path("token");
    }

    private UUID createCourse(String token, String code) {
        String start = LocalDate.now().plusDays(1)
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String end = LocalDate.now().plusMonths(2)
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
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
        jdbc.update(
                "INSERT INTO edu.chapters (chapter_id,title,course_id) VALUES (?,?,?)",
                id,
                title,
                targetCourseId);
        return id;
    }

    private String getChapterNodeId(UUID profileId) {
        return givenOwner().get(basePath() + "/{profileId}", courseId, profileId)
                .then().statusCode(200)
                .extract().path("template.chapters[0].id");
    }

    private String basePath() {
        return "/api/courses/{courseId}/ccct-profiles";
    }

    private String createBody(
            String name,
            UUID mappedChapterId,
            String status,
            String content) {
        return writeBody(null, name, mappedChapterId, status, content);
    }

    private String updateBody(
            long version,
            String name,
            UUID mappedChapterId,
            String content) {
        return writeBody(version, name, mappedChapterId, "LEARN_AND_EXAM", content);
    }

    private String writeBody(
            Long version,
            String name,
            UUID mappedChapterId,
            String status,
            String content) {
        String versionField = version == null ? "" : "\"version\":" + version + ",";
        String chapterMapping = mappedChapterId == null
                ? "\"courseChapterId\":null,"
                : "\"courseChapterId\":\"" + mappedChapterId + "\",";
        return """
                {%s
                 "name":"%s","lastMode":"STRUCTURED","markdown":"# CCCT",
                 "template":{"title":"Course Content Coverage Template","chapters":[
                   {"id":"%s",%s"title":"Algorithms","topics":[
                     {"id":"%s","title":"Sorting","items":[
                       {"id":"%s","content":"%s","status":"%s","note":""}
                     ]}
                   ]}
                 ]}}
                """.formatted(
                versionField,
                name,
                UUID.randomUUID(),
                chapterMapping,
                UUID.randomUUID(),
                UUID.randomUUID(),
                content,
                status);
    }
}
