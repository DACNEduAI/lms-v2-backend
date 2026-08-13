package edu.ai.lms.core.quiz.adapter.in.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class QuestionExplanationMigrationIT {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("quiz2_migration")
            .withUsername("testuser")
            .withPassword("testpass");

    @Test
    void upgradesLegacySchemaTwiceWithoutChangingAnswerExplanations() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            createLegacySchema(connection);
            String fingerprintBefore = answerFingerprint(connection);

            executeMigration(connection);
            executeMigration(connection);

            assertEquals(6, scalarInt(connection, """
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = 'edu'
                      AND table_name IN ('question_drafts', 'question_bank_questions', 'questions')
                      AND column_name IN ('explanation', 'explanation_format')
                    """));
            assertEquals(3, scalarInt(connection, """
                    SELECT COUNT(*)
                    FROM pg_constraint constraint_row
                    JOIN pg_class table_row ON table_row.oid = constraint_row.conrelid
                    JOIN pg_namespace schema_row ON schema_row.oid = table_row.relnamespace
                    WHERE schema_row.nspname = 'edu'
                      AND constraint_row.conname IN (
                          'ck_question_drafts_explanation_format',
                          'ck_question_bank_questions_explanation_format',
                          'ck_questions_explanation_format'
                      )
                    """));
            assertEquals("Draft correct", scalarString(connection,
                    "SELECT explanation FROM edu.question_drafts WHERE id = '10000000-0000-0000-0000-000000000001'"));
            assertEquals("Bank correct", scalarString(connection,
                    "SELECT explanation FROM edu.question_bank_questions WHERE id = '20000000-0000-0000-0000-000000000001'"));
            assertEquals("Variant correct", scalarString(connection,
                    "SELECT explanation FROM edu.questions WHERE id = '30000000-0000-0000-0000-000000000001'"));
            assertEquals(0, scalarInt(connection, """
                    SELECT COUNT(*) FROM edu.questions
                    WHERE id = '30000000-0000-0000-0000-000000000002'
                      AND explanation IS NOT NULL
                    """));
            assertEquals(fingerprintBefore, answerFingerprint(connection));
        }
    }

    private static void createLegacySchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE SCHEMA edu;
                    CREATE TABLE edu.question_drafts (
                        id UUID PRIMARY KEY,
                        assessment_id UUID
                    );
                    CREATE TABLE edu.question_draft_answers (
                        id UUID PRIMARY KEY,
                        question_draft_id UUID NOT NULL,
                        order_index INTEGER NOT NULL,
                        is_correct BOOLEAN NOT NULL,
                        explanation TEXT,
                        explanation_format VARCHAR(30)
                    );
                    CREATE TABLE edu.question_bank_questions (
                        id UUID PRIMARY KEY,
                        assessment_id UUID
                    );
                    CREATE TABLE edu.question_bank_answers (
                        id UUID PRIMARY KEY,
                        question_bank_question_id UUID NOT NULL,
                        order_index INTEGER NOT NULL,
                        is_correct BOOLEAN NOT NULL,
                        explanation TEXT,
                        explanation_format VARCHAR(30)
                    );
                    CREATE TABLE edu.questions (
                        id UUID PRIMARY KEY,
                        quiz_id UUID NOT NULL
                    );
                    CREATE TABLE edu.assessment_variants (quiz_id UUID NOT NULL);
                    CREATE TABLE edu.answers (
                        answer_number INTEGER NOT NULL,
                        question_id UUID NOT NULL,
                        is_correct BOOLEAN NOT NULL,
                        explanation TEXT,
                        explanation_format VARCHAR(30),
                        PRIMARY KEY (answer_number, question_id)
                    );

                    INSERT INTO edu.question_drafts VALUES
                        ('10000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000001');
                    INSERT INTO edu.question_draft_answers VALUES
                        ('11000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 0, TRUE, 'Draft correct', 'PLAIN_TEXT'),
                        ('11000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 1, FALSE, 'Draft wrong', 'PLAIN_TEXT');

                    INSERT INTO edu.question_bank_questions VALUES
                        ('20000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000001');
                    INSERT INTO edu.question_bank_answers VALUES
                        ('21000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 0, TRUE, 'Bank correct', 'TIPTAP_HTML'),
                        ('21000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', 1, FALSE, 'Bank wrong', 'PLAIN_TEXT');

                    INSERT INTO edu.questions VALUES
                        ('30000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001'),
                        ('30000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002');
                    INSERT INTO edu.assessment_variants VALUES
                        ('40000000-0000-0000-0000-000000000001');
                    INSERT INTO edu.answers VALUES
                        (1, '30000000-0000-0000-0000-000000000001', TRUE, 'Variant correct', 'PLAIN_TEXT'),
                        (2, '30000000-0000-0000-0000-000000000001', FALSE, 'Variant wrong', 'PLAIN_TEXT'),
                        (1, '30000000-0000-0000-0000-000000000002', TRUE, 'Legacy quiz correct', 'PLAIN_TEXT');
                    """);
        }
    }

    private static void executeMigration(Connection connection) throws SQLException, IOException {
        String migration;
        try (var input = QuestionExplanationMigrationIT.class.getResourceAsStream(
                "/db/manual/V20260813_01__quiz2_question_explanation.sql")) {
            if (input == null) {
                throw new IOException("Question explanation migration resource is missing");
            }
            migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(migration);
        }
    }

    private static String answerFingerprint(Connection connection) throws SQLException {
        return scalarString(connection, """
                SELECT md5(string_agg(value, '|' ORDER BY value))
                FROM (
                    SELECT 'draft:' || id || ':' || COALESCE(explanation, '<null>') || ':' || COALESCE(explanation_format, '<null>') AS value
                    FROM edu.question_draft_answers
                    UNION ALL
                    SELECT 'bank:' || id || ':' || COALESCE(explanation, '<null>') || ':' || COALESCE(explanation_format, '<null>')
                    FROM edu.question_bank_answers
                    UNION ALL
                    SELECT 'answer:' || question_id || ':' || answer_number || ':' || COALESCE(explanation, '<null>') || ':' || COALESCE(explanation_format, '<null>')
                    FROM edu.answers
                ) values_to_hash
                """);
    }

    private static int scalarInt(Connection connection, String sql) throws SQLException {
        return Integer.parseInt(scalarString(connection, sql));
    }

    private static String scalarString(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }
}
