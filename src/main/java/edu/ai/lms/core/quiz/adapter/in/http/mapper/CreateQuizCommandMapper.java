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
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .passScore(request.getPassScore())
                .build();

        return CreateQuizCommand.builder()
                .courseId(courseId)
                .quiz(quiz)
                .build();
    }
}
