package edu.ai.lms.core.course.app.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import edu.ai.lms.core.course.adapter.in.response.ExamMatrixListResponse;
import edu.ai.lms.core.course.app.domain.ExamMatrix;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.port.in.usecase.GetAllExamMatricesUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetAllExamMatricesService implements GetAllExamMatricesUseCase {

    private final LoadExamMatrixPort loadExamMatrixPort;
    private final LoadCoursePort loadCoursePort;
    private final GetExamMatrixService getExamMatrixService;

    @Override
    public ExamMatrixListResponse getAllExamMatrices(UUID courseId) {
        // Validate course exists
        loadCoursePort.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found: " + courseId));

        List<ExamMatrix> matrices = loadExamMatrixPort.findAllByCourseId(courseId);

        return ExamMatrixListResponse.builder()
                .courseId(courseId)
                .total(matrices.size())
                .matrices(matrices.stream()
                        .map(getExamMatrixService::toResponse)
                        .collect(Collectors.toList()))
                .build();
    }
}
