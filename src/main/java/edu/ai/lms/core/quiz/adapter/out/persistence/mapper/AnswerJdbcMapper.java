package edu.ai.lms.core.quiz.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.app.domain.Answer;
import edu.ai.lms.database.AnswerJdbcTable;

@Component
public class AnswerJdbcMapper {

    public AnswerJdbcTable mapToAnswerJdbcEntity(Answer answer) {
        return AnswerJdbcTable.builder()
                .answerNumber(answer.getAnswerNumber())
                .content(answer.getContent())
                .isCorrect(answer.isCorrect())
                .explanation(answer.getExplanation())
                .questionId(answer.getQuestionId())
                .build();
    }

    public Answer mapToAnswer(AnswerJdbcTable entity) {
        return Answer.builder()
                .answerNumber(entity.getAnswerNumber())
                .content(entity.getContent())
                .isCorrect(entity.isCorrect())
                .explanation(entity.getExplanation())
                .questionId(entity.getQuestionId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
