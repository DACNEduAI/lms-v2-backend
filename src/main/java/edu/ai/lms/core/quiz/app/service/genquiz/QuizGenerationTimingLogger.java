package edu.ai.lms.core.quiz.app.service.genquiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;
import edu.ai.lms.core.quiz.app.port.out.QuestionVisualJobPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/** Emits content-free, end-to-end timing logs for quiz generation. */
@Component
@RequiredArgsConstructor
@Slf4j
public class QuizGenerationTimingLogger {
    private final ObjectMapper objectMapper;

    public void textStarted(QuizGenerationJob job) {
        Counts counts = counts(job);
        log.info("quiz_generation.text.started job_id={} question_count={} visual_count={} attempt_count={}",
                job.getId(), counts.questions(), counts.visuals(), value(job.getAttemptCount()));
    }

    public void textTerminal(QuizGenerationJob job, String outcome, String errorCode, LocalDateTime endedAt) {
        Counts counts = counts(job);
        long durationMs = durationMillis("quiz_generation.text." + outcome, job.getCreatedAt(), endedAt);
        if ("completed".equals(outcome)) {
            log.info("quiz_generation.text.completed job_id={} outcome=completed duration_ms={} "
                            + "question_count={} visual_count={} attempt_count={}",
                    job.getId(), durationMs, counts.questions(), counts.visuals(), value(job.getAttemptCount()));
        } else {
            log.warn("quiz_generation.text.failed job_id={} outcome={} duration_ms={} question_count={} "
                            + "visual_count={} attempt_count={} error_code={}",
                    job.getId(), outcome, durationMs, counts.questions(), counts.visuals(),
                    value(job.getAttemptCount()), safe(errorCode));
        }
    }

    public void visualCompleted(QuestionVisualJobPort.WorkItem work, String actualType, LocalDateTime endedAt) {
        log.info("quiz_generation.visual.completed job_id={} quiz_id={} question_id={} visual_job_id={} "
                        + "image_id={} requested_type={} actual_type={} outcome=completed attempts={} duration_ms={}",
                work.parentJobId(), work.quizId(), work.questionId(), work.id(), work.imageId(),
                safe(work.visualType()), safe(actualType), work.attemptCount() + 1,
                durationMillis("quiz_generation.visual.completed", work.createdAt(), endedAt));
    }

    public void visualFailed(QuestionVisualJobPort.WorkItem work, String errorCode, LocalDateTime endedAt) {
        visualFailed(work.parentJobId(), work.quizId(), work.questionId(), work.id(), work.imageId(),
                work.visualType(), work.attemptCount() + 1, work.createdAt(), errorCode, endedAt);
    }

    public void visualExpired(QuestionVisualJobPort.ExpiredItem item, UUID parentJobId,
                              String errorCode, LocalDateTime endedAt) {
        visualFailed(parentJobId, item.quizId(), item.questionId(), item.jobId(), item.imageId(),
                item.visualType(), item.attemptCount(), item.createdAt(), errorCode, endedAt);
    }

    public void visualRetry(QuestionVisualJobPort.WorkItem work, String errorCode, int attempted) {
        log.warn("quiz_generation.visual.retry job_id={} quiz_id={} question_id={} visual_job_id={} "
                        + "image_id={} requested_type={} attempt_count={} error_code={}",
                work.parentJobId(), work.quizId(), work.questionId(), work.id(), work.imageId(),
                safe(work.visualType()), attempted, safe(errorCode));
    }

    public void visualPhaseTerminal(QuizGenerationJob parent, QuestionVisualJobPort.Summary summary,
                                    LocalDateTime endedAt) {
        long durationMs = durationMillis(
                "quiz_generation.visual_phase.completed", summary.startedAt(), endedAt);
        if (summary.failed() == 0) {
            log.info("quiz_generation.visual_phase.completed job_id={} quiz_id={} outcome=completed "
                            + "duration_ms={} requested={} ready={} failed={}",
                    parent.getId(), parent.getQuizId(), durationMs, summary.requested(),
                    summary.ready(), summary.failed());
        } else {
            log.warn("quiz_generation.visual_phase.completed job_id={} quiz_id={} outcome=partially_failed "
                            + "duration_ms={} requested={} ready={} failed={}",
                    parent.getId(), parent.getQuizId(), durationMs, summary.requested(),
                    summary.ready(), summary.failed());
        }
    }

    public void quizTerminal(QuizGenerationJob job, QuestionVisualJobPort.Summary summary,
                             LocalDateTime endedAt) {
        Counts counts = counts(job);
        int requested = summary == null ? counts.visuals() : summary.requested();
        int ready = summary == null ? 0 : summary.ready();
        int failed = summary == null ? 0 : summary.failed();
        long durationMs = durationMillis("quiz_generation.completed", job.getCreatedAt(), endedAt);
        String status = job.getStatus() == null ? "UNKNOWN" : job.getStatus().name();
        String phase = job.getPhase() == null ? "UNKNOWN" : job.getPhase().name();
        String message = "quiz_generation.completed job_id={} quiz_id={} status={} phase={} duration_ms={} "
                + "attempt_count={} question_count={} visuals_requested={} visuals_ready={} visuals_failed={} "
                + "error_code={}";
        Object[] arguments = {job.getId(), job.getQuizId(), status, phase, durationMs,
                value(job.getAttemptCount()), counts.questions(), requested, ready, failed,
                safe(job.getErrorCode())};
        if ("COMPLETED".equals(status) && failed == 0 && job.getErrorCode() == null) {
            log.info(message, arguments);
        } else {
            log.warn(message, arguments);
        }
    }

    long durationMillis(String event, LocalDateTime startedAt, LocalDateTime endedAt) {
        if (startedAt == null || endedAt == null) {
            log.warn("quiz_generation.timing.missing event={} start_missing={} end_missing={}",
                    event, startedAt == null, endedAt == null);
            return 0;
        }
        long duration = Duration.between(startedAt, endedAt).toMillis();
        if (duration < 0) {
            log.warn("quiz_generation.timing.negative event={} duration_ms={}", event, duration);
            return 0;
        }
        return duration;
    }

    private void visualFailed(UUID parentJobId, UUID quizId, UUID questionId, UUID visualJobId,
                              UUID imageId, String requestedType, int attempts,
                              LocalDateTime startedAt, String errorCode, LocalDateTime endedAt) {
        log.warn("quiz_generation.visual.failed job_id={} quiz_id={} question_id={} visual_job_id={} "
                        + "image_id={} requested_type={} actual_type=none outcome=failed attempts={} "
                        + "duration_ms={} error_code={}",
                parentJobId, quizId, questionId, visualJobId, imageId, safe(requestedType), attempts,
                durationMillis("quiz_generation.visual.failed", startedAt, endedAt), safe(errorCode));
    }

    private Counts counts(QuizGenerationJob job) {
        if (job.getRequestSnapshot() == null || job.getRequestSnapshot().isBlank()) {
            return new Counts(-1, -1);
        }
        try {
            JsonNode snapshot = objectMapper.readTree(job.getRequestSnapshot());
            int questions = snapshot.path("questionCount").asInt(-1);
            int visuals = 0;
            for (JsonNode row : snapshot.path("examMatrix")) {
                if (row.has("levelCounts")) {
                    for (JsonNode level : row.path("levelCounts")) {
                        visuals += level.path("imageQuestionCount").asInt(0);
                    }
                } else {
                    visuals += row.path("imageQuestionCount").asInt(0);
                }
            }
            return new Counts(questions, visuals);
        } catch (Exception ex) {
            log.warn("quiz_generation.timing.snapshot_unreadable job_id={}", job.getId());
            return new Counts(-1, -1);
        }
    }

    private static int value(Integer value) {
        return value == null ? 0 : value;
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }

    private record Counts(int questions, int visuals) {}
}
