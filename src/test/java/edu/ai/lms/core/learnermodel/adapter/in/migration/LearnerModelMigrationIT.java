package edu.ai.lms.core.learnermodel.adapter.in.migration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
class LearnerModelMigrationIT {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("learner_model_migration")
            .withUsername("testuser")
            .withPassword("testpass");

    @Test
    void backfillsUniqueIdentityIdsAndCreatesAttemptOwnedResults() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            createLegacySchema(connection);
            executeMigration(connection);
            executeMigration(connection);

            assertEquals(1L, scalarLong(connection,
                    "SELECT kt_question_id FROM edu.questions WHERE id = "
                            + "'10000000-0000-0000-0000-000000000001'"));
            assertEquals(1L, scalarLong(connection,
                    "SELECT kt_learning_outcome_id FROM edu.learning_outcomes WHERE id = "
                            + "'20000000-0000-0000-0000-000000000001'"));

            execute(connection, """
                    INSERT INTO edu.questions (id)
                    VALUES ('10000000-0000-0000-0000-000000000002');
                    INSERT INTO edu.learning_outcomes (id)
                    VALUES ('20000000-0000-0000-0000-000000000002');
                    INSERT INTO edu.attempt_question_results (
                        attempt_id, question_id, kt_question_id, interaction_order,
                        is_correct, learning_outcome_id, kt_learning_outcome_id,
                        learning_outcome_mapping_count
                    ) VALUES (
                        '30000000-0000-0000-0000-000000000001',
                        '10000000-0000-0000-0000-000000000001', 1, 0, TRUE,
                        '20000000-0000-0000-0000-000000000001', 1, 1
                    );
                    """);

            executeAttemptRecordsMigration(connection);
            executeLearnerStatesMigration(connection);
            executeLearnerStatesMigration(connection);
            executeAktPyktRagMigration(connection);
            executeAktPyktRagMigration(connection);

            assertEquals(2L, scalarLong(connection,
                    "SELECT kt_question_id FROM edu.questions WHERE id = "
                            + "'10000000-0000-0000-0000-000000000002'"));
            assertEquals(2L, scalarLong(connection,
                    "SELECT kt_learning_outcome_id FROM edu.learning_outcomes WHERE id = "
                            + "'20000000-0000-0000-0000-000000000002'"));
            assertEquals(1L, scalarLong(connection,
                    "SELECT COUNT(*) FROM edu.attempt_records WHERE snapshot_complete = FALSE"));

            assertThrows(SQLException.class, () -> execute(connection, """
                    INSERT INTO edu.questions (id, kt_question_id)
                    OVERRIDING SYSTEM VALUE
                    VALUES ('10000000-0000-0000-0000-000000000003', 1)
                    """));

            executeResource(connection, "/schema.sql", "Fresh schema resource is missing");
            assertEquals(2L, scalarLong(connection, """
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = 'edu'
                      AND ((table_name = 'questions' AND column_name = 'kt_question_id')
                        OR (table_name = 'learning_outcomes'
                            AND column_name = 'kt_learning_outcome_id'))
                      AND is_identity = 'YES'
                      AND is_nullable = 'NO'
                    """));
            assertEquals(1L, scalarLong(connection, """
                    SELECT COUNT(*)
                    FROM information_schema.tables
                    WHERE table_schema = 'edu'
                      AND table_name = 'attempt_records'
                    """));
            assertEquals(2L, scalarLong(connection, """
                    SELECT COUNT(*)
                    FROM information_schema.tables
                    WHERE table_schema = 'edu'
                      AND table_name IN ('learner_states', 'learner_state_outcomes')
                    """));
            assertEquals(2L, scalarLong(connection, """
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = 'edu'
                      AND table_name = 'learner_state_outcomes'
                      AND column_name IN ('akt_pykt_probability', 'akt_pykt_prediction_status')
                    """));
            assertEquals(12L, scalarLong(connection, """
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = 'edu'
                      AND table_name = 'attempt_records'
                      AND column_name IN (
                        'question_type', 'question_content', 'question_content_format',
                        'student_answer_number', 'student_answer_position',
                        'student_answer_content', 'student_answer_content_format',
                        'correct_answer_number', 'correct_answer_position',
                        'correct_answer_content', 'correct_answer_content_format',
                        'snapshot_complete')
                    """));
        }
    }

    private static void createLegacySchema(Connection connection) throws SQLException {
        execute(connection, """
                CREATE SCHEMA edu;
                CREATE TABLE edu.questions (id UUID PRIMARY KEY);
                CREATE TABLE edu.learning_outcomes (id UUID UNIQUE NOT NULL);
                CREATE TABLE edu.attempts (id UUID PRIMARY KEY);
                INSERT INTO edu.questions VALUES
                    ('10000000-0000-0000-0000-000000000001');
                INSERT INTO edu.learning_outcomes VALUES
                    ('20000000-0000-0000-0000-000000000001');
                INSERT INTO edu.attempts VALUES
                    ('30000000-0000-0000-0000-000000000001');
                """);
    }

    private static void executeMigration(Connection connection) throws SQLException, IOException {
        executeResource(
                connection,
                "/db/manual/V20260916_01__learner_model_ids_and_attempt_question_results.sql",
                "Learner-model migration resource is missing");
    }

    private static void executeAttemptRecordsMigration(Connection connection) throws SQLException, IOException {
        executeResource(
                connection,
                "/db/manual/V20260916_02__immutable_attempt_records.sql",
                "Attempt-record migration resource is missing");
    }

    private static void executeLearnerStatesMigration(Connection connection) throws SQLException, IOException {
        executeResource(
                connection,
                "/db/manual/V20260917_02__learner_model_states.sql",
                "Learner-state migration resource is missing");
    }

    private static void executeAktPyktRagMigration(Connection connection) throws SQLException, IOException {
        executeResource(
                connection,
                "/db/manual/V20261001_01__learner_state_akt_pykt_for_rag.sql",
                "AKT-pyKT RAG migration resource is missing");
    }

    private static void executeResource(Connection connection, String path, String missingMessage)
            throws SQLException, IOException {
        String migration;
        try (var input = LearnerModelMigrationIT.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException(missingMessage);
            }
            migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        execute(connection, migration);
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static long scalarLong(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }
}
