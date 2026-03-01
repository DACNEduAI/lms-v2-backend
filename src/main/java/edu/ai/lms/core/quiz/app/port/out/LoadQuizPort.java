package edu.ai.lms.core.quiz.app.port.out;

import edu.ai.lms.core.quiz.app.domain.Quiz;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadQuizPort {
    Optional<Quiz> findById(UUID quizId);
    List<Quiz> findAllByCourseId(UUID courseId);
    List<Quiz> findByCourseIdAndTeacherId(UUID courseId, UUID teacherId);
}
