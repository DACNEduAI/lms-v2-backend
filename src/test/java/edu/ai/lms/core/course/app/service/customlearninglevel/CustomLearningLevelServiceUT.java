package edu.ai.lms.core.course.app.service.customlearninglevel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.CustomLearningLevel;
import edu.ai.lms.core.course.app.domain.ExamMatrix;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.exception.CustomLearningLevelValidationException;
import edu.ai.lms.core.course.app.port.in.command.BatchEditCustomLearningLevelsCommand;
import edu.ai.lms.core.course.app.port.in.command.CustomLearningLevelValue;
import edu.ai.lms.core.course.app.port.out.CustomLearningLevelPort;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixPort;
import edu.ai.lms.core.course.app.service.exammatrix.ExamMatrixAccess;

@ExtendWith(MockitoExtension.class)
class CustomLearningLevelServiceUT {
    @Mock ExamMatrixAccess access;
    @Mock CustomLearningLevelPort port;
    @Mock LoadExamMatrixPort matrixPort;
    @InjectMocks CustomLearningLevelService service;

    UUID courseId;
    UUID matrixId;
    UUID outcomeId;
    UUID secondOutcomeId;
    Course course;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        matrixId = UUID.randomUUID();
        outcomeId = UUID.randomUUID();
        secondOutcomeId = UUID.randomUUID();
        course = Course.builder().id(courseId)
                .learningOutcomes(List.of(
                        LearningOutcome.builder().id(outcomeId).courseId(courseId).build(),
                        LearningOutcome.builder().id(secondOutcomeId).courseId(courseId).build()))
                .build();
        lenient().when(access.requireCourseTeacher(courseId)).thenReturn(course);
        lenient().when(matrixPort.findByCourseIdAndId(courseId, matrixId))
                .thenReturn(Optional.of(ExamMatrix.builder().id(matrixId).courseId(courseId).build()));
    }

    @Test
    void createsTrimmedMatrixOwnedLevel() {
        var value = value(null, null, List.of(outcomeId), " CLL1 ", " Understand ", 0);
        when(port.lockAllByMatrixId(matrixId)).thenReturn(List.of());
        when(port.findByMatrixIdAndId(eq(matrixId), any())).thenAnswer(invocation ->
                Optional.of(level(invocation.getArgument(1), "CLL1", "Understand", List.of(outcomeId))));

        var response = service.create(courseId, matrixId, value);

        assertEquals("CLL1", response.code());
        verify(port).insert(argThat(level -> matrixId.equals(level.getMatrixId())
                && level.getCode().equals("CLL1")));
    }

    @Test
    void allowsRemovingAPreviouslyUsedOutcomeMapping() {
        CustomLearningLevel first = level(UUID.randomUUID(), "CLL1", "Understand",
                List.of(outcomeId, secondOutcomeId));
        CustomLearningLevel second = level(UUID.randomUUID(), "CLL2", "Apply", List.of(outcomeId));
        CustomLearningLevel third = level(UUID.randomUUID(), "CLL3", "Create", List.of(secondOutcomeId));
        when(port.lockAllByMatrixId(matrixId)).thenReturn(List.of(first, second, third));
        when(port.findAllByMatrixId(matrixId)).thenReturn(List.of(first, second, third));

        var command = new BatchEditCustomLearningLevelsCommand(
                courseId, matrixId, List.of(),
                List.of(value(first.getId(), null, List.of(outcomeId), "CLL1", "Understand", 0)),
                List.of());

        assertDoesNotThrow(() -> service.batchEdit(command));
        verify(port).update(argThat(level -> level.getLearningOutcomeIds().equals(List.of(outcomeId))));
    }

    @Test
    void rejectsFinalStateWithFewerThanThreeLevels() {
        CustomLearningLevel first = level(UUID.randomUUID(), "CLL1", "Understand", List.of(outcomeId));
        CustomLearningLevel second = level(UUID.randomUUID(), "CLL2", "Apply", List.of(secondOutcomeId));
        when(port.lockAllByMatrixId(matrixId)).thenReturn(List.of(first, second));

        var command = new BatchEditCustomLearningLevelsCommand(
                courseId, matrixId, List.of(), List.of(), List.of(first.getId()));

        assertThrows(CustomLearningLevelValidationException.class, () -> service.batchEdit(command));
        verify(port, never()).delete(any());
    }

    @Test
    void rejectsFinalStateWithAnUncoveredOutcome() {
        CustomLearningLevel first = level(UUID.randomUUID(), "CLL1", "Understand", List.of(outcomeId));
        CustomLearningLevel second = level(UUID.randomUUID(), "CLL2", "Apply", List.of(outcomeId));
        CustomLearningLevel third = level(UUID.randomUUID(), "CLL3", "Create", List.of(outcomeId));
        when(port.lockAllByMatrixId(matrixId)).thenReturn(List.of(first, second, third));

        var command = new BatchEditCustomLearningLevelsCommand(
                courseId, matrixId, List.of(),
                List.of(value(first.getId(), null, List.of(outcomeId), "CLL1A", "Understand", 0)),
                List.of());

        assertThrows(CustomLearningLevelValidationException.class, () -> service.batchEdit(command));
    }

    private CustomLearningLevelValue value(
            UUID id, String tempId, List<UUID> outcomes, String code, String name, int order) {
        return new CustomLearningLevelValue(id, tempId, outcomes, code, name, null, order);
    }

    private CustomLearningLevel level(
            UUID id, String code, String name, List<UUID> outcomes) {
        return CustomLearningLevel.builder()
                .id(id).matrixId(matrixId).learningOutcomeIds(outcomes)
                .code(code).name(name).sortOrder(0).build();
    }
}
