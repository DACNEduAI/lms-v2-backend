package edu.ai.lms.core.quiz.adapter.out.persistence;

import edu.ai.lms.core.quiz.app.port.out.QuestionVisualJobPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class QuestionVisualJobPersistenceAdapter implements QuestionVisualJobPort {
    private final JdbcTemplate jdbc;

    @Override
    public void create(UUID parentJobId, UUID imageId, String type) {
        jdbc.update("""
                INSERT INTO edu.question_image_generation_jobs
                  (id, quiz_generation_job_id, question_image_id, visual_type, status, next_attempt_at)
                VALUES (?, ?, ?, ?, 'PENDING', NOW())
                """, UUID.randomUUID(), parentJobId, imageId, type);
    }

    @Override
    @Transactional
    public List<WorkItem> claimAvailable(int limit, Instant leaseUntil) {
        List<WorkItem> work = jdbc.query("""
                SELECT j.id, j.quiz_generation_job_id, j.question_image_id, q.quiz_id, q.id AS question_id,
                       j.visual_type, q.content, i.generation_brief, i.alt_text,
                       j.attempt_count, j.created_at, p.created_at AS parent_created_at
                FROM edu.question_image_generation_jobs j
                JOIN edu.question_images i ON i.id=j.question_image_id
                JOIN edu.questions q ON q.id=i.question_id
                JOIN edu.quiz_generation_jobs p ON p.id=j.quiz_generation_job_id
                WHERE ((j.status='PENDING' AND (j.next_attempt_at IS NULL OR j.next_attempt_at <= NOW()))
                    OR (j.status='PROCESSING' AND j.lease_until < NOW()))
                  AND p.status='PROCESSING' AND p.phase='VISUAL_GENERATION'
                ORDER BY j.created_at FOR UPDATE OF j SKIP LOCKED LIMIT ?
                """, (rs, row) -> new WorkItem(
                rs.getObject("id", UUID.class), rs.getObject("quiz_generation_job_id", UUID.class),
                rs.getObject("question_image_id", UUID.class), rs.getObject("quiz_id", UUID.class),
                rs.getObject("question_id", UUID.class),
                rs.getString("visual_type"), rs.getString("content"), rs.getString("generation_brief"),
                rs.getString("alt_text"), rs.getInt("attempt_count"),
                instant(rs, "created_at"), instant(rs, "parent_created_at")), limit);
        for (WorkItem item : work) {
            jdbc.update("""
                    UPDATE edu.question_image_generation_jobs SET status='PROCESSING',
                      attempt_count=attempt_count+1, processing_at=COALESCE(processing_at,NOW()),
                      lease_until=?, error_code=NULL, error_message=NULL, updated_at=NOW() WHERE id=?
                    """, asOffsetDateTime(leaseUntil), item.id());
            jdbc.update("UPDATE edu.question_images SET status='PROCESSING', updated_at=NOW() WHERE id=?", item.imageId());
        }
        return work;
    }

    @Override
    @Transactional
    public boolean completeVisual(UUID jobId, UUID imageId, String actualType, String source,
                                  String storageKey, String url, String hash, long byteSize) {
        if (!complete(jobId)) return false;
        jdbc.update("""
                UPDATE edu.question_images SET source_type=?, source_text=?, storage_key=?, url=?, status='READY',
                  media_type='image/svg+xml', content_hash=?, byte_size=?, error_code=NULL,
                  error_message=NULL, updated_at=NOW() WHERE id=?
                """, actualType, source, storageKey, url, hash, byteSize, imageId);
        return true;
    }

    private boolean complete(UUID jobId) {
        return jdbc.update("""
                UPDATE edu.question_image_generation_jobs SET status='COMPLETED', lease_until=NULL,
                  completed_at=NOW(), error_code=NULL, error_message=NULL, updated_at=NOW() WHERE id=?
                  AND status='PROCESSING'
                """, jobId) == 1;
    }

    @Override
    @Transactional
    public boolean retry(UUID jobId, String code, String message, Instant nextAttemptAt) {
        int updated = jdbc.update("""
                UPDATE edu.question_image_generation_jobs SET status='PENDING', lease_until=NULL,
                  next_attempt_at=?, error_code=?, error_message=?, updated_at=NOW() WHERE id=?
                  AND status='PROCESSING'
                """, asOffsetDateTime(nextAttemptAt), code, message, jobId);
        if (updated == 0) return false;
        jdbc.update("""
                UPDATE edu.question_images i SET status='PENDING', error_code=?, error_message=?, updated_at=NOW()
                FROM edu.question_image_generation_jobs j WHERE j.id=? AND i.id=j.question_image_id
                """, code, message, jobId);
        return true;
    }

    @Override
    @Transactional
    public boolean fail(UUID jobId, UUID imageId, String code, String message) {
        int updated = jdbc.update("""
                UPDATE edu.question_image_generation_jobs SET status='FAILED', lease_until=NULL,
                  error_code=?, error_message=?, completed_at=NOW(), updated_at=NOW() WHERE id=?
                  AND status IN ('PENDING','PROCESSING')
                """, code, message, jobId);
        if (updated == 0) return false;
        jdbc.update("""
                UPDATE edu.question_images SET status='FAILED', error_code=?, error_message=?, updated_at=NOW() WHERE id=?
                """, code, message, imageId);
        return true;
    }

    @Override
    @Transactional
    public Optional<RetryItem> retryFailed(UUID quizId, UUID questionId, UUID requestedBy) {
        List<RetryItem> items = jdbc.query("""
                SELECT j.id, j.quiz_generation_job_id, p.created_at AS parent_created_at
                FROM edu.question_image_generation_jobs j
                JOIN edu.question_images i ON i.id=j.question_image_id
                JOIN edu.questions q ON q.id=i.question_id
                JOIN edu.quiz_generation_jobs p ON p.id=j.quiz_generation_job_id
                WHERE q.quiz_id=? AND q.id=? AND p.requested_by=? AND j.status='FAILED'
                ORDER BY j.created_at DESC LIMIT 1 FOR UPDATE OF j
                """, (rs, row) -> new RetryItem(rs.getObject("id", UUID.class),
                rs.getObject("quiz_generation_job_id", UUID.class),
                instant(rs, "parent_created_at")), quizId, questionId, requestedBy);
        if (items.isEmpty()) return Optional.empty();
        RetryItem item = items.getFirst();
        UUID id = item.jobId();
        jdbc.update("""
                UPDATE edu.question_image_generation_jobs SET status='PENDING', attempt_count=0,
                  next_attempt_at=NOW(), lease_until=NULL, error_code=NULL, error_message=NULL,
                  processing_at=NULL, completed_at=NULL, created_at=NOW(), updated_at=NOW() WHERE id=?
                """, id);
        jdbc.update("""
                UPDATE edu.question_images i SET status='PENDING', source_text=NULL, storage_key=NULL,
                  url=NULL, error_code=NULL, error_message=NULL, updated_at=NOW()
                FROM edu.question_image_generation_jobs j WHERE j.id=? AND i.id=j.question_image_id
                """, id);
        jdbc.update("""
                UPDATE edu.quiz_generation_jobs p SET status='PROCESSING', phase='VISUAL_GENERATION',
                  completed_at=NULL, error_code=NULL, error_message=NULL, updated_at=NOW()
                FROM edu.question_image_generation_jobs j WHERE j.id=? AND p.id=j.quiz_generation_job_id
                """, id);
        return Optional.of(item);
    }

    @Override
    public List<RecoveryItem> findActiveForScheduling() {
        return jdbc.query("""
                SELECT j.id, j.quiz_generation_job_id, j.status, j.next_attempt_at, j.lease_until,
                       p.created_at AS parent_created_at
                FROM edu.question_image_generation_jobs j
                JOIN edu.quiz_generation_jobs p ON p.id=j.quiz_generation_job_id
                WHERE p.status='PROCESSING' AND p.phase='VISUAL_GENERATION'
                  AND j.status IN ('PENDING','PROCESSING')
                ORDER BY j.created_at
                """, (rs, row) -> new RecoveryItem(
                rs.getObject("id", UUID.class), rs.getObject("quiz_generation_job_id", UUID.class),
                rs.getString("status"), instant(rs, "next_attempt_at"), instant(rs, "lease_until"),
                instant(rs, "parent_created_at")));
    }

    @Override
    @Transactional
    public DeadlineResult expireParent(UUID parentJobId, String code, String message) {
        List<UUID> parent = jdbc.query("""
                SELECT id FROM edu.quiz_generation_jobs
                WHERE id=? AND status='PROCESSING' AND phase='VISUAL_GENERATION'
                FOR UPDATE
                """, (rs, row) -> rs.getObject(1, UUID.class), parentJobId);
        if (parent.isEmpty()) {
            return new DeadlineResult(false, List.of(), summarize(parentJobId));
        }
        List<ExpiredItem> activeItems = jdbc.query("""
                SELECT j.id, j.question_image_id, q.quiz_id, q.id AS question_id,
                       j.visual_type, j.attempt_count, j.created_at
                FROM edu.question_image_generation_jobs j
                JOIN edu.question_images i ON i.id=j.question_image_id
                JOIN edu.questions q ON q.id=i.question_id
                WHERE j.quiz_generation_job_id=? AND j.status IN ('PENDING','PROCESSING')
                FOR UPDATE OF j
                """, (rs, row) -> new ExpiredItem(
                rs.getObject("id", UUID.class), rs.getObject("question_image_id", UUID.class),
                rs.getObject("quiz_id", UUID.class), rs.getObject("question_id", UUID.class),
                rs.getString("visual_type"), rs.getInt("attempt_count"),
                instant(rs, "created_at")), parentJobId);
        jdbc.update("""
                UPDATE edu.question_images i SET status='FAILED', error_code=?, error_message=?, updated_at=NOW()
                FROM edu.question_image_generation_jobs j
                WHERE j.quiz_generation_job_id=? AND j.status IN ('PENDING','PROCESSING')
                  AND i.id=j.question_image_id
                """, code, message, parentJobId);
        jdbc.update("""
                UPDATE edu.question_image_generation_jobs SET status='FAILED', lease_until=NULL,
                  error_code=?, error_message=?, completed_at=NOW(), updated_at=NOW()
                WHERE quiz_generation_job_id=? AND status IN ('PENDING','PROCESSING')
                """, code, message, parentJobId);
        Summary summary = summarize(parentJobId);
        jdbc.update("""
                UPDATE edu.quiz_generation_jobs SET status='COMPLETED', phase='DONE', lease_until=NULL,
                  completed_at=NOW(), error_code='VISUALS_PARTIALLY_FAILED',
                  error_message=?, updated_at=NOW() WHERE id=?
                """, summary.failed() + " required visual(s) failed; the hidden quiz remains reviewable", parentJobId);
        return new DeadlineResult(true, activeItems, summary);
    }

    @Override
    public Summary summarize(UUID parentJobId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) AS requested,
                  COUNT(*) FILTER (WHERE status='COMPLETED') AS ready,
                  COUNT(*) FILTER (WHERE status='FAILED') AS failed,
                  COUNT(*) FILTER (WHERE status IN ('PENDING','PROCESSING')) AS active,
                  MIN(created_at) AS started_at
                FROM edu.question_image_generation_jobs WHERE quiz_generation_job_id=?
                """, (rs, row) -> new Summary(rs.getInt("requested"), rs.getInt("ready"),
                rs.getInt("failed"), rs.getInt("active"), instant(rs, "started_at")), parentJobId);
    }

    @Override
    public boolean hasUnreadyRequiredVisual(UUID quizId) {
        Boolean value = jdbc.queryForObject("""
                SELECT EXISTS (
                  SELECT 1 FROM edu.question_image_generation_jobs j
                  JOIN edu.question_images i ON i.id=j.question_image_id
                  JOIN edu.questions q ON q.id=i.question_id
                  WHERE q.quiz_id=? AND i.status <> 'READY'
                )
                """, Boolean.class, quizId);
        return Boolean.TRUE.equals(value);
    }

    private Instant instant(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private OffsetDateTime asOffsetDateTime(Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
