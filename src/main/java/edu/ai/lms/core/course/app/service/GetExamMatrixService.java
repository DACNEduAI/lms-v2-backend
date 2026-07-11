package edu.ai.lms.core.course.app.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import edu.ai.lms.core.course.adapter.in.response.ExamMatrixResponse;
import edu.ai.lms.core.course.adapter.in.response.ExamMatrixRowResponse;
import edu.ai.lms.core.course.app.domain.ExamMatrix;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.ExamMatrixNotFoundException;
import edu.ai.lms.core.course.app.port.in.usecase.GetExamMatrixUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetExamMatrixService implements GetExamMatrixUseCase {

    private final LoadExamMatrixPort loadExamMatrixPort;
    private final LoadCoursePort loadCoursePort;

    @Override
    public ExamMatrixResponse getExamMatrix(UUID courseId, UUID matrixId) {
        // Validate course exists
        loadCoursePort.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found: " + courseId));

        // Validate exam matrix exists and belongs to this course
        ExamMatrix matrix = loadExamMatrixPort.findById(matrixId)
                .orElseThrow(() -> new ExamMatrixNotFoundException("Exam matrix not found: " + matrixId));

        if (!matrix.getCourseId().equals(courseId)) {
            throw new ExamMatrixNotFoundException("Exam matrix not found in course: " + matrixId);
        }

        return toResponse(matrix);
    }

    ExamMatrixResponse toResponse(ExamMatrix matrix) {
        List<ExamMatrixRowResponse> rowResponses = matrix.getRows().stream()
                .map(r -> ExamMatrixRowResponse.builder()
                        .id(r.getId())
                        .learningOutcomeId(r.getLearningOutcomeId())
                        .chapterId(r.getChapterId())
                        .topic(r.getTopic())
                        .understandCount(r.getUnderstandCount())
                        .applyCount(r.getApplyCount())
                        .applyPlusCount(r.getApplyPlusCount())
                        .orderIndex(r.getOrderIndex())
                        .build())
                .collect(Collectors.toList());

        return ExamMatrixResponse.builder()
                .id(matrix.getId())
                .courseId(matrix.getCourseId())
                .title(matrix.getTitle())
                .description(matrix.getDescription())
                .rows(rowResponses)
                .createdAt(matrix.getCreatedAt())
                .updatedAt(matrix.getUpdatedAt())
                .build();
    }
}
