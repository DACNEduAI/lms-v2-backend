package edu.ai.lms.core.quiz3.adapter.in.http.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz3.adapter.in.http.request.ModifyAnswerRequest;
import edu.ai.lms.core.quiz3.app.domain.Answer;
import edu.ai.lms.core.quiz3.app.port.in.command.ModifyAnswerCommand;

@Component
public class ModifyAnswerCommandMapper {

    public ModifyAnswerCommand toCommand(UUID questionId, Long answerNumber, ModifyAnswerRequest request) {
        Answer answer = Answer.builder()
                .content(request.getContent())
                .isCorrect(request.isCorrect())
                .explanation(request.getExplanation())
                .build();

        return ModifyAnswerCommand.builder()
                .questionId(questionId)
                .answerNumber(answerNumber)
                .answer(answer)
                .build();
    }
}
