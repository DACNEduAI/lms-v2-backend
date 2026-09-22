package edu.ai.lms.core.quiz3.app.port.out;

import edu.ai.lms.core.quiz3.app.domain.Answer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadAnswerPort {
    Optional<Answer> getByKey(UUID questionId, Long answerNumber);
    List<Answer> getByQuestionId(UUID questionId);
}
