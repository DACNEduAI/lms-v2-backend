package edu.ai.lms.core.quiz3.app.port.out;

import edu.ai.lms.core.quiz3.app.domain.Quiz;

public interface SaveQuizPort {
    Quiz save(Quiz quiz);
}
