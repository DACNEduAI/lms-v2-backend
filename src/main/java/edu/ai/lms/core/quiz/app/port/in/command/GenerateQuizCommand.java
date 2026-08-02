package edu.ai.lms.core.quiz.app.port.in.command;

import java.util.UUID;
import java.util.List;

import lombok.Builder;

@Builder
public record GenerateQuizCommand(
        UUID courseId,
        UUID examMatrixId,
        List<VisualRequirement> visualRequirements,
        List<QuestionTypeOverride> questionTypeOverrides,
        List<UUID> selectedMaterialIds,
        String prompt,
        QuestionDefaults questionDefaults,
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
    public record VisualRequirement(UUID examMatrixRowId, UUID customLearningLevelId, int imageQuestionCount) {}
    public record QuestionTypeOverride(
            UUID examMatrixRowId,
            UUID customLearningLevelId,
            String questionType) {}
    public record QuestionDefaults(MultipleChoiceDefaults multipleChoice) {}
    public record MultipleChoiceDefaults(
            int answerOptionCount,
            boolean allowMultipleCorrectAnswers,
            boolean requireAnswerExplanation) {}
}
