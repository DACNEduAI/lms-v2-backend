package edu.ai.lms.core.course.app.port.out;

import java.util.Optional;
import java.util.UUID;

import edu.ai.lms.core.course.app.domain.ExamMatrix;
import edu.ai.lms.core.course.app.domain.ExamMatrixRow;

public interface BatchEditExamMatrixPort {
    Optional<ExamMatrix> lockByCourseIdAndId(UUID courseId, UUID matrixId);
    void updateMetadata(UUID matrixId, String title, String description);
    void deleteRow(UUID matrixId, UUID rowId);
    void replaceRow(ExamMatrixRow row);
    void insertRow(ExamMatrixRow row);
    void touch(UUID matrixId);
}
