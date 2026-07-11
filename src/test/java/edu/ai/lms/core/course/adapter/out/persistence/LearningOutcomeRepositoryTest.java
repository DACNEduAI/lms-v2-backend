package edu.ai.lms.core.course.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.ai.lms.core.course.app.domain.LearningOutcome;

class LearningOutcomeRepositoryTest {

    @Test
    @DisplayName("Should assign next prefix number when saving grouped learning outcome without prefix")
    void saveGroupedLearningOutcomeWithoutPrefixAssignsNextPrefixNumber() {
        UUID groupId = UUID.randomUUID();
        FakeLearningOutcomeRepository repository = new FakeLearningOutcomeRepository(3);
        LearningOutcome learningOutcome = LearningOutcome.builder()
                .description("New grouped learning outcome")
                .groupLearningOutcomeId(groupId)
                .build();

        LearningOutcome saved = repository.save(learningOutcome);

        assertEquals(4, saved.getPrefixNumber());
        assertEquals(4, saved.getSortOrder());
    }

    private static class FakeLearningOutcomeRepository implements LearningOutcomeRepository {
        private final int maxSortOrder;
        private final List<LearningOutcome> savedLearningOutcomes = new ArrayList<>();

        private FakeLearningOutcomeRepository(int maxSortOrder) {
            this.maxSortOrder = maxSortOrder;
        }

        @Override
        public Optional<LearningOutcome> findById(UUID id) {
            return savedLearningOutcomes.stream()
                    .filter(learningOutcome -> id.equals(learningOutcome.getId()))
                    .findFirst();
        }

        @Override
        public int insert(LearningOutcome entity) {
            savedLearningOutcomes.add(entity);
            return 1;
        }

        @Override
        public int update(LearningOutcome entity) {
            return 0;
        }

        @Override
        public void deleteById(UUID id) {
        }

        @Override
        public void deleteAllById(List<UUID> ids) {
        }

        @Override
        public List<LearningOutcome> findByCourseId(UUID courseId) {
            return List.of();
        }

        @Override
        public List<LearningOutcome> findByGroupLearningOutcomeId(UUID groupId) {
            return List.of();
        }

        @Override
        public List<LearningOutcome> findUngroupedByCourseId(UUID courseId) {
            return List.of();
        }

        @Override
        public void updateGroupLearningOutcomeId(UUID id, UUID courseId, UUID groupId) {
        }

        @Override
        public void updateTreePosition(UUID id, UUID courseId, UUID groupId, int prefixNumber, int sortOrder) {
        }

        @Override
        public int findMaxSortOrderByGroupId(UUID groupId) {
            return maxSortOrder;
        }

        @Override
        public void removeFromGroup(UUID id, UUID courseId) {
        }

        @Override
        public void deleteByCourseId(UUID courseId) {
        }
    }
}
