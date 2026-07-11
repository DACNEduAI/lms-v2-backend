package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationJobMaterialPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizGenerationJobMaterialPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QuizGenerationJobMaterialPersistenceAdapter 
        implements LoadQuizGenerationJobMaterialPort, SaveQuizGenerationJobMaterialPort {

    private final QuizGenerationJobMaterialRepository repository;

    @Override
    public List<UUID> findMaterialIdsByJobId(UUID jobId) {
        return repository.findMaterialIdsByJobId(jobId);
    }

    @Override
    public void addMaterials(UUID jobId, List<UUID> materialIds) {
        repository.deleteByJobId(jobId);
        if (materialIds != null) {
            for (UUID materialId : materialIds) {
                repository.insert(jobId, materialId);
            }
        }
    }
}
