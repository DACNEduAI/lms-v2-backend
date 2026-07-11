package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.ExamMatrix;
import edu.ai.lms.core.course.app.domain.ExamMatrixRow;
import edu.ai.lms.core.course.app.port.out.DeleteExamMatrixPort;
import edu.ai.lms.core.course.app.port.out.LoadExamMatrixPort;
import edu.ai.lms.core.course.app.port.out.SaveExamMatrixPort;
import lombok.RequiredArgsConstructor;

/**
 * Persistence adapter implementing all exam-matrix output ports.
 * Uses a replace-all strategy for rows: on every save it deletes existing
 * rows for the matrix and inserts the new set provided by the domain object.
 */
@Component
@RequiredArgsConstructor
class ExamMatrixPersistenceAdapter implements SaveExamMatrixPort, LoadExamMatrixPort, DeleteExamMatrixPort {

    private final ExamMatrixRepository examMatrixRepository;

    // ── SaveExamMatrixPort ────────────────────────────────────────────────────

    @Override
    public ExamMatrix save(ExamMatrix examMatrix) {
        boolean isNew = examMatrix.getId() == null
                || examMatrixRepository.findById(examMatrix.getId()).isEmpty();

        if (isNew) {
            examMatrix.setId(UUID.randomUUID());
            examMatrixRepository.insertMatrix(examMatrix);
        } else {
            examMatrixRepository.updateMatrix(examMatrix);
            // Replace-all: delete old rows then re-insert
            examMatrixRepository.deleteRowsByMatrixId(examMatrix.getId());
        }

        // Insert all rows
        if (examMatrix.getRows() != null) {
            for (ExamMatrixRow row : examMatrix.getRows()) {
                row.setMatrixId(examMatrix.getId());
                if (row.getId() == null) {
                    row.setId(UUID.randomUUID());
                }
                examMatrixRepository.insertRow(row);
            }
        }

        return loadFull(examMatrix.getId());
    }

    // ── LoadExamMatrixPort ────────────────────────────────────────────────────

    @Override
    public Optional<ExamMatrix> findById(UUID matrixId) {
        return examMatrixRepository.findById(matrixId)
                .map(m -> loadFull(m.getId()));
    }

    @Override
    public List<ExamMatrix> findAllByCourseId(UUID courseId) {
        return examMatrixRepository.findAllByCourseId(courseId).stream()
                .map(m -> loadFull(m.getId()))
                .toList();
    }

    // ── DeleteExamMatrixPort ──────────────────────────────────────────────────

    @Override
    public void deleteById(UUID matrixId) {
        // exam_matrix_rows are deleted by ON DELETE CASCADE in the DB schema
        examMatrixRepository.deleteById(matrixId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ExamMatrix loadFull(UUID matrixId) {
        ExamMatrix matrix = examMatrixRepository.findById(matrixId)
                .orElseThrow(() -> new IllegalStateException("Exam matrix not found after save: " + matrixId));
        List<ExamMatrixRow> rows = examMatrixRepository.findRowsByMatrixId(matrixId);
        matrix.setRows(rows);
        return matrix;
    }
}
