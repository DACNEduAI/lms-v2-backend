package edu.ai.lms.core.course.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import edu.ai.lms.config.IntegrationTestConfiguration;
import edu.ai.lms.core.course.app.domain.CcctChapter;
import edu.ai.lms.core.course.app.domain.CcctCoverageStatus;
import edu.ai.lms.core.course.app.domain.CcctItem;
import edu.ai.lms.core.course.app.domain.CcctMode;
import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.domain.CcctProfileStatus;
import edu.ai.lms.core.course.app.domain.CcctTopic;
import edu.ai.lms.core.course.app.port.out.DeleteCcctProfilePort;
import edu.ai.lms.core.course.app.port.out.LoadCcctProfilePort;
import edu.ai.lms.core.course.app.port.out.SaveCcctProfilePort;

class CcctPersistenceIT extends IntegrationTestConfiguration {

    @Autowired
    private SaveCcctProfilePort savePort;

    @Autowired
    private LoadCcctProfilePort loadPort;

    @Autowired
    private DeleteCcctProfilePort deletePort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsLoadsAndWipeReplacesDraftWithOptimisticLocking() {
        UUID courseId = createCourse();
        UUID courseChapterId = createCourseChapter(courseId, "Chapter 1");
        CcctProfile saved = savePort.create(profile(
                courseId,
                "Midterm blueprint",
                CcctProfileStatus.DRAFT,
                courseChapterId,
                "Original content"));

        assertThat(saved.getVersion()).isZero();
        assertThat(saved.getChapters()).hasSize(1);
        assertThat(saved.getChapters().getFirst().getTopics().getFirst().getItems())
                .extracting(CcctItem::getContent)
                .containsExactly("Original content");
        UUID originalChapterId = saved.getChapters().getFirst().getId();
        UUID originalTopicId = saved.getChapters().getFirst().getTopics().getFirst().getId();
        UUID originalItemId = saved.getChapters().getFirst().getTopics().getFirst().getItems().getFirst().getId();

        CcctProfile replacement = profile(
                courseId,
                "Midterm blueprint",
                CcctProfileStatus.DRAFT,
                courseChapterId,
                "Replacement content");
        replacement.setId(saved.getId());

        assertThat(savePort.replaceDraft(replacement, 0)).isTrue();

        CcctProfile reloaded = loadPort.findByCourseIdAndId(courseId, saved.getId()).orElseThrow();
        assertThat(reloaded.getVersion()).isEqualTo(1);
        assertThat(reloaded.getChapters().getFirst().getTopics().getFirst().getItems())
                .extracting(CcctItem::getContent)
                .containsExactly("Replacement content");
        assertThat(count("edu.ccct_chapters", "profile_id", saved.getId())).isEqualTo(1);
        assertThat(count("edu.ccct_topics", "chapter_id", reloaded.getChapters().getFirst().getId()))
                .isEqualTo(1);
        assertThat(count("edu.ccct_chapters", "id", originalChapterId)).isZero();
        assertThat(count("edu.ccct_topics", "id", originalTopicId)).isZero();
        assertThat(count("edu.ccct_items", "id", originalItemId)).isZero();

        CcctProfile staleReplacement = profile(
                courseId,
                "Midterm blueprint",
                CcctProfileStatus.DRAFT,
                courseChapterId,
                "Must not overwrite");
        staleReplacement.setId(saved.getId());

        assertThat(savePort.replaceDraft(staleReplacement, 0)).isFalse();
        CcctProfile afterConflict = loadPort.findByCourseIdAndId(courseId, saved.getId()).orElseThrow();
        assertThat(afterConflict.getVersion()).isEqualTo(1);
        assertThat(afterConflict.getChapters().getFirst().getTopics().getFirst().getItems()
                .getFirst().getContent()).isEqualTo("Replacement content");
    }

    @Test
    void deletingCourseChapterOnlyClearsCcctMapping() {
        UUID courseId = createCourse();
        UUID courseChapterId = createCourseChapter(courseId, "Chapter to delete");
        CcctProfile saved = savePort.create(profile(
                courseId,
                "Historical blueprint",
                CcctProfileStatus.DRAFT,
                courseChapterId,
                "Historical content"));

        jdbcTemplate.update(
                "DELETE FROM edu.chapters WHERE chapter_id = ?",
                courseChapterId);

        CcctProfile reloaded = loadPort.findByCourseIdAndId(courseId, saved.getId()).orElseThrow();
        CcctChapter historicalChapter = reloaded.getChapters().getFirst();
        assertThat(historicalChapter.getCourseChapterId()).isNull();
        assertThat(historicalChapter.getTitle()).isEqualTo("Chapter to delete");
        assertThat(historicalChapter.getTopics().getFirst().getItems().getFirst().getContent())
                .isEqualTo("Historical content");
    }

    @Test
    void archivedProfileRemainsPinnedAndSameItemCannotAppearTwiceInMatrix() {
        UUID courseId = createCourse();
        UUID courseChapterId = createCourseChapter(courseId, "Pinned chapter");
        CcctProfile profile = savePort.create(profile(
                courseId,
                "Published blueprint",
                CcctProfileStatus.PUBLISHED,
                courseChapterId,
                "Pinned exam content"));
        UUID itemId = profile.getChapters().getFirst().getTopics().getFirst().getItems().getFirst().getId();
        UUID matrixId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO edu.exam_matrices (id, course_id, ccct_profile_id, title)
                VALUES (?, ?, ?, ?)
                """,
                matrixId,
                courseId,
                profile.getId(),
                "Pinned matrix");
        insertMatrixRow(matrixId, courseChapterId, itemId, 0);

        assertThatThrownBy(() -> insertMatrixRow(matrixId, courseChapterId, itemId, 1))
                .isInstanceOf(DataIntegrityViolationException.class);

        jdbcTemplate.update(
                "UPDATE edu.ccct_profiles SET status = 'ARCHIVED' WHERE id = ?",
                profile.getId());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT ccct_profile_id FROM edu.exam_matrices WHERE id = ?",
                UUID.class,
                matrixId)).isEqualTo(profile.getId());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT ccct_item_id FROM edu.exam_matrix_rows WHERE matrix_id = ?",
                UUID.class,
                matrixId)).isEqualTo(itemId);
    }

    @Test
    void databaseAllowsOnlyOnePublishedProfilePerCourse() {
        UUID courseId = createCourse();
        UUID firstChapter = createCourseChapter(courseId, "Chapter A");
        UUID secondChapter = createCourseChapter(courseId, "Chapter B");

        savePort.create(profile(
                courseId,
                "Published A",
                CcctProfileStatus.PUBLISHED,
                firstChapter,
                "Content A"));

        assertThatThrownBy(() -> savePort.create(profile(
                courseId,
                "Published B",
                CcctProfileStatus.PUBLISHED,
                secondChapter,
                "Content B")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletesDraftWithItsTreeButDoesNotDeletePublishedProfile() {
        UUID draftCourseId = createCourse();
        UUID draftChapterId = createCourseChapter(draftCourseId, "Draft chapter");
        CcctProfile draft = savePort.create(profile(
                draftCourseId,
                "Disposable draft",
                CcctProfileStatus.DRAFT,
                draftChapterId,
                "Draft content"));
        UUID draftTreeChapterId = draft.getChapters().getFirst().getId();

        assertThat(deletePort.deleteDraft(draftCourseId, draft.getId())).isTrue();
        assertThat(loadPort.findByCourseIdAndId(draftCourseId, draft.getId())).isEmpty();
        assertThat(count("edu.ccct_chapters", "id", draftTreeChapterId)).isZero();

        UUID publishedCourseId = createCourse();
        UUID publishedChapterId = createCourseChapter(publishedCourseId, "Published chapter");
        CcctProfile published = savePort.create(profile(
                publishedCourseId,
                "Protected profile",
                CcctProfileStatus.PUBLISHED,
                publishedChapterId,
                "Published content"));

        assertThat(deletePort.deleteDraft(publishedCourseId, published.getId())).isFalse();
        assertThat(loadPort.findByCourseIdAndId(publishedCourseId, published.getId())).isPresent();
    }

    private CcctProfile profile(
            UUID courseId,
            String name,
            CcctProfileStatus status,
            UUID courseChapterId,
            String itemContent) {
        CcctItem item = CcctItem.builder()
                .content(itemContent)
                .coverageStatus(CcctCoverageStatus.LEARN_AND_EXAM)
                .note("")
                .build();
        CcctTopic topic = CcctTopic.builder()
                .title("Topic 1")
                .items(List.of(item))
                .build();
        CcctChapter chapter = CcctChapter.builder()
                .courseChapterId(courseChapterId)
                .title(courseChapterTitle(courseChapterId))
                .topics(List.of(topic))
                .build();
        return CcctProfile.builder()
                .courseId(courseId)
                .name(name)
                .templateTitle("Course Content Coverage Template")
                .status(status)
                .lastMode(CcctMode.STRUCTURED)
                .markdownContent("")
                .chapters(List.of(chapter))
                .build();
    }

    private UUID createCourse() {
        UUID courseId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO edu.courses
                    (id, course_name, course_code, start_date, end_date)
                VALUES (?, ?, ?, NOW(), NOW() + INTERVAL '30 days')
                """,
                courseId,
                "CCCT integration course",
                "CCCT-" + courseId);
        return courseId;
    }

    private UUID createCourseChapter(UUID courseId, String title) {
        UUID chapterId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO edu.chapters (chapter_id, title, course_id) VALUES (?, ?, ?)",
                chapterId,
                title,
                courseId);
        return chapterId;
    }

    private String courseChapterTitle(UUID courseChapterId) {
        return jdbcTemplate.queryForObject(
                "SELECT title FROM edu.chapters WHERE chapter_id = ?",
                String.class,
                courseChapterId);
    }

    private void insertMatrixRow(
            UUID matrixId,
            UUID chapterId,
            UUID itemId,
            int orderIndex) {
        jdbcTemplate.update(
                """
                INSERT INTO edu.exam_matrix_rows
                    (id, matrix_id, chapter_id, ccct_item_id, topic, order_index)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                matrixId,
                chapterId,
                itemId,
                "Pinned exam content",
                orderIndex);
    }

    private int count(String table, String foreignKey, UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + foreignKey + " = ?",
                Integer.class,
                id);
    }
}
