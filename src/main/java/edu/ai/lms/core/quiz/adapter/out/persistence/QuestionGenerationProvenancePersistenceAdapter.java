package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.app.domain.QuestionGenerationProvenance;
import edu.ai.lms.core.quiz.app.port.out.LoadQuestionGenerationProvenancePort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuestionGenerationProvenancePort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class QuestionGenerationProvenancePersistenceAdapter
        implements LoadQuestionGenerationProvenancePort, SaveQuestionGenerationProvenancePort {
    private final QuestionGenerationProvenanceRepository repository;

    @Override
    public Optional<QuestionGenerationProvenance> findByQuestionId(UUID questionId) {
        return repository.findByQuestionId(questionId);
    }

    @Override
    public void save(QuestionGenerationProvenance provenance) {
        if (repository.insert(provenance) != 1) {
            throw new IllegalStateException("Could not persist question generation provenance");
        }
    }
}
