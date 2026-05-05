package edu.ai.lms.core.quiz.app.port.in.command;

import java.util.UUID;

import lombok.Builder;

@Builder
public record GenerateQuizCommand(
        UUID courseId,
        UUID chapterId,
        String quizTitle,
        String difficulty,
        Integer questionCount
) {
}
