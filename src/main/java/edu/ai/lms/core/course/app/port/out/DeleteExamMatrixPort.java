package edu.ai.lms.core.course.app.port.out;

import java.util.UUID;

public interface DeleteExamMatrixPort {

    void deleteById(UUID matrixId);
}
