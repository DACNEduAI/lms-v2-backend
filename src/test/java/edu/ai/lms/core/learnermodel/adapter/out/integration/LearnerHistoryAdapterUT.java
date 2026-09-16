package edu.ai.lms.core.learnermodel.adapter.out.integration;

import edu.ai.lms.core.attempt.api.LearnerHistoryApi;
import edu.ai.lms.core.attempt.api.LearnerQuizHistoryView;
import edu.ai.lms.core.quiz.api.LearnerModelQuizApi;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LearnerHistoryAdapterUT {
    @Test
    void ordersAttemptsBySubmissionTimeThenAttemptIdAndMakesTimestampsUnique() {
        UUID quizId = UUID.randomUUID();
        UUID earlierAttempt = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID laterTieA = UUID.fromString("20000000-0000-0000-0000-000000000001");
        UUID laterTieB = UUID.fromString("30000000-0000-0000-0000-000000000001");
        LocalDateTime earlier = LocalDateTime.parse("2026-09-16T09:00:00");
        LocalDateTime tied = LocalDateTime.parse("2026-09-16T10:00:00");
        LearnerHistoryApi historyApi = (studentId, quizIds) -> List.of(
                history(quizId, laterTieB, tied),
                history(quizId, earlierAttempt, earlier),
                history(quizId, laterTieA, tied));
        LearnerModelQuizApi quizApi = courseId -> List.of(quizId);
        LearnerHistoryAdapter adapter = new LearnerHistoryAdapter(
                quizApi, historyApi, "Asia/Ho_Chi_Minh");

        var histories = adapter.load(UUID.randomUUID(), UUID.randomUUID());

        assertThat(histories).extracting(item -> item.attemptId())
                .containsExactly(earlierAttempt, laterTieA, laterTieB);
        assertThat(histories).extracting(item -> item.submittedAt())
                .doesNotHaveDuplicates()
                .isSorted();
    }

    private LearnerQuizHistoryView history(UUID quizId, UUID attemptId, LocalDateTime submittedAt) {
        return new LearnerQuizHistoryView(quizId, attemptId, submittedAt, List.of());
    }
}
