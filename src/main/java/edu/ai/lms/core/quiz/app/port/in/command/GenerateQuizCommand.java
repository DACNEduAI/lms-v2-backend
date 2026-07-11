package edu.ai.lms.core.quiz.app.port.in.command;

import java.util.UUID;
import java.util.List;

import edu.ai.lms.core.quiz.app.domain.Question;

import lombok.Builder;

@Builder
public record GenerateQuizCommand(
        UUID courseId,
        UUID chapterId,
        Boolean useOptions,
        String prompt,
        String quizTitle,
        Boolean unlimited,
        Integer timeLimitMinutes,
        Double passScore,
        java.time.LocalDateTime startTime,
        java.time.LocalDateTime dueTime,
        Integer attemptLimited,
        Boolean display,
        List<UUID> learningOutcomeIds,
        Integer questionCount,
        Question.QuestionType questionType,
        UUID examMatrixId,
        Boolean strictMode,
        List<UUID> selectedMaterialIds
) {
}
