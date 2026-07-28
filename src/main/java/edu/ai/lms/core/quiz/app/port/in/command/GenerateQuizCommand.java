package edu.ai.lms.core.quiz.app.port.in.command;

import java.util.UUID;
import java.util.List;

import lombok.Builder;

@Builder
public record GenerateQuizCommand(
        UUID courseId,
        UUID examMatrixId,
        List<UUID> selectedMaterialIds,
        String prompt,
        Boolean strictMode,
        String codeRunnerPolicy,
        String idempotencyKey,
        String quizTitle,
        Boolean unlimited,
        Integer timeLimitMinutes,
        Double passScore,
        java.time.LocalDateTime startTime,
        java.time.LocalDateTime dueTime,
        Integer attemptLimited,
        Boolean display
) {
}
