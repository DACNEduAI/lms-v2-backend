package edu.ai.lms.core.quiz.app.port.out;

import edu.ai.lms.core.quiz.app.domain.Answer;

public interface SaveAnswerPort {
    Answer save(Answer answer);
}
