package edu.ai.lms.core.course.app.service.exammatrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.core.course.adapter.in.response.ExamMatrixSummaryResponse;
import edu.ai.lms.core.course.app.domain.ExamMatrixSummary;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixPort;

@ExtendWith(MockitoExtension.class)
class GetAllExamMatricesServiceUT {
    @Mock LoadExamMatrixPort loadPort;
    @Mock ExamMatrixAccess access;
    @Mock ExamMatrixResponseAssembler assembler;
    @InjectMocks GetAllExamMatricesService service;

    @Test
    void returnsSummaryListWithoutLoadingRows() {
        UUID courseId = UUID.randomUUID();
        ExamMatrixSummary summary = ExamMatrixSummary.builder().id(UUID.randomUUID()).rowCount(2).build();
        when(loadPort.findAllSummariesByCourseId(courseId)).thenReturn(List.of(summary));
        when(assembler.toSummary(summary)).thenReturn(ExamMatrixSummaryResponse.builder()
                .id(summary.getId()).rowCount(2).build());
        var response = service.getAllExamMatrices(courseId);
        assertEquals(1, response.getTotal());
        assertEquals(2, response.getMatrices().getFirst().getRowCount());
    }
}
