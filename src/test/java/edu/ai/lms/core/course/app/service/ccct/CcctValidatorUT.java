package edu.ai.lms.core.course.app.service.ccct;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import edu.ai.lms.core.course.app.domain.CcctChapter;
import edu.ai.lms.core.course.app.domain.CcctCoverageStatus;
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
        UUID courseChapterId = UUID.randomUUID();
        var content = new CcctProfileContent(
                "Profile",
                CcctMode.STRUCTURED,
                2,
                true,
                "",
                "Template",
                List.of(new CcctChapterValue(
                        duplicate,
                        courseChapterId,
                        "Chapter",
                        "",
                        List.of(new CcctTopicValue(
                                duplicate,
                                "Topic",
                                List.of())))));

        assertThatThrownBy(() -> validator.validateDraft(course(courseChapterId), content))
                .isInstanceOf(CcctValidationException.class)
                .hasMessageContaining("Node IDs");
    }

    @Test
    void countsUtf8BytesWhenEnforcingTwoMegabyteLimit() {
        UUID chapterId = UUID.randomUUID();
        String multibyte = "ế".repeat(1_100_000);
        var content = new CcctProfileContent(
                "Profile",
                CcctMode.CUSTOM,
                2,
                false,
                "",
                "Template",
                List.of(new CcctChapterValue(
                        null,
                        chapterId,
                        "Chapter",
                        multibyte,
                        List.of())));

        assertThatThrownBy(() -> validator.validateDraft(course(chapterId), content))
                .isInstanceOf(CcctPayloadTooLargeException.class);
    }

    @Test
    void rejectsPublishWhenExamItemHasNoCurrentCourseChapterMapping() {
        CcctProfile profile = CcctProfile.builder()
                .formatVersion(2)
                .chapters(List.of(CcctChapter.builder()
                        .title("Chapter")
                        .topics(List.of(CcctTopic.builder()
                                .title("Topic")
                                .items(List.of(CcctItem.builder()
                                        .content("Item")
                                        .coverageStatus(CcctCoverageStatus.LEARN_AND_EXAM)
                                        .build()))
                                .build()))
                        .build()))
                .build();

        assertThatThrownBy(() -> validator.validatePublish(course(UUID.randomUUID()), profile))
                .isInstanceOf(CcctValidationException.class)
                .hasMessageContaining("mapped Course Chapter");
    }

    @Test
    void rejectsPublishWithoutLearnAndExamItems() {
        CcctProfile profile = CcctProfile.builder()
                .formatVersion(2)
                .chapters(List.of(CcctChapter.builder()
                        .title("Chapter")
                        .topics(List.of(CcctTopic.builder()
                                .title("Topic")
                                .items(List.of(CcctItem.builder()
                                        .content("Item")
                                        .coverageStatus(CcctCoverageStatus.LEARN_ONLY)
                                        .build()))
                                .build()))
                        .build()))
                .build();

        assertThatThrownBy(() -> validator.validatePublish(course(null), profile))
                .isInstanceOf(CcctValidationException.class)
                .hasMessageContaining("at least one LEARN_AND_EXAM");
    }

    @Test
    void rejectsChapterOrderThatDoesNotMatchCourse() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Course course = Course.builder().chapters(List.of(
                Chapter.builder().chapterId(first).title("First").build(),
                Chapter.builder().chapterId(second).title("Second").build())).build();
        var content = new CcctProfileContent(
                "Profile", CcctMode.CUSTOM, 2, false, "", "Profile", List.of(
                        new CcctChapterValue(null, second, "Second", "", List.of()),
                        new CcctChapterValue(null, first, "First", "", List.of())));

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

    private Course course(UUID chapterId) {
        return Course.builder()
                .chapters(chapterId == null
                        ? List.of()
                        : List.of(Chapter.builder().chapterId(chapterId).title("Chapter").build()))
                .build();
    }
}
