package edu.ai.lms.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuizGenerationTimeConfig {

    @Bean
    public Clock quizGenerationClock() {
        return Clock.systemUTC();
    }
}
