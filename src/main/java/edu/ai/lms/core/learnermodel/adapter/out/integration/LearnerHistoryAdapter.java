package edu.ai.lms.core.learnermodel.adapter.out.integration;

import edu.ai.lms.core.attempt.api.LearnerHistoryApi;
import edu.ai.lms.core.attempt.api.LearnerQuizHistoryView;
import edu.ai.lms.core.learnermodel.app.domain.LearnerInteraction;
import edu.ai.lms.core.learnermodel.app.domain.LearnerQuizHistory;
import edu.ai.lms.core.learnermodel.app.port.out.LoadLearnerHistoryPort;
import edu.ai.lms.core.quiz.api.LearnerModelQuizApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
class LearnerHistoryAdapter implements LoadLearnerHistoryPort {
    private final LearnerModelQuizApi quizApi;
    private final LearnerHistoryApi historyApi;
    private final ZoneId historyZone;

    LearnerHistoryAdapter(
            LearnerModelQuizApi quizApi,
            LearnerHistoryApi historyApi,
            @Value("${learner-model.history-zone-id:Asia/Ho_Chi_Minh}") String historyZoneId) {
        this.quizApi = quizApi;
        this.historyApi = historyApi;
        this.historyZone = ZoneId.of(historyZoneId);
    }

    @Override
    public List<LearnerQuizHistory> load(UUID studentId, UUID courseId) {
        var histories = historyApi.findFinishedHistory(
                studentId, quizApi.findQuizIdsByCourseId(courseId)).stream()
                .sorted(Comparator.comparing(LearnerQuizHistoryView::submittedAt)
                        .thenComparing(LearnerQuizHistoryView::attemptId))
                .toList();
        Set<OffsetDateTime> usedTimestamps = new HashSet<>();
        return histories.stream().map(history -> {
            OffsetDateTime submittedAt = history.submittedAt()
                    .atZone(historyZone).toOffsetDateTime();
            while (!usedTimestamps.add(submittedAt)) {
                submittedAt = submittedAt.plusNanos(1);
            }
            return new LearnerQuizHistory(
                    history.quizId(),
                    history.attemptId(),
                    submittedAt,
                    history.interactions().stream()
                            .map(interaction -> new LearnerInteraction(
                                    interaction.questionId(),
                                    interaction.ktQuestionId(),
                                    interaction.correct(),
                                    interaction.learningOutcomeId(),
                                    interaction.ktLearningOutcomeId(),
                                    interaction.learningOutcomeMappingCount()))
                            .toList());
        }).toList();
    }
}
