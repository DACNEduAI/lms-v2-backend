package edu.ai.lms.core.quiz3.app.port.out;

import edu.ai.lms.core.quiz3.app.domain.Question;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadQuestionPort {
    Optional<Question> getById(UUID questionId);
    Optional<Question> getByIdWithoutAnswers(UUID questionId);
    List<Question> getByQuizId(UUID quizId);
}
