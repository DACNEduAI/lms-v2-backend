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
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
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
    private final LoadSignedInUserPort signedInUserPort;

    @Transactional(readOnly = true)
    public Matrix2List list(UUID courseId) {
        access.requireCourseTeacher(courseId);
        List<Matrix2Summary> matrices = jdbcTemplate.query("""
                SELECT m.id, m.course_id, m.title, m.description, m.lifecycle_status, m.version,
                       m.published_by, m.published_at, m.created_at, m.updated_at,
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
                rs.getString("title"), rs.getString("description"), Matrix2LifecycleStatus.valueOf(rs.getString("lifecycle_status")),
                rs.getLong("version"), rs.getObject("published_by", UUID.class),
                rs.getObject("published_at", LocalDateTime.class), rs.getInt("row_count"),
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
    public Matrix2Detail copyAsDraft(UUID courseId, UUID sourceMatrixId, Matrix2CopyRequest request) {
        access.requireCourseTeacher(courseId);
        Matrix2Detail source = loadDetail(courseId, sourceMatrixId);
        Map<UUID, UUID> levelIds = new HashMap<>();
        List<Matrix2LearningLevelPayload> levels = source.customLearningLevels().stream()
                .map(level -> {
                    UUID id = UUID.randomUUID();
                    levelIds.put(level.id(), id);
                    return new Matrix2LearningLevelPayload(id, level.name());
                }).toList();
        List<Matrix2RowPayload> rows = source.rows().stream().map(row -> new Matrix2RowPayload(
                UUID.randomUUID(), row.chapterId(), row.mappedLearningOutcomeId(),
                row.cellCounts().stream().map(cell -> new Matrix2CellCount(
                        levelIds.get(cell.customLearningLevelId()), cell.questionCount())).toList())).toList();
        String title = request == null || request.title() == null || request.title().isBlank()
                ? source.title() + " (Copy)" : request.title().trim();
        return create(courseId, new Matrix2Payload(title, source.description(), rows, levels, null));
    }

    @Transactional
    public Matrix2Detail update(UUID courseId, UUID matrixId, Matrix2Payload payload) {
        access.requireCourseTeacher(courseId);
        Matrix2Metadata current = requireEditableMatrix(courseId, matrixId);
        if (payload == null || payload.expectedVersion() == null || payload.expectedVersion() != current.version()) {
            throw conflict("Matrix 2 changed before update; reload and try again");
        }
        validatePayload(courseId, payload);
        int updated = jdbcTemplate.update("""
                UPDATE edu.exam_matrices_2
                SET title = ?, description = ?, version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND course_id = ? AND lifecycle_status = 'DRAFT' AND version = ?
                """, payload.title().trim(), normalize(payload.description()), matrixId, courseId, payload.expectedVersion());
        if (updated == 0) throw conflict("Matrix 2 changed before update; reload and try again");
        jdbcTemplate.update("DELETE FROM edu.exam_matrix_2_rows WHERE matrix_id = ?", matrixId);
        jdbcTemplate.update("DELETE FROM edu.exam_matrix_2_custom_learning_levels WHERE matrix_id = ?", matrixId);
        replaceChildren(courseId, matrixId, payload);
        return loadDetail(courseId, matrixId);
    }

    @Transactional
    public void delete(UUID courseId, UUID matrixId) {
        access.requireCourseTeacher(courseId);
        requireEditableMatrix(courseId, matrixId);
        int deleted = jdbcTemplate.update("DELETE FROM edu.exam_matrices_2 WHERE id = ? AND course_id = ? AND lifecycle_status = 'DRAFT'", matrixId, courseId);
        if (deleted == 0) throw conflict("Only a DRAFT Matrix 2 can be deleted");
    }

    @Transactional
    public Matrix2Detail publish(UUID courseId, UUID matrixId, Matrix2PublishRequest request) {
        access.requireCourseTeacher(courseId);
        Matrix2Metadata current = requireEditableMatrix(courseId, matrixId);
        if (request == null || request.expectedVersion() != current.version()) {
            throw conflict("Matrix 2 changed before publish; reload and try again");
        }
        Matrix2Detail detail = loadDetail(courseId, matrixId);
        validateConfirmable(detail);
        UUID actorId = signedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required"))
                .getId();
        int updated = jdbcTemplate.update("""
                UPDATE edu.exam_matrices_2
                SET lifecycle_status = 'PUBLISHED', version = version + 1,
                    published_by = ?, published_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND course_id = ? AND lifecycle_status = 'DRAFT' AND version = ?
                """, actorId, matrixId, courseId, request.expectedVersion());
        if (updated == 0) throw conflict("Matrix 2 changed before publish; reload and try again");
        return loadDetail(courseId, matrixId);
    }

    private Matrix2Detail loadDetail(UUID courseId, UUID matrixId) {
        Matrix2Metadata metadata = jdbcTemplate.query("""
                SELECT id, course_id, title, description, lifecycle_status, version,
                       published_by, published_at, created_at, updated_at
                FROM edu.exam_matrices_2 WHERE id = ? AND course_id = ?
                """, rs -> rs.next() ? new Matrix2Metadata(
                rs.getObject("id", UUID.class), rs.getObject("course_id", UUID.class),
                rs.getString("title"), rs.getString("description"),
                Matrix2LifecycleStatus.valueOf(rs.getString("lifecycle_status")), rs.getLong("version"),
                rs.getObject("published_by", UUID.class), rs.getObject("published_at", LocalDateTime.class),
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
                SELECT r.id, r.chapter_id, c.title AS chapter_name, r.mapped_learning_outcome_id,
                       CASE WHEN lo.prefix_number IS NULL THEN lo.description
                            ELSE 'LO' || lo.prefix_number || ' - ' || lo.description END AS learning_outcome_name,
                       r.sort_order
                FROM edu.exam_matrix_2_rows r
                LEFT JOIN edu.chapters c ON c.chapter_id = r.chapter_id
                LEFT JOIN edu.learning_outcomes lo ON lo.id = r.mapped_learning_outcome_id
                WHERE r.matrix_id = ? ORDER BY r.sort_order, r.id
                """, (rs, rowNum) -> {
            UUID rowId = rs.getObject("id", UUID.class);
            return new Matrix2Row(rowId, rs.getObject("chapter_id", UUID.class), rs.getString("chapter_name"),
                    rs.getObject("mapped_learning_outcome_id", UUID.class), rs.getString("learning_outcome_name"), rs.getInt("sort_order"),
                    List.copyOf(countsByRow.getOrDefault(rowId, List.of())));
        }, matrixId);
        return new Matrix2Detail(metadata.id(), metadata.courseId(), metadata.title(), metadata.description(),
                metadata.lifecycleStatus(), metadata.version(), metadata.publishedBy(), metadata.publishedAt(),
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

    public void validateConfirmable(Matrix2Detail matrix) {
        List<String> errors = new ArrayList<>();
        if (matrix.rows().isEmpty()) errors.add("Matrix 2 needs at least one row");
        if (matrix.customLearningLevels().isEmpty()) errors.add("Matrix 2 needs at least one custom learning level");
        for (int index = 0; index < matrix.rows().size(); index++) {
            Matrix2Row row = matrix.rows().get(index);
            if (row.chapterId() == null) errors.add("Row " + (index + 1) + " is missing Chapter");
            if (row.mappedLearningOutcomeId() == null) errors.add("Row " + (index + 1) + " is missing mapped learning outcome");
        }
        if (matrix.customLearningLevels().stream().anyMatch(level -> level.name() == null || level.name().isBlank())) {
            errors.add("Every custom learning level needs a name");
        }
        int total = matrix.rows().stream().flatMap(row -> row.cellCounts().stream())
                .mapToInt(Matrix2CellCount::questionCount).sum();
        if (total <= 0) errors.add("Matrix 2 must contain at least one question");
        if (!errors.isEmpty()) throw badRequest(String.join("; ", errors));
    }

    private boolean exists(String sql, UUID id, UUID courseId) {
        return Boolean.TRUE.equals(jdbcTemplate.query(sql, (ResultSetExtractor<Boolean>) rs -> rs.next(), id, courseId));
    }

    private Matrix2Metadata requireEditableMatrix(UUID courseId, UUID matrixId) {
        Matrix2Detail detail = loadDetail(courseId, matrixId);
        if (detail.lifecycleStatus() != Matrix2LifecycleStatus.DRAFT) {
            throw conflict("Published Matrix 2 is read-only");
        }
        return new Matrix2Metadata(detail.id(), detail.courseId(), detail.title(), detail.description(),
                detail.lifecycleStatus(), detail.version(), detail.publishedBy(), detail.publishedAt(),
                detail.createdAt(), detail.updatedAt());
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

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private static ResponseStatusException notFound(UUID matrixId) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrix 2 not found: " + matrixId);
    }

    private record Matrix2Metadata(UUID id, UUID courseId, String title, String description,
                                   Matrix2LifecycleStatus lifecycleStatus, long version, UUID publishedBy,
                                   LocalDateTime publishedAt,
                                   LocalDateTime createdAt, LocalDateTime updatedAt) { }

    public enum Matrix2LifecycleStatus { DRAFT, PUBLISHED }
    public record Matrix2List(UUID courseId, List<Matrix2Summary> matrices) { }
    public record Matrix2Summary(UUID id, UUID courseId, String title, String description,
                                 Matrix2LifecycleStatus lifecycleStatus, long version, UUID publishedBy,
                                 LocalDateTime publishedAt, int rowCount,
                                 int totalQuestionCount, LocalDateTime createdAt, LocalDateTime updatedAt) { }
    public record Matrix2Detail(UUID id, UUID courseId, String title, String description,
                                Matrix2LifecycleStatus lifecycleStatus, long version, UUID publishedBy,
                                LocalDateTime publishedAt, List<Matrix2Row> rows,
                                List<Matrix2LearningLevel> customLearningLevels, LocalDateTime createdAt,
                                LocalDateTime updatedAt) { }
    public record Matrix2Row(UUID id, UUID chapterId, String chapterName, UUID mappedLearningOutcomeId,
                             String mappedLearningOutcomeName, int sortOrder,
                             List<Matrix2CellCount> cellCounts) { }
    public record Matrix2LearningLevel(UUID id, String name, int sortOrder) { }
    public record Matrix2CellCount(UUID customLearningLevelId, int questionCount) { }
    public record Matrix2Payload(String title, String description, List<Matrix2RowPayload> rows,
                                 List<Matrix2LearningLevelPayload> customLearningLevels,
                                 Long expectedVersion) { }
    public record Matrix2RowPayload(UUID id, UUID chapterId, UUID mappedLearningOutcomeId,
                                    List<Matrix2CellCount> cellCounts) { }
    public record Matrix2LearningLevelPayload(UUID id, String name) { }
    public record Matrix2PublishRequest(long expectedVersion) { }
    public record Matrix2CopyRequest(String title) { }
}
