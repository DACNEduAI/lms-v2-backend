package edu.ai.lms.core.course.app.service.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Verifies that a development reset produces Matrix fixtures accepted by the active model. */
@Testcontainers(disabledWithoutDocker = true)
class MatrixSeedFixtureIT {
    private static final String MIDTERM_MATRIX_ID = "b3000001-0000-4000-8000-000000000001";
    private static final String CODE_RUNNER_MATRIX_ID = "b3000001-0000-4000-8000-000000000003";
    private static final String BIG_O_THREE_MATRIX_ID = "d5000001-0000-4000-8000-000000000001";
    private static final String BIG_O_SIX_MATRIX_ID = "d5000001-0000-4000-8000-000000000002";
    private static final String TEST_IMAGE_MATRIX_ID = "ad000033-dfad-46cd-9fe9-0f5c129f4032";
    private static final String DEMO_TWENTY_EIGHT_MATRIX_ID = "f0c3cf65-106a-4632-b16f-1a73439e437c";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("matrix_seed_fixture")
            .withUsername("testuser")
            .withPassword("testpass");

    @Test
    void resetFixturesHaveOneCellAndOneCllAssignmentPerMappedLearningOutcome() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            execute(connection, "CREATE EXTENSION IF NOT EXISTS pgcrypto;");
            executeResource(connection, "/schema.sql");
            executeResource(connection, "/data.sql");

            assertEquals(0L, scalarLong(connection, """
                    SELECT count(*)
                    FROM (
                        SELECT row.id
                        FROM edu.exam_matrix_rows row
                        LEFT JOIN edu.exam_matrix_cells cell ON cell.row_id = row.id
                        GROUP BY row.id
                        HAVING count(cell.custom_learning_level_id) <> 1
                    ) invalid_rows
                    """));
            assertEquals(0L, scalarLong(connection, """
                    SELECT count(*)
                    FROM edu.exam_matrix_rows row
                    LEFT JOIN edu.exam_matrix_outcome_learning_levels assignment
                      ON assignment.matrix_id = row.matrix_id
                     AND assignment.learning_outcome_id = row.mapped_learning_outcome_id
                    WHERE row.mapped_learning_outcome_id IS NOT NULL
                      AND assignment.learning_outcome_id IS NULL
                    """));
            assertEquals(0L, scalarLong(connection, """
                    SELECT count(*)
                    FROM edu.exam_matrix_rows row
                    JOIN edu.exam_matrix_cells cell ON cell.row_id = row.id
                    JOIN edu.exam_matrix_outcome_learning_levels assignment
                      ON assignment.matrix_id = row.matrix_id
                     AND assignment.learning_outcome_id = row.mapped_learning_outcome_id
                    WHERE cell.custom_learning_level_id <> assignment.custom_learning_level_id
                    """));
            assertEquals(6L, scalarLong(connection, """
                    SELECT count(*)
                    FROM edu.exam_matrices
                    WHERE course_id = 'c1111111-1111-1111-1111-111111111111'
                    """));
            assertEquals(0L, scalarLong(connection, """
                    SELECT count(*)
                    FROM edu.exam_matrices
                    WHERE title IN ('Matrix 2 - DSA final sample', 'manual matrix - final DSA 22 questions')
                    """));

            Map<String, Long> expectedManualTotals = Map.of(
                    MIDTERM_MATRIX_ID, 3L,
                    CODE_RUNNER_MATRIX_ID, 2L,
                    TEST_IMAGE_MATRIX_ID, 1L,
                    DEMO_TWENTY_EIGHT_MATRIX_ID, 28L);
            for (var expected : expectedManualTotals.entrySet()) {
                assertEquals(expected.getValue(), scalarLong(connection, """
                        SELECT coalesce(sum(cell.question_count), 0)
                        FROM edu.exam_matrix_rows row
                        JOIN edu.exam_matrix_cells cell ON cell.row_id = row.id
                        WHERE row.matrix_id = '%s'
                        """.formatted(expected.getKey())));
            }
            assertEquals(3L, scalarLong(connection, "SELECT total_question_count FROM edu.exam_matrices WHERE id = '"
                    + BIG_O_THREE_MATRIX_ID + "'"));
            assertEquals(6L, scalarLong(connection, "SELECT total_question_count FROM edu.exam_matrices WHERE id = '"
                    + BIG_O_SIX_MATRIX_ID + "'"));
            assertEquals(1L, scalarLong(connection, """
                    SELECT count(*)
                    FROM edu.exam_matrices
                    WHERE id = 'ad000033-dfad-46cd-9fe9-0f5c129f4032'
                      AND mode = 'MANUAL'
                      AND lifecycle_status = 'PUBLISHED'
                      AND ccct_snapshot_at = TIMESTAMP '2026-10-04 17:59:11.553745'
                    """));
            assertEquals(1L, scalarLong(connection, """
                    SELECT count(*)
                    FROM edu.exam_matrices
                    WHERE id = 'f0c3cf65-106a-4632-b16f-1a73439e437c'
                      AND title = 'Demo Matrix-28-quesions'
                      AND mode = 'MANUAL'
                      AND lifecycle_status = 'DRAFT'
                    """));
            assertEquals(10L, scalarLong(connection, """
                    SELECT count(*) FROM edu.matrix_ccct_snapshot_chapters
                    WHERE matrix_id = 'ad000033-dfad-46cd-9fe9-0f5c129f4032'
                    """));
            assertEquals(20L, scalarLong(connection, """
                    SELECT count(*) FROM edu.matrix_ccct_snapshot_items
                    WHERE matrix_id = 'ad000033-dfad-46cd-9fe9-0f5c129f4032'
                    """));
            assertEquals(0L, scalarLong(connection, """
                    SELECT count(*)
                    FROM (
                        SELECT coverage.custom_learning_level_id
                        FROM edu.exam_matrix_custom_learning_level_asset_coverages coverage
                        JOIN edu.exam_matrix_custom_learning_levels level
                          ON level.id = coverage.custom_learning_level_id
                        WHERE level.matrix_id IN (
                            'ad000033-dfad-46cd-9fe9-0f5c129f4032',
                            'f0c3cf65-106a-4632-b16f-1a73439e437c'
                        )
                        GROUP BY coverage.custom_learning_level_id
                        HAVING sum(coverage.percentage) <> 100
                    ) invalid_coverages
                    """));
        }
    }

    private static void executeResource(Connection connection, String path) throws IOException, SQLException {
        String sql;
        try (var input = MatrixSeedFixtureIT.class.getResourceAsStream(path)) {
            if (input == null) throw new IOException("Missing SQL resource: " + path);
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        execute(connection, sql);
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
