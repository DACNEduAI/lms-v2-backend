package edu.ai.lms.core.course.app.service.exammatrix;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.core.course.app.domain.*;
import edu.ai.lms.core.course.app.exception.ExamMatrixValidationException;
import edu.ai.lms.core.course.app.port.in.command.*;
import edu.ai.lms.core.course.app.port.out.LoadCcctProfilePort;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixReferencePort;

@ExtendWith(MockitoExtension.class)
class ExamMatrixValidatorUT {
    @Mock LoadExamMatrixReferencePort referencePort;
    @Mock LoadCcctProfilePort ccctProfilePort;
    @InjectMocks ExamMatrixValidator validator;

    @Test
    void mapsValidChapterOutcomeAndLevelRelationships() {
        UUID chapterId = UUID.randomUUID(), outcomeId = UUID.randomUUID(), levelId = UUID.randomUUID();
        UUID matrixId = UUID.randomUUID();
        Course course = course(chapterId, outcomeId);
        when(referencePort.findCustomLearningLevelsByIds(anyCollection())).thenReturn(List.of(
                CustomLearningLevel.builder().id(levelId).matrixId(matrixId)
                        .learningOutcomeIds(List.of(outcomeId))
                        .name("Understand").sortOrder(1).build()));
        UUID itemId = UUID.randomUUID();
        var command = row(chapterId, itemId, outcomeId, levelId, 3, 0);
        CcctProfile profile = profile(course.getId(), chapterId, itemId);

        var result = validator.validateAndMap(course, matrixId, profile, List.of(command));

        assertEquals(List.of(outcomeId), result.getFirst().getLearningOutcomeIds());
        assertEquals(3, result.getFirst().getLevelCounts().getFirst().getQuestionCount());
        assertEquals("Understand", result.getFirst().getLevelCounts().getFirst().getName());
        assertEquals(ExamMatrixQuestionType.MULTIPLE_CHOICE,
                result.getFirst().getLevelCounts().getFirst().getQuestionType());
        assertEquals(itemId, result.getFirst().getCcctItemId());
        assertEquals("Canonical content", result.getFirst().getTopic());
    }

    @Test
    void rejectsOutcomeNotAssignedToChapter() {
        UUID chapterId = UUID.randomUUID(), outcomeId = UUID.randomUUID();
        Course course = Course.builder()
                .chapters(List.of(Chapter.builder().chapterId(chapterId).learningOutcomeIds(List.of()).build()))
                .learningOutcomes(List.of(LearningOutcome.builder().id(outcomeId).build())).build();
        UUID itemId = UUID.randomUUID();
        assertThrows(ExamMatrixValidationException.class,
                () -> validator.validateAndMap(course, null, profile(course.getId(), chapterId, itemId),
                        List.of(row(chapterId, itemId, outcomeId, null, 0, 0))));
    }

    @Test
    void rejectsDuplicateOrderIndexes() {
        UUID chapterId = UUID.randomUUID(), outcomeId = UUID.randomUUID();
        Course course = course(chapterId, outcomeId);
        UUID firstItemId = UUID.randomUUID(), secondItemId = UUID.randomUUID();
        CcctProfile profile = profile(course.getId(), chapterId, firstItemId, secondItemId);
        assertThrows(ExamMatrixValidationException.class,
                () -> validator.validateAndMap(course, null, profile, List.of(
                        row(chapterId, firstItemId, outcomeId, null, 0, 0),
                        row(chapterId, secondItemId, outcomeId, null, 0, 0))));
    }

    @Test
    void rejectsDuplicateCcctItems() {
        UUID chapterId = UUID.randomUUID(), outcomeId = UUID.randomUUID(), itemId = UUID.randomUUID();
        Course course = course(chapterId, outcomeId);
        CcctProfile profile = profile(course.getId(), chapterId, itemId);

        var error = assertThrows(ExamMatrixValidationException.class,
                () -> validator.validateAndMap(course, null, profile, List.of(
                        row(chapterId, itemId, outcomeId, null, 0, 0),
                        row(chapterId, itemId, outcomeId, null, 0, 1))));

        assertTrue(error.getMessage().contains("ccctItemId"));
    }

    @Test
    void rejectsItemThatIsNotLearnAndExam() {
        UUID chapterId = UUID.randomUUID(), outcomeId = UUID.randomUUID(), itemId = UUID.randomUUID();
        Course course = course(chapterId, outcomeId);
        CcctProfile profile = profile(
                course.getId(), chapterId, itemId, CcctCoverageStatus.LEARN_ONLY);

        var error = assertThrows(ExamMatrixValidationException.class,
                () -> validator.validateAndMap(
                        course, null, profile, List.of(row(chapterId, itemId, outcomeId, null, 0, 0))));

        assertTrue(error.getMessage().contains("not eligible"));
    }

    @Test
    void rejectsItemMappedToAnotherCourseChapter() {
        UUID selectedChapterId = UUID.randomUUID(), mappedChapterId = UUID.randomUUID();
        UUID outcomeId = UUID.randomUUID(), itemId = UUID.randomUUID();
        Course course = course(selectedChapterId, outcomeId);
        CcctProfile profile = profile(course.getId(), mappedChapterId, itemId);

        var error = assertThrows(ExamMatrixValidationException.class,
                () -> validator.validateAndMap(course, null, profile,
                        List.of(row(selectedChapterId, itemId, outcomeId, null, 0, 0))));

        assertTrue(error.getMessage().contains("selected course chapter"));
    }

    @Test
    void rejectsUnsupportedQuestionType() {
        UUID chapterId = UUID.randomUUID(), outcomeId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID(), levelId = UUID.randomUUID(), matrixId = UUID.randomUUID();
        Course course = course(chapterId, outcomeId);
        when(referencePort.findCustomLearningLevelsByIds(anyCollection())).thenReturn(List.of(
                CustomLearningLevel.builder().id(levelId).matrixId(matrixId)
                        .learningOutcomeIds(List.of(outcomeId)).name("Apply").sortOrder(1).build()));
        var command = ExamMatrixRowCommand.builder()
                .chapterId(chapterId).ccctItemId(itemId).orderIndex(0)
                .learningOutcomeIds(List.of(outcomeId))
                .levelCounts(List.of(new LearningLevelCountCommand(
                        levelId, null, ExamMatrixQuestionType.ESSAY, 1)))
                .build();

        var error = assertThrows(ExamMatrixValidationException.class,
                () -> validator.validateAndMap(
                        course, matrixId, profile(course.getId(), chapterId, itemId), List.of(command)));

        assertTrue(error.getMessage().contains("UNSUPPORTED_QUESTION_TYPE"));
    }

    @Test
    void newMatrixRequiresPublishedProfileFromSameCourse() {
        UUID courseId = UUID.randomUUID(), profileId = UUID.randomUUID();
        when(ccctProfilePort.findByCourseIdAndId(courseId, profileId))
                .thenReturn(Optional.of(CcctProfile.builder()
                        .id(profileId).courseId(courseId).status(CcctProfileStatus.ARCHIVED).build()));

        assertThrows(ExamMatrixValidationException.class,
                () -> validator.requirePublishedProfile(courseId, profileId));
    }

    @Test
    void existingMatrixKeepsArchivedPinButCannotRetarget() {
        UUID courseId = UUID.randomUUID(), pinnedId = UUID.randomUUID();
        when(ccctProfilePort.findByCourseIdAndId(courseId, pinnedId))
                .thenReturn(Optional.of(CcctProfile.builder()
                        .id(pinnedId).courseId(courseId).status(CcctProfileStatus.ARCHIVED).build()));

        assertEquals(pinnedId, validator.requirePinnedProfile(courseId, pinnedId, pinnedId).getId());
        assertThrows(ExamMatrixValidationException.class,
                () -> validator.requirePinnedProfile(courseId, UUID.randomUUID(), pinnedId));
    }

    private Course course(UUID chapterId, UUID outcomeId) {
        return Course.builder()
                .id(UUID.randomUUID())
                .chapters(List.of(Chapter.builder().chapterId(chapterId)
                        .learningOutcomeIds(List.of(outcomeId)).build()))
                .learningOutcomes(List.of(LearningOutcome.builder().id(outcomeId).build())).build();
    }

    private ExamMatrixRowCommand row(
            UUID chapterId, UUID itemId, UUID outcomeId, UUID levelId, int count, int order) {
        return ExamMatrixRowCommand.builder().chapterId(chapterId).ccctItemId(itemId).orderIndex(order)
                .learningOutcomeIds(List.of(outcomeId))
                .levelCounts(levelId == null ? List.of() : List.of(
                        new LearningLevelCountCommand(
                                levelId, null, ExamMatrixQuestionType.MULTIPLE_CHOICE, count))).build();
    }

    private CcctProfile profile(UUID courseId, UUID chapterId, UUID... itemIds) {
        return profile(courseId, chapterId, CcctCoverageStatus.LEARN_AND_EXAM, itemIds);
    }

    private CcctProfile profile(
            UUID courseId,
            UUID chapterId,
            UUID itemId,
            CcctCoverageStatus coverageStatus) {
        return profile(courseId, chapterId, coverageStatus, itemId);
    }

    private CcctProfile profile(
            UUID courseId,
            UUID chapterId,
            CcctCoverageStatus coverageStatus,
            UUID... itemIds) {
        return CcctProfile.builder()
                .id(UUID.randomUUID()).courseId(courseId).status(CcctProfileStatus.PUBLISHED)
                .chapters(List.of(CcctChapter.builder()
                        .courseChapterId(chapterId)
                        .topics(List.of(CcctTopic.builder()
                                .items(java.util.Arrays.stream(itemIds).map(itemId -> CcctItem.builder()
                                        .id(itemId).content("Canonical content")
                                        .coverageStatus(coverageStatus).build()).toList())
                                .build()))
                        .build()))
                .build();
    }
}
