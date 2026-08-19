package edu.ai.lms.core.quiz3;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class Quiz3GenerationDispatcher {
    private final JdbcTemplate jdbc;
    private final WebClient aiWebClient;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    @Value("${quiz.generation.webhook.base-url}") private String webhookBaseUrl;
    @Value("${quiz.generation.webhook.secret}") private String webhookSecret;
    @Value("${quiz.generation.timeout-minutes:20}") private long timeoutMinutes;

    @Async
    public void dispatch(UUID jobId) {
        try {
            ObjectNode request = new TransactionTemplate(transactionManager).execute(status -> {
                try { return prepare(jobId); }
                catch (Exception ex) { throw new IllegalStateException(ex); }
            });
            if (request == null) throw new IllegalStateException("Quiz3 generation job preparation failed");
            aiWebClient.post().uri("/quiz3/generate").contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request).retrieve().toBodilessEntity().block(Duration.ofMinutes(timeoutMinutes));
        } catch (Exception ex) {
            String message = dispatchFailureMessage(ex);
            log.error("event=quiz3.dispatch.failed job_id={} error_type={}", jobId, ex.getClass().getSimpleName(), ex);
            jdbc.update("""
                    UPDATE edu.quiz3_generation_jobs SET status='FAILED', error_code='DISPATCH_FAILED',
                        error_message=?, lease_until=NULL, updated_at=CURRENT_TIMESTAMP WHERE id=?
                    """, message.substring(0, Math.min(1000, message.length())), jobId);
            jdbc.update("""
                    UPDATE edu.quiz3_assessments SET lifecycle_status='DRAFT', version=version+1,
                        updated_at=CURRENT_TIMESTAMP WHERE base_generation_job_id=?
                    """, jobId);
        }
    }

    static String dispatchFailureMessage(Exception ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        if (ex instanceof WebClientResponseException response && response.getStatusCode().value() == 422) {
            String detail = response.getResponseBodyAsString();
            if (!detail.isBlank()) return message + " detail=" + detail;
        }
        return message;
    }

    @Transactional
    ObjectNode prepare(UUID jobId) throws Exception {
        String snapshot = jdbc.query("""
                SELECT request_snapshot::text FROM edu.quiz3_generation_jobs
                WHERE id=? AND status IN ('PENDING','PROCESSING') FOR UPDATE
                """, rs -> rs.next() ? rs.getString(1) : null, jobId);
        if (snapshot == null) throw new IllegalStateException("Quiz3 generation job is not dispatchable");
        jdbc.update("""
                UPDATE edu.quiz3_generation_jobs SET status='PROCESSING', phase='TEXT_GENERATION',
                    attempt_count=attempt_count+1, processing_at=CURRENT_TIMESTAMP,
                    lease_until=CURRENT_TIMESTAMP + (? * INTERVAL '1 minute'), updated_at=CURRENT_TIMESTAMP WHERE id=?
                """, timeoutMinutes, jobId);
        int attempt = jdbc.queryForObject("SELECT attempt_count FROM edu.quiz3_generation_jobs WHERE id=?", Integer.class, jobId);
        ObjectNode request = (ObjectNode) objectMapper.readTree(snapshot);
        String base = webhookBaseUrl.replaceAll("/+$", "");
        request.put("webhookUrl", base + "/api/internal/webhooks/quiz3-generation");
        request.put("progressWebhookUrl", base + "/api/internal/webhooks/quiz3-generation/progress");
        request.put("webhookSecret", webhookSecret);
        request.put("attempt", attempt);
        return request;
    }

    @Scheduled(fixedDelayString = "${quiz3.generation.recovery-delay-ms:60000}")
    public void recover() {
        List<UUID> jobs = jdbc.query("""
                SELECT id FROM edu.quiz3_generation_jobs
                WHERE status='PENDING' OR (status='PROCESSING' AND lease_until < CURRENT_TIMESTAMP)
                ORDER BY created_at LIMIT 10
                """, (rs, row) -> rs.getObject(1, UUID.class));
        jobs.forEach(this::dispatch);
    }
}
