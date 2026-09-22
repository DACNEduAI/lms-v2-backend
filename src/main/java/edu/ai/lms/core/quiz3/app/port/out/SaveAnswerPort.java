package edu.ai.lms.core.quiz3.app.port.out;

import edu.ai.lms.core.quiz3.app.domain.Answer;

public interface SaveAnswerPort {
    Answer save(Answer answer);
}
