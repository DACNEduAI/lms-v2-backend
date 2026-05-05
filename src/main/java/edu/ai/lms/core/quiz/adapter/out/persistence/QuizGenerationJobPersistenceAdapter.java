package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.quiz.app.domain.QuizGenerationJob;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizGenerationJobPort;
import edu.ai.lms.core.quiz.app.port.out.SaveQuizGenerationJobPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class QuizGenerationJobPersistenceAdapter implements SaveQuizGenerationJobPort, LoadQuizGenerationJobPort {

    private final QuizGenerationJobRepository quizGenerationJobRepository;

    @Override
    public QuizGenerationJob save(QuizGenerationJob job) {
        return quizGenerationJobRepository.save(job);
    }

    @Override
    public Optional<QuizGenerationJob> getById(UUID jobId) {
        return quizGenerationJobRepository.getById(jobId);
    }
}
