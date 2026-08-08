package edu.ai.lms.core.quiz.adapter.in.http.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.adapter.in.http.request.CreateQuizRequest;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.port.in.command.CreateQuizCommand;

@Component
public class CreateQuizCommandMapper {

    public CreateQuizCommand toCommand(UUID courseId, CreateQuizRequest request) {
        Quiz quiz = Quiz.builder()
                .quizTitle(request.getQuizTitle())
                .description(request.getDescription())
            .unlimited(request.getUnlimited())
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .passScore(request.getPassScore())
                .startTime(request.getStartTime())
                .dueTime(request.getDueTime())
                .attemptLimited(request.getAttemptLimited())
                .display(request.getDisplay())
                .build();

        return CreateQuizCommand.builder()
                .courseId(courseId)
                .quiz(quiz)
                .learningOutcomeIds(request.getLearningOutcomeIds())
                .build();
    }
}
