package edu.ai.lms.core.attempt.app.port.in.command;

import lombok.Builder;

import java.util.UUID;

@Builder
public record StartAttemptCommand(UUID courseId, UUID quizId) {
}
