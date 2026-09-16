package edu.ai.lms.core.attempt.adapter.in.api;

import edu.ai.lms.core.attempt.api.LearnerHistoryApi;
import edu.ai.lms.core.attempt.api.LearnerInteractionView;
import edu.ai.lms.core.attempt.api.LearnerQuizHistoryView;
import edu.ai.lms.core.attempt.app.domain.AttemptQuestionResult;
import edu.ai.lms.core.attempt.app.port.out.AttemptQuestionResultPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class AttemptLearnerHistoryApiAdapter implements LearnerHistoryApi {
    private final AttemptQuestionResultPort resultPort;

    @Override
    public List<LearnerQuizHistoryView> findFinishedHistory(UUID studentId, Collection<UUID> quizIds) {
        if (quizIds == null || quizIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<AttemptQuestionResult>> byAttempt = new LinkedHashMap<>();
        resultPort.findFinishedHistory(studentId, quizIds)
                .forEach(result -> byAttempt.computeIfAbsent(result.getAttemptId(), ignored -> new ArrayList<>())
                        .add(result));
        return byAttempt.values().stream()
                .map(results -> {
                    AttemptQuestionResult first = results.getFirst();
                    return new LearnerQuizHistoryView(
                            first.getQuizId(),
                            first.getAttemptId(),
                            first.getSubmittedTime(),
                            results.stream().map(this::toView).toList());
                })
                .toList();
    }

    private LearnerInteractionView toView(AttemptQuestionResult result) {
        return new LearnerInteractionView(
                result.getQuestionId(),
                result.getKtQuestionId(),
                Boolean.TRUE.equals(result.getCorrect()),
                result.getLearningOutcomeId(),
                result.getKtLearningOutcomeId(),
                result.getLearningOutcomeMappingCount());
    }
}
