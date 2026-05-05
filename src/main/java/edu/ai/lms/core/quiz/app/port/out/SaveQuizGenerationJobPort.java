package edu.ai.lms.core.quiz.app.port.out;

import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;

public interface SaveQuizGenerationJobPort {
    QuizGenerationJob save(QuizGenerationJob job);
}
