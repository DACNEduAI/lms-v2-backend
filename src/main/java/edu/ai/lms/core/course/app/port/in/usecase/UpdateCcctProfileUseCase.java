package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.port.in.command.UpdateCcctProfileCommand;

public interface UpdateCcctProfileUseCase {
    CcctProfile update(UpdateCcctProfileCommand command);
}
