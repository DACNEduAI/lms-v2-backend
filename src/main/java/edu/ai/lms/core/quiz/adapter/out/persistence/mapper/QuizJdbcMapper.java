package edu.ai.lms.core.quiz.adapter.out.persistence.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.app.domain.Question;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.database.QuizJdbcTable;

@Component
public class QuizJdbcMapper {

    public QuizJdbcTable mapToQuizJdbcEntity(Quiz quiz) {
        return QuizJdbcTable.builder()
                .id(quiz.getId())
                .quizTitle(quiz.getQuizTitle())
                .timeLimitMinutes(quiz.getTimeLimitMinutes())
                .passScore(quiz.getPassScore())
                .courseId(quiz.getCourseId())
                .studentId(quiz.getStudentId())
                .teacherId(quiz.getTeacherId())
                .active(quiz.getActive())
                .build();
    }

    public Quiz mapToQuiz(QuizJdbcTable entity) {
        return Quiz.builder()
                .id(entity.getId())
                .quizTitle(entity.getQuizTitle())
                .timeLimitMinutes(entity.getTimeLimitMinutes())
                .passScore(entity.getPassScore())
                .courseId(entity.getCourseId())
                .studentId(entity.getStudentId())
                .teacherId(entity.getTeacherId())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public Quiz mapToQuizWithQuestionsAndAnswers(QuizJdbcTable entity, List<Question> questions) {
        Quiz quiz = mapToQuiz(entity);
        quiz.setQuestions(questions);
        return quiz;
    }
}
