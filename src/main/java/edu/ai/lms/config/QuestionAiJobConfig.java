package edu.ai.lms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class QuestionAiJobConfig {
    @Bean("questionAiExecutor")
    public ThreadPoolTaskExecutor questionAiExecutor(
            @Value("${quiz.question-ai.worker-count:4}") int workerCount,
            @Value("${quiz.question-ai.executor-queue-capacity:0}") int queueCapacity) {
        if (workerCount < 1 || queueCapacity < 0) {
            throw new IllegalArgumentException("Question AI worker configuration is invalid");
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(workerCount);
        executor.setMaxPoolSize(workerCount);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("question-ai-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
