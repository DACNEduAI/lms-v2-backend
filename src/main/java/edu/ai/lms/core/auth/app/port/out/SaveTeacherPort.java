package edu.ai.lms.core.auth.app.port.out;

import edu.ai.lms.core.auth.app.domain.Teacher;

public interface SaveTeacherPort {
    boolean save(Teacher teacher);
}
