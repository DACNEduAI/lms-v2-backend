package edu.ai.lms.core.course.app.port.out;

import edu.ai.lms.core.course.app.domain.ExamMatrix;

public interface SaveExamMatrixPort {

    ExamMatrix save(ExamMatrix examMatrix);
}
