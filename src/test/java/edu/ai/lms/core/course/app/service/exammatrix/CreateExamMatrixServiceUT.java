package edu.ai.lms.core.course.app.service.exammatrix;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.core.course.adapter.in.response.ExamMatrixResponse;
import edu.ai.lms.core.course.app.domain.*;
import edu.ai.lms.core.course.app.port.in.command.CreateExamMatrixCommand;
import edu.ai.lms.core.course.app.port.in.command.CustomLearningLevelValue;
import edu.ai.lms.core.course.app.port.out.*;

@ExtendWith(MockitoExtension.class)
class CreateExamMatrixServiceUT {
    @Mock SaveExamMatrixPort savePort;
    @Mock ExamMatrixAccess access;
    @Mock ExamMatrixValidator validator;
    @Mock ExamMatrixResponseAssembler assembler;
    @Mock CustomLearningLevelPort levelPort;
    @Mock BatchEditExamMatrixPort batchPort;
    @Mock LoadExamMatrixPort loadPort;
    @InjectMocks CreateExamMatrixService service;

    @Test
    void createsMatrixAndThreeMatrixOwnedLevelsAtomically() {
        UUID courseId = UUID.randomUUID();
        UUID outcomeId = UUID.randomUUID();
        Course course = Course.builder().id(courseId)
                .learningOutcomes(List.of(LearningOutcome.builder().id(outcomeId).build())).build();
        ExamMatrix reloaded = ExamMatrix.builder().id(UUID.randomUUID()).build();
        ExamMatrixResponse expected = ExamMatrixResponse.builder().id(reloaded.getId()).build();
        List<CustomLearningLevelValue> levels = List.of(
                level("temp-1", "CLL1", outcomeId, 0),
                level("temp-2", "CLL2", outcomeId, 1),
                level("temp-3", "CLL3", outcomeId, 2));
        var command = CreateExamMatrixCommand.builder().courseId(courseId).title("Matrix")
                .customLearningLevels(levels).rows(List.of()).build();
        when(access.requireCourseTeacher(courseId)).thenReturn(course);
        when(savePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(loadPort.findByCourseIdAndId(eq(courseId), any())).thenReturn(Optional.of(reloaded));
        when(assembler.toResponse(reloaded)).thenReturn(expected);

        assertSame(expected, service.createExamMatrix(command));
        verify(savePort).save(argThat(matrix -> matrix.getRows().isEmpty()));
        verify(levelPort, times(3)).insert(argThat(level -> level.getMatrixId() != null));
    }

    private CustomLearningLevelValue level(String tempId, String code, UUID outcomeId, int order) {
        return new CustomLearningLevelValue(
                null, tempId, List.of(outcomeId), code, code, null, order);
    }
}
