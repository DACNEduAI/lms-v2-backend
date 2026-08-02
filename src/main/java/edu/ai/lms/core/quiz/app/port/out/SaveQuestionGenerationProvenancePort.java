package edu.ai.lms.core.quiz.app.port.out;

import edu.ai.lms.core.quiz.app.domain.QuestionGenerationProvenance;

public interface SaveQuestionGenerationProvenancePort {
    void save(QuestionGenerationProvenance provenance);
}
