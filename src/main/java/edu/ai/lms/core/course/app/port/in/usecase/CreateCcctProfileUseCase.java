package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.port.in.command.CreateCcctProfileCommand;

public interface CreateCcctProfileUseCase {
    CcctProfile create(CreateCcctProfileCommand command);
}
