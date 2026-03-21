package edu.ai.lms.core.attempt.app.port.in.command;

import edu.ai.lms.core.attempt.app.domain.SubmittedAnswer;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record SubmitAttemptCommand(
        UUID courseId,
        UUID quizId,
        List<SubmittedAnswer> submittedAnswers,
        Integer timeSpentSeconds
) {
}
