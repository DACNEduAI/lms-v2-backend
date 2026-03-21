package edu.ai.lms.core.attempt.app.port.in.usecase;

import edu.ai.lms.core.attempt.adapter.in.response.StartAttemptResponse;
import edu.ai.lms.core.attempt.app.port.in.command.StartAttemptCommand;

public interface StartAttemptUseCase {

    StartAttemptResponse startAttempt(StartAttemptCommand command);
}
