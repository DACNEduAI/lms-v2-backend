package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.out.persistence.mapper.LearningOutcomeJdbcMapper;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.port.out.DeleteLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.LoadLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.SaveLearningOutcomePort;
import edu.ai.lms.database.LearningOutcomeJdbcTable;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class LearningOutcomePersistenceAdapter implements SaveLearningOutcomePort, LoadLearningOutcomePort, DeleteLearningOutcomePort {

    private final LearningOutcomeRepository learningOutcomeRepository;
    private final LearningOutcomeJdbcMapper learningOutcomeJdbcMapper;

    @Override
    public LearningOutcome save(LearningOutcome learningOutcome) {
        LearningOutcomeJdbcTable entity = learningOutcomeJdbcMapper.mapToEntity(
                learningOutcome, learningOutcome.getCourseId());
        LearningOutcomeJdbcTable saved = learningOutcomeRepository.save(entity);
        return learningOutcomeJdbcMapper.mapToLearningOutcome(saved);
    }

    @Override
    public void assignToGroup(UUID learningOutcomeId, UUID courseId, UUID groupLearningOutcomeId) {
        learningOutcomeRepository.updateGroupLearningOutcomeId(learningOutcomeId, courseId, groupLearningOutcomeId);
    }

    @Override
    public void removeFromGroup(UUID learningOutcomeId, UUID courseId) {
        learningOutcomeRepository.removeFromGroup(learningOutcomeId, courseId);
    }

    @Override
    public Optional<LearningOutcome> findById(UUID id) {
        return learningOutcomeRepository.findById(id)
                .map(learningOutcomeJdbcMapper::mapToLearningOutcome);
    }

    @Override
    public List<LearningOutcome> findByCourseId(UUID courseId) {
        return learningOutcomeRepository.findByCourseId(courseId).stream()
                .map(learningOutcomeJdbcMapper::mapToLearningOutcome)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        learningOutcomeRepository.deleteById(id);
    }

    @Override
    public void deleteAllByIds(List<UUID> ids) {
        learningOutcomeRepository.deleteAllById(ids);
    }
}
