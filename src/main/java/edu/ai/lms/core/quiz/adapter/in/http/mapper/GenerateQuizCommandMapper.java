package edu.ai.lms.core.quiz.adapter.in.http.mapper;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.adapter.in.http.request.GenerateQuizRequest;
import edu.ai.lms.core.quiz.app.port.in.command.GenerateQuizCommand;

@Component
public class GenerateQuizCommandMapper {

    public GenerateQuizCommand toCommand(GenerateQuizRequest request, String idempotencyKey) {
        return GenerateQuizCommand.builder()
                .courseId(request.getCourseId())
                .examMatrixId(request.getExamMatrixId())
                .visualRequirements(request.getVisualRequirements() == null ? java.util.List.of()
                        : request.getVisualRequirements().stream()
                                .map(item -> new GenerateQuizCommand.VisualRequirement(
                                        item.getExamMatrixRowId(), item.getCustomLearningLevelId(),
                                        item.getImageQuestionCount()))
                                .toList())
                .questionTypeOverrides(request.getQuestionTypeOverrides() == null ? java.util.List.of()
                        : request.getQuestionTypeOverrides().stream()
                                .map(item -> new GenerateQuizCommand.QuestionTypeOverride(
                                        item.getExamMatrixRowId(), item.getCustomLearningLevelId(),
                                        item.getQuestionType()))
                                .toList())
                .selectedMaterialIds(request.getSelectedMaterialIds())
                .prompt(request.getPrompt())
                .strictMode(request.getStrictMode())
                .codeRunnerPolicy(request.getCodeRunnerPolicy())
                .idempotencyKey(idempotencyKey)
                .quizTitle(request.getQuizTitle())
                .unlimited(request.getUnlimited())
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .passScore(request.getPassScore())
                .startTime(request.getStartTime())
                .dueTime(request.getDueTime())
                .attemptLimited(request.getAttemptLimited())
                .display(request.getDisplay())
                .build();
    }
}
