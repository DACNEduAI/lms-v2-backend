package edu.ai.lms.core.quiz3.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz3.app.exception.QuizLifecycleConflictException;
import edu.ai.lms.core.quiz3.app.port.out.ManageQuizReleasePort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QuizReleasePersistenceAdapter implements ManageQuizReleasePort {
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public UUID publishRelease(UUID quizId, long quizVersion, UUID publishedBy, LocalDateTime publishedAt) {
        Integer codeRunnerQuestions = jdbc.queryForObject("""
                SELECT COUNT(*) FROM edu.questions
                WHERE quiz_id = :quizId AND type = 'CODE_RUNNER'
                """, Map.of("quizId", quizId), Integer.class);
        Integer verifiedQuestions = jdbc.queryForObject("""
                SELECT COUNT(*) FROM edu.questions q
                WHERE q.quiz_id = :quizId AND q.type = 'CODE_RUNNER'
                  AND 'VERIFIED_EXECUTION_GOLD' = (
                      SELECT c.contract_status FROM edu.code_runner_configs c
                      WHERE c.question_id = q.id
                      ORDER BY c.revision DESC
                      LIMIT 1
                  )
                """, Map.of("quizId", quizId), Integer.class);
        if (!codeRunnerQuestions.equals(verifiedQuestions)) {
            throw new QuizLifecycleConflictException(
                    "Every Code Runner question needs a verified configuration before publishing");
        }

        jdbc.update("UPDATE edu.quiz_releases SET active = false WHERE quiz_id = :quizId AND active = true",
                Map.of("quizId", quizId));
        UUID releaseId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO edu.quiz_releases
                    (id, quiz_id, release_number, quiz_version, active, published_by, published_at)
                SELECT :id, :quizId, COALESCE(MAX(release_number), 0) + 1,
                       :quizVersion, true, :publishedBy, :publishedAt
                FROM edu.quiz_releases WHERE quiz_id = :quizId
                """, new MapSqlParameterSource().addValue("id", releaseId).addValue("quizId", quizId)
                .addValue("quizVersion", quizVersion).addValue("publishedBy", publishedBy)
                .addValue("publishedAt", publishedAt));
        jdbc.update("""
                INSERT INTO edu.quiz_release_code_runner_bindings
                    (quiz_release_id, question_id, code_runner_config_id)
                SELECT :releaseId, q.id, config.id
                FROM edu.questions q
                JOIN LATERAL (
                    SELECT c.id FROM edu.code_runner_configs c
                    WHERE c.question_id = q.id
                    ORDER BY c.revision DESC
                    LIMIT 1
                ) config ON true
                JOIN edu.code_runner_configs selected ON selected.id = config.id
                WHERE q.quiz_id = :quizId AND q.type = 'CODE_RUNNER'
                  AND selected.contract_status = 'VERIFIED_EXECUTION_GOLD'
                """, Map.of("releaseId", releaseId, "quizId", quizId));
        return releaseId;
    }

    @Override
    public Optional<UUID> findActiveReleaseId(UUID quizId) {
        return jdbc.query("""
                SELECT id FROM edu.quiz_releases
                WHERE quiz_id = :quizId AND active = true
                """, Map.of("quizId", quizId), (rs, rowNum) -> rs.getObject("id", UUID.class))
                .stream().findFirst();
    }

    @Override
    public void deactivate(UUID quizId) {
        jdbc.update("UPDATE edu.quiz_releases SET active = false WHERE quiz_id = :quizId AND active = true",
                Map.of("quizId", quizId));
    }
}
