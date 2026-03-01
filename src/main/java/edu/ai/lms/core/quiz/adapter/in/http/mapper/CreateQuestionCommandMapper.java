package edu.ai.lms.core.quiz.adapter.in.http.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.adapter.in.http.request.CreateQuestionRequest;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.port.in.command.CreateQuestionCommand;

@Component
public class CreateQuestionCommandMapper {

    public CreateQuestionCommand toCommand(UUID quizId, CreateQuestionRequest request) {
        Question question = Question.builder()
                .content(request.getContent())
                .type(request.getType() != null ? Question.QuestionType.valueOf(request.getType()) : null)
                .points(request.getPoints())
                .build();

        return CreateQuestionCommand.builder()
                .quizId(quizId)
                .question(question)
                .build();
    }
}
