package edu.ai.lms.core.course.adapter.out.persistence;

import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import edu.ai.lms.core.course.app.domain.ExamMatrixRow;

class ExamMatrixPersistenceAdapterUT {

    @Test
    void replacingRowReconcilesCandidatesAndTogglesSelectionWithoutDeletingMappings() {
        ExamMatrixRepository repository = mock(ExamMatrixRepository.class);
        ExamMatrixPersistenceAdapter adapter = new ExamMatrixPersistenceAdapter(repository);
        UUID rowId = UUID.randomUUID();
        UUID selected = UUID.randomUUID();
        ExamMatrixRow row = ExamMatrixRow.builder()
                .id(rowId).matrixId(UUID.randomUUID()).chapterId(UUID.randomUUID())
                .learningOutcomeIds(List.of(selected)).levelCounts(List.of()).build();

        adapter.replaceRow(row);

        var order = inOrder(repository);
        order.verify(repository).updateRow(row);
        order.verify(repository).reconcileRowOutcomeCandidates(rowId);
        order.verify(repository).insertRowOutcomeCandidates(rowId);
        order.verify(repository).clearRowOutcomeSelections(rowId);
        order.verify(repository).selectRowOutcomes(rowId, List.of(selected));
        verify(repository, never()).deleteRow(any(), any());
    }
}
