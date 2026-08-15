package edu.ai.lms.core.course.app.service.matrix2;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.ai.lms.core.course.app.service.exammatrix.ExamMatrixAccess;
import lombok.RequiredArgsConstructor;

/**
 * Persistence and validation for Matrix 2. This service intentionally does not
 * reference Exam Matrix, Quiz2, or any AI-generation contract.
 */
@Service
@RequiredArgsConstructor
public class Matrix2Service {
    private final JdbcTemplate jdbcTemplate;
    private final ExamMatrixAccess access;

    @Transactional(readOnly = true)
    public Matrix2List list(UUID courseId) {
        access.requireCourseTeacher(courseId);
        List<Matrix2Summary> matrices = jdbcTemplate.query("""
                SELECT m.id, m.course_id, m.title, m.description, m.created_at, m.updated_at,
                       COUNT(DISTINCT r.id) AS row_count,
                       COALESCE(SUM(c.question_count), 0) AS total_question_count
                FROM edu.exam_matrices_2 m
                LEFT JOIN edu.exam_matrix_2_rows r ON r.matrix_id = m.id
                LEFT JOIN edu.exam_matrix_2_cell_counts c ON c.row_id = r.id
                WHERE m.course_id = ?
                GROUP BY m.id
                ORDER BY m.updated_at DESC, m.created_at DESC
                """, (rs, rowNum) -> new Matrix2Summary(
                rs.getObject("id", UUID.class), rs.getObject("course_id", UUID.class),
                rs.getString("title"), rs.getString("description"), rs.getInt("row_count"),
                rs.getInt("total_question_count"), rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class)), courseId);
        return new Matrix2List(courseId, matrices);
    }

    @Transactional(readOnly = true)
    public Matrix2Detail get(UUID courseId, UUID matrixId) {
        access.requireCourseTeacher(courseId);
        return loadDetail(courseId, matrixId);
    }

    @Transactional
    public Matrix2Detail create(UUID courseId, Matrix2Payload payload) {
        access.requireCourseTeacher(courseId);
        validatePayload(courseId, payload);
        UUID matrixId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO edu.exam_matrices_2 (id, course_id, title, description)
                VALUES (?, ?, ?, ?)
                """, matrixId, courseId, payload.title().trim(), normalize(payload.description()));
        replaceChildren(courseId, matrixId, payload);
        return loadDetail(courseId, matrixId);
    }

    @Transactional
    public Matrix2Detail update(UUID courseId, UUID matrixId, Matrix2Payload payload) {
        access.requireCourseTeacher(courseId);
        requireMatrix(courseId, matrixId);
        validatePayload(courseId, payload);
        jdbcTemplate.update("""
                UPDATE edu.exam_matrices_2
                SET title = ?, description = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND course_id = ?
                """, payload.title().trim(), normalize(payload.description()), matrixId, courseId);
        jdbcTemplate.update("DELETE FROM edu.exam_matrix_2_rows WHERE matrix_id = ?", matrixId);
        jdbcTemplate.update("DELETE FROM edu.exam_matrix_2_custom_learning_levels WHERE matrix_id = ?", matrixId);
        replaceChildren(courseId, matrixId, payload);
        return loadDetail(courseId, matrixId);
    }

    @Transactional
    public void delete(UUID courseId, UUID matrixId) {
        access.requireCourseTeacher(courseId);
        int deleted = jdbcTemplate.update("DELETE FROM edu.exam_matrices_2 WHERE id = ? AND course_id = ?", matrixId, courseId);
        if (deleted == 0) throw notFound(matrixId);
    }

    private Matrix2Detail loadDetail(UUID courseId, UUID matrixId) {
        Matrix2Metadata metadata = jdbcTemplate.query("""
                SELECT id, course_id, title, description, created_at, updated_at
                FROM edu.exam_matrices_2 WHERE id = ? AND course_id = ?
                """, rs -> rs.next() ? new Matrix2Metadata(
                rs.getObject("id", UUID.class), rs.getObject("course_id", UUID.class),
                rs.getString("title"), rs.getString("description"),
                rs.getObject("created_at", LocalDateTime.class), rs.getObject("updated_at", LocalDateTime.class)) : null,
                matrixId, courseId);
        if (metadata == null) throw notFound(matrixId);

        List<Matrix2LearningLevel> levels = jdbcTemplate.query("""
                SELECT id, name, sort_order FROM edu.exam_matrix_2_custom_learning_levels
                WHERE matrix_id = ? ORDER BY sort_order, id
                """, (rs, rowNum) -> new Matrix2LearningLevel(
                rs.getObject("id", UUID.class), rs.getString("name"), rs.getInt("sort_order")), matrixId);

        Map<UUID, List<Matrix2CellCount>> countsByRow = new HashMap<>();
        jdbcTemplate.query("""
                SELECT r.id AS row_id, c.custom_learning_level_id, c.question_count
                FROM edu.exam_matrix_2_rows r
                JOIN edu.exam_matrix_2_cell_counts c ON c.row_id = r.id
                WHERE r.matrix_id = ?
                """, (RowCallbackHandler) rs -> countsByRow
                .computeIfAbsent(rs.getObject("row_id", UUID.class), ignored -> new ArrayList<>())
                .add(new Matrix2CellCount(rs.getObject("custom_learning_level_id", UUID.class), rs.getInt("question_count"))), matrixId);

        List<Matrix2Row> rows = jdbcTemplate.query("""
                SELECT id, chapter_id, mapped_learning_outcome_id, sort_order
                FROM edu.exam_matrix_2_rows WHERE matrix_id = ? ORDER BY sort_order, id
                """, (rs, rowNum) -> {
            UUID rowId = rs.getObject("id", UUID.class);
            return new Matrix2Row(rowId, rs.getObject("chapter_id", UUID.class),
                    rs.getObject("mapped_learning_outcome_id", UUID.class), rs.getInt("sort_order"),
                    List.copyOf(countsByRow.getOrDefault(rowId, List.of())));
        }, matrixId);
        return new Matrix2Detail(metadata.id(), metadata.courseId(), metadata.title(), metadata.description(),
                rows, levels, metadata.createdAt(), metadata.updatedAt());
    }

    private void replaceChildren(UUID courseId, UUID matrixId, Matrix2Payload payload) {
        List<Matrix2LearningLevelPayload> levels = safe(payload.customLearningLevels());
        for (int index = 0; index < levels.size(); index++) {
            Matrix2LearningLevelPayload level = levels.get(index);
            jdbcTemplate.update("""
                    INSERT INTO edu.exam_matrix_2_custom_learning_levels (id, matrix_id, name, sort_order)
                    VALUES (?, ?, ?, ?)
                    """, level.id(), matrixId, level.name().trim(), index);
        }
        for (int index = 0; index < safe(payload.rows()).size(); index++) {
            Matrix2RowPayload row = safe(payload.rows()).get(index);
            jdbcTemplate.update("""
                    INSERT INTO edu.exam_matrix_2_rows (id, matrix_id, chapter_id, mapped_learning_outcome_id, sort_order)
                    VALUES (?, ?, ?, ?, ?)
                    """, row.id(), matrixId, row.chapterId(), row.mappedLearningOutcomeId(), index);
            for (Matrix2CellCount count : safe(row.cellCounts())) {
                if (count.questionCount() == 0) continue;
                jdbcTemplate.update("""
                        INSERT INTO edu.exam_matrix_2_cell_counts (row_id, custom_learning_level_id, question_count)
                        VALUES (?, ?, ?)
                        """, row.id(), count.customLearningLevelId(), count.questionCount());
            }
        }
    }

    private void validatePayload(UUID courseId, Matrix2Payload payload) {
        if (payload == null || payload.title() == null || payload.title().isBlank()) {
            throw badRequest("Matrix 2 title cannot be blank");
        }
        Set<UUID> levelIds = new HashSet<>();
        for (Matrix2LearningLevelPayload level : safe(payload.customLearningLevels())) {
            if (level.id() == null || level.name() == null || level.name().isBlank()) {
                throw badRequest("Each Matrix 2 learning level needs an id and a name");
            }
            if (!levelIds.add(level.id())) throw badRequest("Duplicate Matrix 2 learning level id");
        }
        Set<UUID> rowIds = new HashSet<>();
        for (Matrix2RowPayload row : safe(payload.rows())) {
            if (row.id() == null || !rowIds.add(row.id())) throw badRequest("Each Matrix 2 row needs a unique id");
            if (row.chapterId() != null && !exists("SELECT 1 FROM edu.chapters WHERE chapter_id = ? AND course_id = ?", row.chapterId(), courseId)) {
                throw badRequest("Chapter does not belong to this course");
            }
            if (row.mappedLearningOutcomeId() != null
                    && !exists("SELECT 1 FROM edu.learning_outcomes WHERE id = ? AND course_id = ?", row.mappedLearningOutcomeId(), courseId)) {
                throw badRequest("Mapped learning outcome does not belong to this course");
            }
            Set<UUID> countedLevels = new HashSet<>();
            for (Matrix2CellCount count : safe(row.cellCounts())) {
                if (count.customLearningLevelId() == null || !levelIds.contains(count.customLearningLevelId())) {
                    throw badRequest("A Matrix 2 count references an unknown learning level");
                }
                if (count.questionCount() < 0) throw badRequest("Matrix 2 counts must be zero or greater");
                if (!countedLevels.add(count.customLearningLevelId())) throw badRequest("Duplicate count in a Matrix 2 row");
            }
        }
    }

    private boolean exists(String sql, UUID id, UUID courseId) {
        return Boolean.TRUE.equals(jdbcTemplate.query(sql, (ResultSetExtractor<Boolean>) rs -> rs.next(), id, courseId));
    }

    private void requireMatrix(UUID courseId, UUID matrixId) {
        if (!exists("SELECT 1 FROM edu.exam_matrices_2 WHERE id = ? AND course_id = ?", matrixId, courseId)) throw notFound(matrixId);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static <T> List<T> safe(List<T> value) {
        return value == null ? List.of() : value;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException notFound(UUID matrixId) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrix 2 not found: " + matrixId);
    }

    private record Matrix2Metadata(UUID id, UUID courseId, String title, String description,
                                   LocalDateTime createdAt, LocalDateTime updatedAt) { }

    public record Matrix2List(UUID courseId, List<Matrix2Summary> matrices) { }
    public record Matrix2Summary(UUID id, UUID courseId, String title, String description, int rowCount,
                                 int totalQuestionCount, LocalDateTime createdAt, LocalDateTime updatedAt) { }
    public record Matrix2Detail(UUID id, UUID courseId, String title, String description, List<Matrix2Row> rows,
                                List<Matrix2LearningLevel> customLearningLevels, LocalDateTime createdAt,
                                LocalDateTime updatedAt) { }
    public record Matrix2Row(UUID id, UUID chapterId, UUID mappedLearningOutcomeId, int sortOrder,
                             List<Matrix2CellCount> cellCounts) { }
    public record Matrix2LearningLevel(UUID id, String name, int sortOrder) { }
    public record Matrix2CellCount(UUID customLearningLevelId, int questionCount) { }
    public record Matrix2Payload(String title, String description, List<Matrix2RowPayload> rows,
                                 List<Matrix2LearningLevelPayload> customLearningLevels) { }
    public record Matrix2RowPayload(UUID id, UUID chapterId, UUID mappedLearningOutcomeId,
                                    List<Matrix2CellCount> cellCounts) { }
    public record Matrix2LearningLevelPayload(UUID id, String name) { }
}
