package edu.ai.lms.core.auth.app.port.in.command;

import edu.ai.lms.core.auth.app.domain.Teacher;
import lombok.Builder;

@Builder
public record TeacherRegisterCommand(Teacher teacher) {
}
