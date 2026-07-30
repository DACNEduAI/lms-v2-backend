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
        var content = new CcctProfileContent(
                "Profile",
                CcctMode.STRUCTURED,
                "",
                "Template",
                List.of(new CcctChapterValue(
                        duplicate,
                        null,
                        "Chapter",
                        List.of(new CcctTopicValue(
                                duplicate,
                                "Topic",
                                List.of())))));

        assertThatThrownBy(() -> validator.validateDraft(course(null), content))
                .isInstanceOf(CcctValidationException.class)
                .hasMessageContaining("Node IDs");
    }

    @Test
    void countsUtf8BytesWhenEnforcingTwoMegabyteLimit() {
        String multibyte = "ế".repeat(1_100_000);
        var content = new CcctProfileContent(
                "Profile",
                CcctMode.MARKDOWN,
                multibyte,
                "Template",
                List.of());

        assertThatThrownBy(() -> validator.validateDraft(course(null), content))
                .isInstanceOf(CcctPayloadTooLargeException.class);
    }

    @Test
    void rejectsPublishWhenExamItemHasNoCurrentCourseChapterMapping() {
        CcctProfile profile = CcctProfile.builder()
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

    private Course course(UUID chapterId) {
        return Course.builder()
                .chapters(chapterId == null
                        ? List.of()
                        : List.of(Chapter.builder().chapterId(chapterId).build()))
                .build();
    }
}
