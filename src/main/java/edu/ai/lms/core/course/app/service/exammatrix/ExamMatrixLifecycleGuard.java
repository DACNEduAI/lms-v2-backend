package edu.ai.lms.core.course.app.service.exammatrix;

import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import edu.ai.lms.core.course.app.exception.ExamMatrixValidationException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExamMatrixLifecycleGuard {
    private final NamedParameterJdbcTemplate jdbc;

    public void requireDraft(UUID courseId, UUID matrixId) {
        Boolean draft = jdbc.query("""
                SELECT lifecycle_status = 'DRAFT' AS draft FROM edu.exam_matrices
                WHERE course_id = :courseId AND id = :matrixId
                """, Map.of("courseId", courseId, "matrixId", matrixId),
                (rs, rowNum) -> rs.getBoolean("draft")).stream().findFirst().orElse(null);
        if (!Boolean.TRUE.equals(draft)) {
            throw new ExamMatrixValidationException(
                    "Published Exam Matrix is immutable; clone it to a Draft before editing or deleting");
        }
    }

    public void markDraftChanged(UUID courseId, UUID matrixId) {
        int updated = jdbc.update("""
                UPDATE edu.exam_matrices SET version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE course_id = :courseId AND id = :matrixId AND lifecycle_status = 'DRAFT'
                """, Map.of("courseId", courseId, "matrixId", matrixId));
        if (updated != 1) requireDraft(courseId, matrixId);
    }
}
