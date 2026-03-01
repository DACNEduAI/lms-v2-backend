package edu.ai.lms.core.quiz.app.port.out;

import java.util.UUID;

public interface DeleteQuizPort {
    void deleteById(UUID quizId);
}
