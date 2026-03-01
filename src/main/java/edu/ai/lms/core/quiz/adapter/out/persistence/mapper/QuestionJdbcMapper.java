package edu.ai.lms.core.quiz.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.database.QuestionJdbcTable;

@Component
public class QuestionJdbcMapper {

    public QuestionJdbcTable mapToQuestionJdbcEntity(Question question) {
        return QuestionJdbcTable.builder()
                .id(question.getId())
                .content(question.getContent())
                .type(question.getType() != null
                        ? QuestionJdbcTable.QuestionType.valueOf(question.getType().name())
                        : null)
                .points(question.getPoints())
                .quizId(question.getQuizId())
                .build();
    }

    public Question mapToQuestionWithoutAnswers(QuestionJdbcTable entity) {
        return Question.builder()
                .id(entity.getId())
                .content(entity.getContent())
                .type(entity.getType() != null
                        ? Question.QuestionType.valueOf(entity.getType().name())
                        : null)
                .points(entity.getPoints())
                .quizId(entity.getQuizId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
