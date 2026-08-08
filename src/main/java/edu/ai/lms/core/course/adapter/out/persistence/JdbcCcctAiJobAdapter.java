package edu.ai.lms.core.course.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ai.lms.core.course.app.domain.CcctAiJob;
import edu.ai.lms.core.course.app.domain.CcctAiJobStatus;
import edu.ai.lms.core.course.app.domain.CcctAiJobType;
import edu.ai.lms.core.course.app.port.out.CcctAiJobPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class JdbcCcctAiJobAdapter implements CcctAiJobPort {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @Override
    public CcctAiJob create(CcctAiJob job) {
        UUID id = job.getId() == null ? UUID.randomUUID() : job.getId();
        jdbc.update("""
                INSERT INTO edu.ccct_ai_jobs
                  (id, course_id, profile_id, type, status, stage, expected_version,
                   file_key, warnings_json, created_at, updated_at)
                VALUES
                  (:id, :courseId, :profileId, :type, 'QUEUED', 'QUEUED',
                   :expectedVersion, :fileKey, '[]', NOW(), NOW())
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("courseId", job.getCourseId())
                .addValue("profileId", job.getProfileId())
                .addValue("type", job.getType().name())
                .addValue("expectedVersion", job.getExpectedVersion())
                .addValue("fileKey", job.getFileKey()));
        return findById(id).orElseThrow();
    }

    @Override
    public Optional<CcctAiJob> findById(UUID jobId) {
        return jdbc.query("""
                SELECT id, course_id, profile_id, type, status, stage, expected_version,
                       file_key, warnings_json, error_message, created_at, updated_at, completed_at
                FROM edu.ccct_ai_jobs
                WHERE id = :id
                """, Map.of("id", jobId), this::map).stream().findFirst();
    }

    @Override
    public boolean claim(UUID jobId) {
        return jdbc.update("""
                UPDATE edu.ccct_ai_jobs
                SET status = 'RUNNING', stage = 'STARTING', updated_at = NOW()
                WHERE id = :id AND status = 'QUEUED'
                """, Map.of("id", jobId)) == 1;
    }

    @Override
    public void updateStage(UUID jobId, String stage) {
        jdbc.update("""
                UPDATE edu.ccct_ai_jobs SET stage = :stage, updated_at = NOW()
                WHERE id = :id AND status = 'RUNNING'
                """, Map.of("id", jobId, "stage", stage));
    }

    @Override
    public void complete(UUID jobId, List<String> warnings) {
        terminal(jobId, "COMPLETED", "COMPLETED", warnings, null);
    }

    @Override
    public void fail(UUID jobId, String message) {
        terminal(jobId, "FAILED", "FAILED", List.of(), message);
    }

    @Override
    public void stale(UUID jobId, String message) {
        terminal(jobId, "STALE", "STALE", List.of(), message);
    }

    @Override
    public int requeueStaleRunning(Duration age) {
        return jdbc.update("""
                UPDATE edu.ccct_ai_jobs
                SET status = 'QUEUED', stage = 'QUEUED', updated_at = NOW()
                WHERE status = 'RUNNING'
                  AND updated_at < NOW() - (:seconds * INTERVAL '1 second')
                """, Map.of("seconds", age.toSeconds()));
    }

    @Override
    public List<UUID> findQueuedIds(int limit) {
        return jdbc.queryForList("""
                SELECT id FROM edu.ccct_ai_jobs
                WHERE status = 'QUEUED'
                ORDER BY created_at
                LIMIT :limit
                """, Map.of("limit", limit), UUID.class);
    }

    private void terminal(
            UUID jobId,
            String status,
            String stage,
            List<String> warnings,
            String error) {
        jdbc.update("""
                UPDATE edu.ccct_ai_jobs
                SET status = :status, stage = :stage, warnings_json = :warnings,
                    error_message = :error, updated_at = NOW(), completed_at = NOW()
                WHERE id = :id AND status IN ('QUEUED', 'RUNNING')
                """, new MapSqlParameterSource()
                .addValue("id", jobId)
                .addValue("status", status)
                .addValue("stage", stage)
                .addValue("warnings", writeWarnings(warnings))
                .addValue("error", error));
    }

    private CcctAiJob map(ResultSet rs, int rowNumber) throws SQLException {
        return CcctAiJob.builder()
                .id(rs.getObject("id", UUID.class))
                .courseId(rs.getObject("course_id", UUID.class))
                .profileId(rs.getObject("profile_id", UUID.class))
                .type(CcctAiJobType.valueOf(rs.getString("type")))
                .status(CcctAiJobStatus.valueOf(rs.getString("status")))
                .stage(rs.getString("stage"))
                .expectedVersion(rs.getLong("expected_version"))
                .fileKey(rs.getString("file_key"))
                .warnings(readWarnings(rs.getString("warnings_json")))
                .errorMessage(rs.getString("error_message"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                .completedAt(rs.getTimestamp("completed_at") == null
                        ? null : rs.getTimestamp("completed_at").toLocalDateTime())
                .build();
    }

    private String writeWarnings(List<String> warnings) {
        try {
            return objectMapper.writeValueAsString(warnings == null ? List.of() : warnings);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize CCCT AI warnings", exception);
        }
    }

    private List<String> readWarnings(String value) {
        try {
            return value == null ? List.of() : objectMapper.readValue(value, STRING_LIST);
        } catch (Exception exception) {
            return List.of("AI warnings could not be decoded");
        }
    }
}
