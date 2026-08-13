package edu.ai.lms.core.quiz.adapter.in.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class QuizGenerationQueueInstantsMigrationIT {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("quiz_generation_timezone")
            .withUsername("testuser")
            .withPassword("testpass");

    @Test
    void convertsTheKnownLegacyQueueTimestampsToTheirOriginalInstants() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            createLegacyQueueSchema(connection);
            executeMigration(connection);

            assertEquals(12, scalarInt(connection, """
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = 'edu'
                      AND table_name IN ('quiz_generation_jobs', 'question_image_generation_jobs')
                      AND column_name IN ('next_attempt_at', 'lease_until', 'processing_at',
                                          'completed_at', 'created_at', 'updated_at')
                      AND data_type = 'timestamp with time zone'
                    """));

            assertInstant(connection, "created_at", Instant.parse("2026-08-13T19:36:58Z"));
            assertInstant(connection, "processing_at", Instant.parse("2026-08-13T19:36:59Z"));
            assertInstant(connection, "next_attempt_at", Instant.parse("2026-08-13T19:37:00Z"));
            assertInstant(connection, "lease_until", Instant.parse("2026-08-13T19:37:59Z"));
            assertInstant(connection, "completed_at", Instant.parse("2026-08-13T19:37:05Z"));
            assertInstant(connection, "updated_at", Instant.parse("2026-08-13T19:37:06Z"));
        }
    }

    private static void createLegacyQueueSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE SCHEMA edu;
                    CREATE TABLE edu.quiz_generation_jobs (
                        status TEXT NOT NULL,
                        phase TEXT NOT NULL,
                        attempt_count INTEGER NOT NULL,
                        error_code TEXT,
                        next_attempt_at TIMESTAMP WITHOUT TIME ZONE,
                        lease_until TIMESTAMP WITHOUT TIME ZONE,
                        processing_at TIMESTAMP WITHOUT TIME ZONE,
                        completed_at TIMESTAMP WITHOUT TIME ZONE,
                        created_at TIMESTAMP WITHOUT TIME ZONE,
                        updated_at TIMESTAMP WITHOUT TIME ZONE
                    );
                    CREATE TABLE edu.question_image_generation_jobs (
                        status TEXT NOT NULL,
                        next_attempt_at TIMESTAMP WITHOUT TIME ZONE,
                        lease_until TIMESTAMP WITHOUT TIME ZONE,
                        processing_at TIMESTAMP WITHOUT TIME ZONE,
                        completed_at TIMESTAMP WITHOUT TIME ZONE,
                        created_at TIMESTAMP WITHOUT TIME ZONE,
                        updated_at TIMESTAMP WITHOUT TIME ZONE
                    );

                    INSERT INTO edu.quiz_generation_jobs (
                        status, phase, attempt_count, error_code, next_attempt_at, lease_until,
                        processing_at, completed_at, created_at, updated_at
                    ) VALUES
                        ('TIMED_OUT', 'TEXT_GENERATION', 0, 'GENERATION_TIMEOUT',
                         TIMESTAMP '2026-08-14 02:37:00', TIMESTAMP '2026-08-14 02:37:59',
                         TIMESTAMP '2026-08-13 19:36:59', TIMESTAMP '2026-08-14 02:37:05',
                         TIMESTAMP '2026-08-13 19:36:58', TIMESTAMP '2026-08-13 19:37:06'),
                        ('TIMED_OUT', 'TEXT_GENERATION', 0, 'GENERATION_TIMEOUT',
                         TIMESTAMP '2026-08-14 02:37:00', TIMESTAMP '2026-08-14 02:37:59',
                         TIMESTAMP '2026-08-13 19:36:59', TIMESTAMP '2026-08-14 02:37:05',
                         TIMESTAMP '2026-08-13 19:36:58', TIMESTAMP '2026-08-13 19:37:06');
                    """);
        }
    }

    private static void executeMigration(Connection connection) throws SQLException, IOException {
        String migration;
        try (var input = QuizGenerationQueueInstantsMigrationIT.class.getResourceAsStream(
                "/db/manual/V20260814_02__quiz_generation_queue_instants.sql")) {
            if (input == null) {
                throw new IOException("Quiz Generation UTC migration resource is missing");
            }
            migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(migration);
        }
    }

    private static void assertInstant(Connection connection, String column, Instant expected) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("""
                SELECT %s
                FROM edu.quiz_generation_jobs
                ORDER BY created_at
                LIMIT 1
                """.formatted(column))) {
            result.next();
            assertEquals(expected, result.getObject(1, OffsetDateTime.class).toInstant());
        }
    }

    private static int scalarInt(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }
}
