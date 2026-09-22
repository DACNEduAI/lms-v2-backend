package edu.ai.lms.core.quiz3.app.port.out;

import edu.ai.lms.core.quiz3.app.domain.Question;

public interface SaveQuestionPort {
    Question save(Question question);
}
