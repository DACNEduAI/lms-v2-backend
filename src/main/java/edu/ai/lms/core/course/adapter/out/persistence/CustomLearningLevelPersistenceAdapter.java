package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.CustomLearningLevel;
import edu.ai.lms.core.course.app.port.out.CustomLearningLevelPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CustomLearningLevelPersistenceAdapter implements CustomLearningLevelPort {
    private final CustomLearningLevelRepository repository;

    @Override
    public List<CustomLearningLevel> findAllByCourseId(UUID courseId) {
        return assemble(courseId, repository.findAllByCourseId(courseId));
    }

    @Override
    public List<CustomLearningLevel> lockAllByCourseId(UUID courseId) {
        return assemble(courseId, repository.lockAllByCourseId(courseId));
    }

    @Override
    public Optional<CustomLearningLevel> findByCourseIdAndId(UUID courseId, UUID id) {
        return assemble(courseId, repository.findByCourseIdAndId(courseId, id).stream().toList())
                .stream().findFirst();
    }

    @Override
    public long countMatrixUsages(UUID id) {
        return repository.countMatrixUsages(id);
    }

    @Override
    public long countIncompatibleMatrixUsages(UUID id, List<UUID> learningOutcomeIds) {
        return repository.countIncompatibleMatrixUsages(id, learningOutcomeIds);
    }

    @Override
    public void insert(CustomLearningLevel level) {
        repository.insert(level);
        replaceOutcomeMappings(level.getId(), level.getLearningOutcomeIds());
    }

    @Override
    public void stageUpdate(UUID id, String temporaryValue) {
        repository.stageUpdate(id, temporaryValue);
    }

    @Override
    public void update(CustomLearningLevel level) {
        repository.update(level);
        replaceOutcomeMappings(level.getId(), level.getLearningOutcomeIds());
    }

    @Override
    public void replaceOutcomeMappings(UUID levelId, List<UUID> learningOutcomeIds) {
        repository.deleteOutcomeMappings(levelId);
        learningOutcomeIds.forEach(outcomeId -> repository.insertOutcomeMapping(levelId, outcomeId));
    }

    @Override
    public void delete(UUID id) {
        repository.delete(id);
    }

    private List<CustomLearningLevel> assemble(UUID courseId, List<CustomLearningLevel> levels) {
        Map<UUID, CustomLearningLevel> byId = levels.stream()
                .collect(Collectors.toMap(CustomLearningLevel::getId, Function.identity()));
        byId.values().forEach(level -> level.setLearningOutcomeIds(new ArrayList<>()));
        repository.findOutcomeLinksByCourseId(courseId).forEach(link -> {
            CustomLearningLevel level = byId.get(link.customLearningLevelId());
            if (level != null) level.getLearningOutcomeIds().add(link.learningOutcomeId());
        });
        return levels;
    }
}
