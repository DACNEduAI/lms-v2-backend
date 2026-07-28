package edu.ai.lms.core.course.app.service.exammatrix;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.core.course.app.domain.*;
import edu.ai.lms.core.course.app.exception.ExamMatrixValidationException;
import edu.ai.lms.core.course.app.port.in.command.*;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixReferencePort;

@ExtendWith(MockitoExtension.class)
class ExamMatrixValidatorUT {
    @Mock LoadExamMatrixReferencePort referencePort;
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
        var command = row(chapterId, outcomeId, levelId, 3, 0);

        var result = validator.validateAndMap(course, matrixId, List.of(command));

        assertEquals(List.of(outcomeId), result.getFirst().getLearningOutcomeIds());
        assertEquals(3, result.getFirst().getLevelCounts().getFirst().getQuestionCount());
        assertEquals("Understand", result.getFirst().getLevelCounts().getFirst().getName());
    }

    @Test
    void rejectsOutcomeNotAssignedToChapter() {
        UUID chapterId = UUID.randomUUID(), outcomeId = UUID.randomUUID();
        Course course = Course.builder()
                .chapters(List.of(Chapter.builder().chapterId(chapterId).learningOutcomeIds(List.of()).build()))
                .learningOutcomes(List.of(LearningOutcome.builder().id(outcomeId).build())).build();
        assertThrows(ExamMatrixValidationException.class,
                () -> validator.validateAndMap(course, null,
                        List.of(row(chapterId, outcomeId, null, 0, 0))));
    }

    @Test
    void rejectsDuplicateOrderIndexes() {
        UUID chapterId = UUID.randomUUID(), outcomeId = UUID.randomUUID();
        Course course = course(chapterId, outcomeId);
        assertThrows(ExamMatrixValidationException.class,
                () -> validator.validateAndMap(course, null, List.of(
                        row(chapterId, outcomeId, null, 0, 0),
                        row(chapterId, outcomeId, null, 0, 0))));
    }

    private Course course(UUID chapterId, UUID outcomeId) {
        return Course.builder()
                .id(UUID.randomUUID())
                .chapters(List.of(Chapter.builder().chapterId(chapterId)
                        .learningOutcomeIds(List.of(outcomeId)).build()))
                .learningOutcomes(List.of(LearningOutcome.builder().id(outcomeId).build())).build();
    }

    private ExamMatrixRowCommand row(UUID chapterId, UUID outcomeId, UUID levelId, int count, int order) {
        return ExamMatrixRowCommand.builder().chapterId(chapterId).orderIndex(order)
                .learningOutcomeIds(List.of(outcomeId))
                .levelCounts(levelId == null ? List.of() : List.of(
                        new LearningLevelCountCommand(levelId, null, count))).build();
    }
}
