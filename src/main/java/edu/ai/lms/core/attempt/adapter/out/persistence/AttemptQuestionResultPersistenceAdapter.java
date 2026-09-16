package edu.ai.lms.core.attempt.adapter.out.persistence;

import edu.ai.lms.core.attempt.app.domain.AttemptQuestionResult;
import edu.ai.lms.core.attempt.app.port.out.AttemptQuestionResultPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class AttemptQuestionResultPersistenceAdapter implements AttemptQuestionResultPort {
    private final AttemptQuestionResultRepository repository;

    @Override
    public void upsertAll(List<AttemptQuestionResult> results) {
        if (results != null && !results.isEmpty()) {
            repository.upsertAll(results);
        }
    }

    @Override
    public void updateCorrectness(UUID attemptId, UUID questionId, boolean correct) {
        repository.updateCorrectness(attemptId, questionId, correct);
    }

    @Override
    public List<AttemptQuestionResult> findFinishedHistory(UUID studentId, Collection<UUID> quizIds) {
        if (quizIds == null || quizIds.isEmpty()) {
            return List.of();
        }
        return repository.findFinishedHistory(studentId, quizIds);
    }

    @Override
    public List<AttemptQuestionResult> findAttemptRecords(
            UUID studentId, UUID quizId, Collection<UUID> attemptIds) {
        if (attemptIds == null || attemptIds.isEmpty()) {
            return List.of();
        }
        return repository.findAttemptRecords(studentId, quizId, attemptIds);
    }
}
