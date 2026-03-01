package edu.ai.lms.core.quiz.adapter.in.http.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.adapter.in.http.request.ModifyAnswerRequest;
import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.core.quiz.app.port.in.command.ModifyAnswerCommand;

@Component
public class ModifyAnswerCommandMapper {

    public ModifyAnswerCommand toCommand(UUID questionId, Long answerNumber, ModifyAnswerRequest request) {
        Answer answer = Answer.builder()
                .content(request.getContent())
                .isCorrect(request.getIsCorrect() != null ? request.getIsCorrect() : false)
                .explanation(request.getExplanation())
                .build();

        return ModifyAnswerCommand.builder()
                .questionId(questionId)
                .answerNumber(answerNumber)
                .answer(answer)
                .build();
    }
}
