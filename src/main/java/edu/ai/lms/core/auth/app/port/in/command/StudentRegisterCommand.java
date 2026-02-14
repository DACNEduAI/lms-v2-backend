package edu.ai.lms.core.auth.app.port.in.command;

import edu.ai.lms.core.auth.app.domain.Student;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
public record StudentRegisterCommand(Student student) {
}
