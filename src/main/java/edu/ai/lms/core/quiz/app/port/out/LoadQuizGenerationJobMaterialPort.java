package edu.ai.lms.core.quiz.app.port.out;

import java.util.List;
import java.util.UUID;

public interface LoadQuizGenerationJobMaterialPort {
    List<UUID> findMaterialIdsByJobId(UUID jobId);
}
