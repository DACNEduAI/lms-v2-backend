package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.out.persistence.mapper.GroupLearningOutcomeJdbcMapper;
import edu.ai.lms.core.course.adapter.out.persistence.mapper.LearningOutcomeJdbcMapper;
import edu.ai.lms.core.course.app.domain.GroupLearningOutcome;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.port.out.LoadGroupLearningOutcomePort;
import edu.ai.lms.core.course.app.port.out.SaveGroupLearningOutcomePort;
import edu.ai.lms.database.GroupLearningOutcomeJdbcTable;
import edu.ai.lms.database.LearningOutcomeJdbcTable;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class GroupLearningOutcomePersistenceAdapter implements SaveGroupLearningOutcomePort, LoadGroupLearningOutcomePort {

    private final GroupLearningOutcomeRepository groupLearningOutcomeRepository;
    private final LearningOutcomeRepository learningOutcomeRepository;
    private final GroupLearningOutcomeJdbcMapper groupLearningOutcomeJdbcMapper;
    private final LearningOutcomeJdbcMapper learningOutcomeJdbcMapper;

    @Override
    public GroupLearningOutcome save(GroupLearningOutcome groupLearningOutcome) {
        GroupLearningOutcomeJdbcTable entity = groupLearningOutcomeJdbcMapper.mapToEntity(
                groupLearningOutcome, groupLearningOutcome.getCourseId());
        GroupLearningOutcomeJdbcTable saved = groupLearningOutcomeRepository.save(entity);
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

    private GroupLearningOutcome loadWithLearningOutcomes(GroupLearningOutcomeJdbcTable entity) {
        List<LearningOutcomeJdbcTable> loEntities =
                learningOutcomeRepository.findByGroupLearningOutcomeId(entity.getId());
        List<LearningOutcome> learningOutcomes = loEntities.stream()
                .map(learningOutcomeJdbcMapper::mapToLearningOutcome)
                .collect(Collectors.toList());

        GroupLearningOutcome domain = groupLearningOutcomeJdbcMapper.mapToDomain(entity);
        domain.setLearningOutcomes(learningOutcomes);
        return domain;
    }
}
