package edu.ai.lms.core.quiz.app.port.out;

import edu.ai.lms.core.quiz.app.domain.Question;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadQuestionPort {
    Optional<Question> findById(UUID questionId);
    Optional<Question> findByIdWithoutAnswers(UUID questionId);
    List<Question> findByQuizId(UUID quizId);
}
