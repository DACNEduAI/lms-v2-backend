package edu.ai.lms.core.quiz.app.port.out;

import edu.ai.lms.core.quiz.app.domain.Answer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadAnswerPort {
    Optional<Answer> findByKey(UUID questionId, Long answerNumber);
    List<Answer> findByQuestionId(UUID questionId);
}
