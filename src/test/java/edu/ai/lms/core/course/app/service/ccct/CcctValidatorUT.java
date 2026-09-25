package edu.ai.lms.core.course.app.service.ccct;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import edu.ai.lms.core.course.app.domain.CcctChapter;
import edu.ai.lms.core.course.app.domain.CcctItem;
import edu.ai.lms.core.course.app.domain.CcctMode;
import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.domain.CcctTopic;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.exception.CcctPayloadTooLargeException;
import edu.ai.lms.core.course.app.exception.CcctValidationException;
import edu.ai.lms.core.course.app.port.in.command.CcctChapterValue;
import edu.ai.lms.core.course.app.port.in.command.CcctItemValue;
import edu.ai.lms.core.course.app.port.in.command.CcctProfileContent;
import edu.ai.lms.core.course.app.port.in.command.CcctTopicValue;

class CcctValidatorUT {
    private final CcctValidator validator = new CcctValidator();

    @Test
    void rejectsDuplicateNodeIdsAcrossDifferentLevels() {
        UUID duplicate = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        var content = content(List.of(new CcctChapterValue(duplicate, chapterId, "Chapter",
                List.of(new CcctTopicValue(duplicate, "topic", "Topic", List.of())))));

        assertThatThrownBy(() -> validator.validateDraft(course(chapterId), content))
                .isInstanceOf(CcctValidationException.class)
                .hasMessageContaining("Node IDs");
    }

    @Test
    void countsUtf8BytesWhenEnforcingTwoMegabyteLimit() {
        UUID chapterId = UUID.randomUUID();
        String multibyte = "ế".repeat(1_100_000);
        var content = content(List.of(new CcctChapterValue(null, chapterId, "Chapter",
                List.of(new CcctTopicValue(null, "topic", "Topic", List.of(new CcctItemValue(null, multibyte)))))));

        assertThatThrownBy(() -> validator.validateDraft(course(chapterId), content))
                .isInstanceOf(CcctPayloadTooLargeException.class);
    }

    @Test
    void permitsPublishingContentWithoutCoverageStatus() {
        CcctProfile profile = CcctProfile.builder().formatVersion(2).chapters(List.of(
                CcctChapter.builder().title("Chapter").topics(List.of(
                        CcctTopic.builder().title("Topic").items(List.of(
                                CcctItem.builder().content("Item").build())).build())).build())).build();

        validator.validatePublish(course(UUID.randomUUID()), profile);
    }

    @Test
    void rejectsChapterOrderThatDoesNotMatchCourse() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Course course = Course.builder().chapters(List.of(
                Chapter.builder().chapterId(first).title("First").build(),
                Chapter.builder().chapterId(second).title("Second").build())).build();
        var content = content(List.of(
                new CcctChapterValue(null, second, "Second", List.of()),
                new CcctChapterValue(null, first, "First", List.of())));

        assertThatThrownBy(() -> validator.validateDraft(course, content))
                .isInstanceOf(CcctValidationException.class)
                .hasMessageContaining("order");
    }

    @Test
    void rejectsLegacyProfilePublishing() {
        CcctProfile legacy = CcctProfile.builder().formatVersion(1).build();
        assertThatThrownBy(() -> validator.validatePublish(course(null), legacy))
                .isInstanceOf(CcctValidationException.class)
                .hasMessageContaining("Legacy");
    }

    @Test
    void rejectsInvalidOrDuplicateGroupIdsWithinOneChapter() {
        UUID chapterId = UUID.randomUUID();
        var invalid = content(List.of(new CcctChapterValue(null, chapterId, "Chapter",
                List.of(new CcctTopicValue(null, "Invalid Group", "Topic", List.of())))));
        assertThatThrownBy(() -> validator.validateDraft(course(chapterId), invalid))
                .isInstanceOf(CcctValidationException.class)
                .hasMessageContaining("lowercase slug");

        var duplicate = content(List.of(new CcctChapterValue(null, chapterId, "Chapter", List.of(
                new CcctTopicValue(null, "same_group", "One", List.of()),
                new CcctTopicValue(null, "same_group", "Two", List.of())))));
        assertThatThrownBy(() -> validator.validateDraft(course(chapterId), duplicate))
                .isInstanceOf(CcctValidationException.class)
                .hasMessageContaining("unique within the CCCT profile");
    }

    private CcctProfileContent content(List<CcctChapterValue> chapters) {
        return new CcctProfileContent("Profile", CcctMode.STRUCTURED, 2, true, "", "Template", chapters);
    }

    private Course course(UUID chapterId) {
        return Course.builder().chapters(chapterId == null ? List.of()
                : List.of(Chapter.builder().chapterId(chapterId).title("Chapter").build())).build();
    }
}
