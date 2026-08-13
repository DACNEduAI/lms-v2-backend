package edu.ai.lms.core.quiz.app.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionVisualJobPort {
    void create(UUID parentJobId, UUID questionImageId, String visualType);
    List<WorkItem> claimAvailable(int limit, Instant leaseUntil);
    boolean completeVisual(UUID jobId, UUID imageId, String actualType, String source,
                           String storageKey, String url, String hash, long byteSize);
    boolean retry(UUID jobId, String code, String message, Instant nextAttemptAt);
    boolean fail(UUID jobId, UUID imageId, String code, String message);
    Optional<RetryItem> retryFailed(UUID quizId, UUID questionId, UUID requestedBy);
    List<RecoveryItem> findActiveForScheduling();
    DeadlineResult expireParent(UUID parentJobId, String code, String message);
    Summary summarize(UUID parentJobId);
    boolean hasUnreadyRequiredVisual(UUID quizId);

    record WorkItem(UUID id, UUID parentJobId, UUID imageId, UUID quizId, UUID questionId, String visualType,
                    String questionContent, String generationBrief, String altText,
                    int attemptCount, Instant createdAt, Instant parentCreatedAt) {}
    record RetryItem(UUID jobId, UUID parentJobId, Instant parentCreatedAt) {}
    record RecoveryItem(UUID jobId, UUID parentJobId, String status, Instant nextAttemptAt,
                        Instant leaseUntil, Instant parentCreatedAt) {}
    record ExpiredItem(UUID jobId, UUID imageId, UUID quizId, UUID questionId, String visualType,
                       int attemptCount, Instant createdAt) {}
    record DeadlineResult(boolean applied, List<ExpiredItem> expiredItems, Summary summary) {}
    record Summary(int requested, int ready, int failed, int active, Instant startedAt) {}
}
