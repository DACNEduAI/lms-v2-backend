package edu.ai.lms.core.quiz.adapter.in.http.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.adapter.in.http.request.ModifyQuestionRequest;
import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.port.in.command.ModifyQuestionCommand;

@Component
public class ModifyQuestionCommandMapper {

    public ModifyQuestionCommand toCommand(UUID quizId, UUID questionId, ModifyQuestionRequest request) {
        Question question = Question.builder()
                .content(request.getContent())
                .type(request.getType() != null ? Question.QuestionType.valueOf(request.getType()) : null)
                .points(request.getPoints())
                .build();

        return ModifyQuestionCommand.builder()
                .quizId(quizId)
                .questionId(questionId)
                .question(question)
                .build();
    }
}
