package edu.ai.lms.core.attempt.app.port.in.usecase;

import edu.ai.lms.core.attempt.adapter.in.response.SubmitAttemptResponse;
import edu.ai.lms.core.attempt.app.port.in.command.SubmitAttemptCommand;

public interface SubmitAttemptUseCase {

    SubmitAttemptResponse submitAttempt(SubmitAttemptCommand command);
}
