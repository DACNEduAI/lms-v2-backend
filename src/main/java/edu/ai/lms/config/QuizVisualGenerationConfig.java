package edu.ai.lms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class QuizVisualGenerationConfig {
    @Bean("quizVisualGenerationExecutor")
    public ThreadPoolTaskExecutor quizVisualGenerationExecutor(
            @Value("${quiz.visual.max-concurrency:3}") int maxConcurrency) {
        if (maxConcurrency < 1) {
            throw new IllegalArgumentException("Quiz visual concurrency must be positive");
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(maxConcurrency);
        executor.setMaxPoolSize(maxConcurrency);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("quiz-visual-");
        executor.initialize();
        return executor;
    }
}
