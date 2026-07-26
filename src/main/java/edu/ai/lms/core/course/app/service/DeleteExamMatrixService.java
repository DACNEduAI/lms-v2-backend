package edu.ai.lms.core.course.app.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.adapter.in.response.DeleteExamMatrixResponse;
import edu.ai.lms.core.course.app.domain.ExamMatrix;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.ExamMatrixNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherDeleteExamMatrixUseCase;
import edu.ai.lms.core.course.app.port.out.DeleteExamMatrixPort;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixPort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteExamMatrixService implements TeacherDeleteExamMatrixUseCase {

    private final DeleteExamMatrixPort deleteExamMatrixPort;
    private final LoadExamMatrixPort loadExamMatrixPort;
    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInUserPort loadSignedInUserPort;

    @Override
    @Transactional
    public DeleteExamMatrixResponse deleteExamMatrix(UUID courseId, UUID matrixId) {
        // Validate course exists
        var course = loadCoursePort.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found: " + courseId));

        // Validate authenticated teacher and ownership
        CurrentActor user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));

        if (!course.getTeacherIds().contains(user.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to manage this course");
        }

        // Validate exam matrix exists and belongs to this course
        ExamMatrix existing = loadExamMatrixPort.findById(matrixId)
                .orElseThrow(() -> new ExamMatrixNotFoundException("Exam matrix not found: " + matrixId));

        if (!existing.getCourseId().equals(courseId)) {
            throw new IllegalArgumentException("Exam matrix does not belong to this course");
        }

        deleteExamMatrixPort.deleteById(matrixId);

        return DeleteExamMatrixResponse.builder()
                .matrixId(matrixId)
                .message("Exam matrix deleted successfully")
                .success(true)
                .build();
    }
}
