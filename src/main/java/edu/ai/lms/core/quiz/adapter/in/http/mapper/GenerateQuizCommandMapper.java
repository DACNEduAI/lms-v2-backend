package edu.ai.lms.core.quiz.adapter.in.http.mapper;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.adapter.in.http.request.GenerateQuizRequest;
import edu.ai.lms.core.quiz.app.port.in.command.GenerateQuizCommand;

@Component
public class GenerateQuizCommandMapper {

    public GenerateQuizCommand toCommand(GenerateQuizRequest request) {
        return GenerateQuizCommand.builder()
                .courseId(request.getCourseId())
                .chapterId(request.getChapterId())
                .useOptions(request.getUseOptions())
                .prompt(request.getPrompt())
                .quizTitle(request.getQuizTitle())
                .unlimited(request.getUnlimited())
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .passScore(request.getPassScore())
                .startTime(request.getStartTime())
                .dueTime(request.getDueTime())
                .attemptLimited(request.getAttemptLimited())
                .display(request.getDisplay())
                .learningOutcomeIds(request.getLearningOutcomeIds())
                .questionCount(request.getQuestionCount())
                .build();
    }
}
