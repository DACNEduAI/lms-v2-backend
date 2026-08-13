package edu.ai.lms.core.quiz.app.service.assessment;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/** Stops startup before the AI scheduler can query a partially migrated schema. */
@Component
@RequiredArgsConstructor
public class QuestionAiSchemaVerifier {
    static final String MIGRATION = "src/main/resources/db/manual/V20260813_02__quiz2_question_ai_jobs.sql";

    private static final String MISSING_OBJECTS_SQL = """
            SELECT object_name
            FROM (VALUES
                ('table edu.question_ai_working_drafts', to_regclass('edu.question_ai_working_drafts') IS NOT NULL),
                ('table edu.question_ai_jobs', to_regclass('edu.question_ai_jobs') IS NOT NULL),
                ('table edu.question_ai_job_steps', to_regclass('edu.question_ai_job_steps') IS NOT NULL),
                ('table edu.question_ai_artifacts', to_regclass('edu.question_ai_artifacts') IS NOT NULL),
                ('table edu.question_ai_artifact_revisions', to_regclass('edu.question_ai_artifact_revisions') IS NOT NULL),
                ('table edu.question_ai_change_sets', to_regclass('edu.question_ai_change_sets') IS NOT NULL),
                ('column edu.rich_content_assets.ai_artifact_id', EXISTS (
                    SELECT 1 FROM information_schema.columns
                    WHERE table_schema = 'edu' AND table_name = 'rich_content_assets'
                      AND column_name = 'ai_artifact_id')),
                ('unique partial index edu.uq_question_ai_active_job', EXISTS (
                    SELECT 1
                    FROM pg_class index_relation
                    JOIN pg_namespace namespace ON namespace.oid = index_relation.relnamespace
                    JOIN pg_index index_definition ON index_definition.indexrelid = index_relation.oid
                    WHERE namespace.nspname = 'edu'
                      AND index_relation.relname = 'uq_question_ai_active_job'
                      AND index_definition.indisunique
                      AND index_definition.indpred IS NOT NULL))
            ) required(object_name, present)
            WHERE NOT present
            ORDER BY object_name
            """;

    private final JdbcTemplate jdbc;

    @PostConstruct
    void verify() {
        List<String> missing = jdbc.queryForList(MISSING_OBJECTS_SQL, String.class);
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Quiz2 Question AI database migration is missing: "
                    + String.join(", ", missing) + ". Stop the backend and apply " + MIGRATION
                    + " while keeping DATABASE_INITIALIZATION_OPTION=never.");
        }
    }
}
