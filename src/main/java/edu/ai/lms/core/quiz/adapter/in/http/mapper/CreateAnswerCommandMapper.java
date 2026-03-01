package edu.ai.lms.core.quiz.adapter.in.http.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.adapter.in.http.request.CreateAnswerRequest;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.port.in.command.CreateAnswerCommand;

@Component
public class CreateAnswerCommandMapper {

    public CreateAnswerCommand toCommand(UUID questionId, CreateAnswerRequest request) {
        Answer answer = Answer.builder()
                .content(request.getContent())
                .isCorrect(request.isCorrect())
                .explanation(request.getExplanation())
                .build();

        return CreateAnswerCommand.builder()
                .questionId(questionId)
                .answer(answer)
                .build();
    }
}
