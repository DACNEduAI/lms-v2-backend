package edu.ai.lms.core.attempt.app.port.out;

import edu.ai.lms.core.attempt.app.domain.AttemptQuestionResult;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AttemptQuestionResultPort {
    void upsertAll(List<AttemptQuestionResult> results);

    void updateCorrectness(UUID attemptId, UUID questionId, boolean correct);

    List<AttemptQuestionResult> findFinishedHistory(UUID studentId, Collection<UUID> quizIds);

    List<AttemptQuestionResult> findAttemptRecords(UUID studentId, UUID quizId, Collection<UUID> attemptIds);
}
