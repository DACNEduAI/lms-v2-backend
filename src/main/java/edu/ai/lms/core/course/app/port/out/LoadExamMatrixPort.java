package edu.ai.lms.core.course.app.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.ExamMatrix;
import edu.ai.lms.core.course.app.domain.ExamMatrixSummary;

public interface LoadExamMatrixPort {
    Optional<ExamMatrix> findById(UUID matrixId);
    Optional<ExamMatrix> findByCourseIdAndId(UUID courseId, UUID matrixId);
    List<ExamMatrixSummary> findAllSummariesByCourseId(UUID courseId);
}
