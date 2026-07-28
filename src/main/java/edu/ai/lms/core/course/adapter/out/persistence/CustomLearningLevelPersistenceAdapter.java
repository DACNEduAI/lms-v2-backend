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
import edu.ai.lms.core.course.app.domain.CustomLearningLevelOutcomeLink;
import edu.ai.lms.core.course.app.port.out.CustomLearningLevelPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CustomLearningLevelPersistenceAdapter implements CustomLearningLevelPort {
    private final CustomLearningLevelRepository repository;

    @Override
    public List<CustomLearningLevel> findAllByMatrixId(UUID matrixId) {
        return assemble(matrixId, repository.findAllByMatrixId(matrixId));
    }

    @Override
    public List<CustomLearningLevel> lockAllByMatrixId(UUID matrixId) {
        return assemble(matrixId, repository.lockAllByMatrixId(matrixId));
    }

    @Override
    public Optional<CustomLearningLevel> findByMatrixIdAndId(UUID matrixId, UUID id) {
        return assemble(matrixId, repository.findByMatrixIdAndId(matrixId, id).stream().toList())
                .stream().findFirst();
    }

    @Override
    public void insert(CustomLearningLevel level) {
        repository.insert(level);
        repository.insertAllOutcomeMappings(level.getId(), level.getMatrixId());
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
        repository.clearOutcomeSelections(levelId);
        if (learningOutcomeIds != null && !learningOutcomeIds.isEmpty()) {
            repository.selectOutcomeMappings(levelId, learningOutcomeIds);
        }
    }

    @Override
    public void addUnselectedOutcomesToCourseMatrices(UUID courseId, List<UUID> learningOutcomeIds) {
        if (learningOutcomeIds != null && !learningOutcomeIds.isEmpty()) {
            repository.insertOutcomeMappingsForCourseMatrices(courseId, learningOutcomeIds);
        }
    }

    @Override
    public void delete(UUID id) {
        repository.delete(id);
    }

    private List<CustomLearningLevel> assemble(UUID matrixId, List<CustomLearningLevel> levels) {
        Map<UUID, CustomLearningLevel> byId = levels.stream()
                .collect(Collectors.toMap(CustomLearningLevel::getId, Function.identity()));
        byId.values().forEach(level -> level.setLearningOutcomeIds(new ArrayList<>()));
        repository.findOutcomeLinksByMatrixId(matrixId).stream()
                .filter(CustomLearningLevelOutcomeLink::selected).forEach(link -> {
            CustomLearningLevel level = byId.get(link.customLearningLevelId());
            if (level != null) level.getLearningOutcomeIds().add(link.learningOutcomeId());
        });
        return levels;
    }
}
