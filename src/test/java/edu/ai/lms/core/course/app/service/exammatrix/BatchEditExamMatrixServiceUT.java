package edu.ai.lms.core.course.app.service.exammatrix;

import static org.junit.jupiter.api.Assertions.*;
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
import edu.ai.lms.core.course.app.exception.ExamMatrixValidationException;
import edu.ai.lms.core.course.app.port.in.command.*;
import edu.ai.lms.core.course.app.port.out.*;

@ExtendWith(MockitoExtension.class)
class BatchEditExamMatrixServiceUT {
    @Mock BatchEditExamMatrixPort batchPort;
    @Mock LoadExamMatrixPort loadPort;
    @Mock ExamMatrixAccess access;
    @Mock ExamMatrixValidator validator;
    @Mock ExamMatrixResponseAssembler assembler;
    @InjectMocks BatchEditExamMatrixService service;

    @Test
    void appliesDeleteUpdateCreateAndReturnsTempIdMapping() {
        UUID courseId = UUID.randomUUID(), matrixId = UUID.randomUUID();
        UUID deleteId = UUID.randomUUID(), updateId = UUID.randomUUID();
        Course course = Course.builder().id(courseId).build();
        ExamMatrix existing = ExamMatrix.builder().id(matrixId).courseId(courseId)
                .rows(List.of(ExamMatrixRow.builder().id(deleteId).orderIndex(0).build(),
                        ExamMatrixRow.builder().id(updateId).orderIndex(1).build())).build();
        var update = ExamMatrixRowCommand.builder().id(updateId).orderIndex(2).build();
        var create = ExamMatrixRowCommand.builder().clientTempId("tmp-1").orderIndex(1).build();
        ExamMatrixRow updated = ExamMatrixRow.builder().id(updateId).matrixId(matrixId).orderIndex(2).build();
        ExamMatrixRow created = ExamMatrixRow.builder().matrixId(matrixId).orderIndex(1).build();
        var command = BatchEditExamMatrixCommand.builder().courseId(courseId).matrixId(matrixId)
                .create(List.of(create)).update(List.of(update)).delete(List.of(deleteId)).build();
        when(access.requireCourseTeacher(courseId)).thenReturn(course);
        when(batchPort.lockByCourseIdAndId(courseId, matrixId)).thenReturn(Optional.of(existing));
        when(validator.validateAndMap(course, matrixId, List.of(update))).thenReturn(List.of(updated));
        when(validator.validateAndMap(course, matrixId, List.of(create))).thenReturn(List.of(created));
        when(loadPort.findByCourseIdAndId(courseId, matrixId)).thenReturn(Optional.of(existing));
        when(assembler.toResponse(existing)).thenReturn(ExamMatrixResponse.builder().id(matrixId).build());

        var response = service.batchEdit(command);

        verify(batchPort).deleteRow(matrixId, deleteId);
        verify(batchPort).replaceRow(updated);
        verify(batchPort).insertRow(created);
        assertEquals("tmp-1", response.getCreatedRowMappings().getFirst().getClientTempId());
        assertNotNull(response.getCreatedRowMappings().getFirst().getRowId());
    }

    @Test
    void rejectsEmptyBatch() {
        UUID courseId = UUID.randomUUID();
        when(access.requireCourseTeacher(courseId)).thenReturn(Course.builder().id(courseId).build());
        var command = BatchEditExamMatrixCommand.builder()
                .courseId(courseId).matrixId(UUID.randomUUID()).build();
        assertThrows(ExamMatrixValidationException.class, () -> service.batchEdit(command));
        verifyNoInteractions(batchPort);
    }
}
