package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.port.out.LoadGroupLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.SaveGroupLearningOutcomePort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class GroupLearningOutcomePersistenceAdapter implements SaveGroupLearningOutcomePort, LoadGroupLearningOutcomePort {

    private final GroupLearningOutcomeRepository groupLearningOutcomeRepository;
    private final LearningOutcomeRepository learningOutcomeRepository;

    @Override
    public GroupLearningOutcome save(GroupLearningOutcome groupLearningOutcome) {
    GroupLearningOutcome saved = groupLearningOutcomeRepository.save(groupLearningOutcome);
        return loadWithLearningOutcomes(saved);
    }

    @Override
    public Optional<GroupLearningOutcome> findById(UUID id) {
        return groupLearningOutcomeRepository.findById(id)
                .map(this::loadWithLearningOutcomes);
    }

    @Override
    public List<GroupLearningOutcome> findByCourseId(UUID courseId) {
        return groupLearningOutcomeRepository.findByCourseId(courseId).stream()
                .map(this::loadWithLearningOutcomes)
                .collect(Collectors.toList());
    }

    private GroupLearningOutcome loadWithLearningOutcomes(GroupLearningOutcome entity) {
        List<LearningOutcome> learningOutcomes =
                learningOutcomeRepository.findByGroupLearningOutcomeId(entity.getId());
        entity.setLearningOutcomes(learningOutcomes.stream().collect(Collectors.toList()));
        return entity;
    }
}
