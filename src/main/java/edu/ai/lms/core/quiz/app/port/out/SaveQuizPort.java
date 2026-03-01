package edu.ai.lms.core.quiz.app.port.out;

import edu.ai.lms.core.quiz.app.domain.Quiz;

public interface SaveQuizPort {
    Quiz save(Quiz quiz);
}
