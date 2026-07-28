package edu.ai.lms.core.quiz.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttemptPolicyApi {

    Optional<AttemptPolicyView> findPolicy(UUID quizId);

    List<QuestionAnswerKeyView> findAnswerKeys(UUID quizId);
}
