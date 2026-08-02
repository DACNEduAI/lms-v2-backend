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
    void mapsTeacherAuthoredTopicAndRelationships() {
        UUID chapterId = UUID.randomUUID(), outcomeId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID(), matrixId = UUID.randomUUID();
        Course course = course(chapterId, outcomeId);
        when(referencePort.findCustomLearningLevelsByIds(anyCollection())).thenReturn(List.of(
                CustomLearningLevel.builder().id(levelId).matrixId(matrixId)
                        .learningOutcomeIds(List.of(outcomeId))
                        .name("Understand").sortOrder(1).build()));

        var result = validator.validateAndMap(course, matrixId,
                List.of(row(chapterId, "  Teacher topic  ", outcomeId, levelId, 3, 0)));

        assertEquals("Teacher topic", result.getFirst().getTopic());
        assertEquals(List.of(outcomeId), result.getFirst().getLearningOutcomeIds());
        assertEquals(3, result.getFirst().getLevelCounts().getFirst().getQuestionCount());
    }

    @Test
    void permitsDuplicateTopicsButRejectsDuplicateOrderIndexes() {
        UUID chapterId = UUID.randomUUID(), outcomeId = UUID.randomUUID();
        Course course = course(chapterId, outcomeId);
        assertEquals(2, validator.validateAndMap(course, null, List.of(
                row(chapterId, "Same topic", outcomeId, null, 0, 0),
                row(chapterId, "Same topic", outcomeId, null, 0, 1))).size());
        assertThrows(ExamMatrixValidationException.class,
                () -> validator.validateAndMap(course, null, List.of(
                        row(chapterId, "First", outcomeId, null, 0, 0),
                        row(chapterId, "Second", outcomeId, null, 0, 0))));
    }

    @Test
    void rejectsBlankTopic() {
        UUID chapterId = UUID.randomUUID(), outcomeId = UUID.randomUUID();
        var error = assertThrows(ExamMatrixValidationException.class,
                () -> validator.validateAndMap(course(chapterId, outcomeId), null,
                        List.of(row(chapterId, "  ", outcomeId, null, 0, 0))));
        assertTrue(error.getMessage().contains("topic"));
    }

    @Test
    void rejectsOutcomeNotAssignedToSelectedChapter() {
        UUID chapterId = UUID.randomUUID(), outcomeId = UUID.randomUUID();
        Course course = Course.builder()
                .chapters(List.of(Chapter.builder().chapterId(chapterId).learningOutcomeIds(List.of()).build()))
                .learningOutcomes(List.of(LearningOutcome.builder().id(outcomeId).build())).build();
        assertThrows(ExamMatrixValidationException.class,
                () -> validator.validateAndMap(course, null,
                        List.of(row(chapterId, "Topic", outcomeId, null, 0, 0))));
    }

    @Test
    void rejectsUnsupportedQuestionType() {
        UUID chapterId = UUID.randomUUID(), outcomeId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID(), matrixId = UUID.randomUUID();
        Course course = course(chapterId, outcomeId);
        when(referencePort.findCustomLearningLevelsByIds(anyCollection())).thenReturn(List.of(
                CustomLearningLevel.builder().id(levelId).matrixId(matrixId)
                        .learningOutcomeIds(List.of(outcomeId)).build()));
        var command = ExamMatrixRowCommand.builder().chapterId(chapterId).topic("Topic").orderIndex(0)
                .learningOutcomeIds(List.of(outcomeId))
                .levelCounts(List.of(new LearningLevelCountCommand(
                        levelId, null, ExamMatrixQuestionType.ESSAY, 1))).build();

        var error = assertThrows(ExamMatrixValidationException.class,
                () -> validator.validateAndMap(course, matrixId, List.of(command)));
        assertTrue(error.getMessage().contains("UNSUPPORTED_QUESTION_TYPE"));
    }

    private Course course(UUID chapterId, UUID outcomeId) {
        return Course.builder().id(UUID.randomUUID())
                .chapters(List.of(Chapter.builder().chapterId(chapterId)
                        .learningOutcomeIds(List.of(outcomeId)).build()))
                .learningOutcomes(List.of(LearningOutcome.builder().id(outcomeId).build())).build();
    }

    private ExamMatrixRowCommand row(
            UUID chapterId, String topic, UUID outcomeId, UUID levelId, int count, int order) {
        return ExamMatrixRowCommand.builder().chapterId(chapterId).topic(topic).orderIndex(order)
                .learningOutcomeIds(List.of(outcomeId))
                .levelCounts(levelId == null ? List.of() : List.of(
                        new LearningLevelCountCommand(
                                levelId, null, ExamMatrixQuestionType.MULTIPLE_CHOICE, count))).build();
    }
}
