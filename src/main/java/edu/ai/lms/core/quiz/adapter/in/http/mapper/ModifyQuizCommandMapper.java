package edu.ai.lms.core.quiz.adapter.in.http.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.adapter.in.http.request.ModifyQuizRequest;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.port.in.command.ModifyQuizCommand;

@Component
public class ModifyQuizCommandMapper {

    public ModifyQuizCommand toCommand(UUID courseId, UUID quizId, ModifyQuizRequest request) {
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

        return ModifyQuizCommand.builder()
                .courseId(courseId)
                .quizId(quizId)
                .quiz(quiz)
                .clearAttemptLimit(Boolean.TRUE.equals(request.getAttemptUnlimited()))
                .build();
    }
}
