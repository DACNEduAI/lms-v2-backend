package edu.ai.lms.core.quiz.app.port.out;

import edu.ai.lms.core.quiz.app.domain.Question;

public interface SaveQuestionPort {
    Question save(Question question);
}
