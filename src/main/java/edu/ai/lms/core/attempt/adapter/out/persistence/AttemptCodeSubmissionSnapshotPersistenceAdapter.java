package edu.ai.lms.core.attempt.adapter.out.persistence;

import edu.ai.lms.core.attempt.app.port.out.AttemptCodeSubmissionSnapshotPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class AttemptCodeSubmissionSnapshotPersistenceAdapter implements AttemptCodeSubmissionSnapshotPort {

    private final AttemptCodeSubmissionSnapshotRepository repository;

    @Override
    public void replace(UUID attemptId, Map<UUID, UUID> submissionIdsByQuestion) {
        repository.deleteByAttemptId(attemptId);
        submissionIdsByQuestion.forEach((questionId, submissionId) ->
                repository.insert(attemptId, questionId, submissionId));
    }

    @Override
    public Map<UUID, UUID> findByAttemptId(UUID attemptId) {
        Map<UUID, UUID> result = new LinkedHashMap<>();
        repository.findByAttemptId(attemptId)
                .forEach(row -> result.put(row.getQuestionId(), row.getSubmissionId()));
        return result;
    }
}
