package edu.ai.lms.core.course.adapter.out.persistence;

import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import edu.ai.lms.core.course.app.domain.CustomLearningLevel;

class CustomLearningLevelPersistenceAdapterUT {

    @Test
    void insertMaterializesAllCandidatesThenSelectsSubmittedOutcomes() {
        CustomLearningLevelRepository repository = mock(CustomLearningLevelRepository.class);
        CustomLearningLevelPersistenceAdapter adapter =
                new CustomLearningLevelPersistenceAdapter(repository);
        UUID matrixId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        CustomLearningLevel level = CustomLearningLevel.builder()
                .id(levelId).matrixId(matrixId)
                .learningOutcomeIds(List.of(first, second))
                .code("CLL1").name("Understand").build();

        adapter.insert(level);

        var order = inOrder(repository);
        order.verify(repository).insert(level);
        order.verify(repository).insertAllOutcomeMappings(levelId, matrixId);
        order.verify(repository).clearOutcomeSelections(levelId);
        order.verify(repository).selectOutcomeMappings(levelId, List.of(first, second));
        verify(repository, never()).delete(any());
    }

    @Test
    void updateTogglesSelectionWithoutDeletingJunctionRows() {
        CustomLearningLevelRepository repository = mock(CustomLearningLevelRepository.class);
        CustomLearningLevelPersistenceAdapter adapter =
                new CustomLearningLevelPersistenceAdapter(repository);
        UUID selected = UUID.randomUUID();
        CustomLearningLevel level = CustomLearningLevel.builder()
                .id(UUID.randomUUID()).matrixId(UUID.randomUUID())
                .learningOutcomeIds(List.of(selected)).build();

        adapter.update(level);

        verify(repository).update(level);
        verify(repository).clearOutcomeSelections(level.getId());
        verify(repository).selectOutcomeMappings(level.getId(), List.of(selected));
        verify(repository, never()).delete(any());
    }
}
