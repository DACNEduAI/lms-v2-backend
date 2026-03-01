package edu.ai.lms.core.quiz.app.port.out;

import java.util.UUID;

public interface DeleteAnswerPort {
    void deleteByKey(UUID questionId, Long answerNumber);
}
