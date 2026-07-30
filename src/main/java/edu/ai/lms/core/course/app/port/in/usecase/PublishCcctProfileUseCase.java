package edu.ai.lms.core.course.app.port.in.usecase;

import edu.ai.lms.core.course.app.domain.CcctProfile;
import edu.ai.lms.core.course.app.port.in.command.PublishCcctProfileCommand;

public interface PublishCcctProfileUseCase {
    CcctProfile publish(PublishCcctProfileCommand command);
}
