package edu.ai.lms.core.course.app.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.ExamMatrix;

public interface LoadExamMatrixPort {

    Optional<ExamMatrix> findById(UUID matrixId);

    List<ExamMatrix> findAllByCourseId(UUID courseId);
}
