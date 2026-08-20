package edu.ai.lms.core.coderunner.adapter.out.execution;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Exposes durable queue stalls to Actuator and emits a periodic warning that
 * production log/alert collectors can route without inspecting application data.
 */
@Slf4j
@Component("codeRunnerQueue")
public class CodeRunnerQueueHealthIndicator implements HealthIndicator {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final Duration submissionStuckAfter;
    private final Duration finalizingStuckAfter;

    public CodeRunnerQueueHealthIndicator(
            JdbcTemplate jdbc,
            Clock clock,
            @Value("${code-runner.monitor.submission-stuck-after:5m}") Duration submissionStuckAfter,
            @Value("${code-runner.monitor.finalizing-stuck-after:15m}") Duration finalizingStuckAfter) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.submissionStuckAfter = submissionStuckAfter;
        this.finalizingStuckAfter = finalizingStuckAfter;
    }

    @Override
    public Health health() {
        Snapshot snapshot = snapshot();
        Health.Builder health = snapshot.isHealthy() ? Health.up() : Health.down();
        return health.withDetail("stuckSubmissions", snapshot.stuckSubmissions())
                .withDetail("stuckFinalizingAttempts", snapshot.stuckFinalizingAttempts())
                .withDetail("submissionThresholdSeconds", submissionStuckAfter.toSeconds())
                .withDetail("finalizingThresholdSeconds", finalizingStuckAfter.toSeconds())
                .build();
    }

    @Scheduled(fixedDelayString = "${code-runner.monitor.delay-ms:60000}")
    public void warnWhenStuck() {
        Snapshot snapshot = snapshot();
        if (!snapshot.isHealthy()) {
            log.warn("Code Runner queue health degraded: stuckSubmissions={}, stuckFinalizingAttempts={}",
                    snapshot.stuckSubmissions(), snapshot.stuckFinalizingAttempts());
        }
    }

    private Snapshot snapshot() {
        Timestamp submissionCutoff = Timestamp.from(clock.instant().minus(submissionStuckAfter));
        Timestamp finalizingCutoff = Timestamp.from(clock.instant().minus(finalizingStuckAfter));
        Integer submissions = jdbc.queryForObject("""
                SELECT COUNT(*) FROM edu.code_submissions
                WHERE (status='PENDING' AND created_at < ? AND next_attempt_at <= CURRENT_TIMESTAMP)
                   OR (status='PROCESSING' AND lease_until < ?)
                """, Integer.class, submissionCutoff, submissionCutoff);
        Integer attempts = jdbc.queryForObject("""
                SELECT COUNT(*) FROM edu.attempts
                WHERE status='FINALIZING' AND updated_at < ?
                """, Integer.class, finalizingCutoff);
        return new Snapshot(submissions == null ? 0 : submissions, attempts == null ? 0 : attempts);
    }

    private record Snapshot(int stuckSubmissions, int stuckFinalizingAttempts) {
        private boolean isHealthy() {
            return stuckSubmissions == 0 && stuckFinalizingAttempts == 0;
        }
    }
}
