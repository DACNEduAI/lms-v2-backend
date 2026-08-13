package edu.ai.lms.core.material.adapter.out.persistence;

import edu.ai.lms.config.IntegrationTestConfiguration;
import edu.ai.lms.core.material.app.domain.MaterialIndexJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialIndexJobRepositoryIT extends IntegrationTestConfiguration {
    @Autowired MaterialIndexJobRepository repository;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void postgresUtcClaimsAJobQueuedByAJvmRunningInHoChiMinhTime() {
        TimeZone original = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        try {
            Instant queuedAt = Instant.now().minusSeconds(1);
            Instant leaseUntil = Instant.now().plus(Duration.ofMinutes(15));
            MaterialIndexJob job = MaterialIndexJob.builder()
                    .id(UUID.randomUUID())
                    .courseId(UUID.randomUUID())
                    .materialId(UUID.randomUUID())
                    .operation(MaterialIndexJob.Operation.INGEST)
                    .revision(UUID.randomUUID())
                    .fileKey("courses/course/material.pdf")
                    .status(MaterialIndexJob.Status.PENDING)
                    .attemptCount(0)
                    .nextAttemptAt(queuedAt)
                    .build();
            repository.save(job);

            assertEquals(1, repository.claim(job.getId(), leaseUntil));
            MaterialIndexJob claimed = repository.findById(job.getId()).orElseThrow();
            assertEquals(MaterialIndexJob.Status.PROCESSING, claimed.getStatus());
            assertEquals(1, claimed.getAttemptCount());
            assertTrue(Duration.between(leaseUntil, claimed.getLeaseUntil()).abs().toMillis() < 10);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void schedulingColumnsUseTimestampWithTimeZone() {
        for (String column : new String[]{"next_attempt_at", "lease_until", "completed_at", "created_at", "updated_at"}) {
            String dataType = jdbcTemplate.queryForObject("""
                    SELECT data_type
                    FROM information_schema.columns
                    WHERE table_schema = 'edu'
                      AND table_name = 'material_index_jobs'
                      AND column_name = ?
                    """, String.class, column);
            assertEquals("timestamp with time zone", dataType, column);
        }
    }
}
