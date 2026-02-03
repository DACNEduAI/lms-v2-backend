package edu.ai.lms.core.auth.app.port.out;

import edu.ai.lms.core.auth.app.domain.Student;

public interface SaveStudentPort {
    boolean save(Student student);
}
