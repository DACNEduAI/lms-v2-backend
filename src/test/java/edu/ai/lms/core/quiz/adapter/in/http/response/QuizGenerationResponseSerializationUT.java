package edu.ai.lms.core.quiz.adapter.in.http.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.ai.lms.core.quiz.app.domain.QuizGenerationStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuizGenerationResponseSerializationUT {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void serializesQuizGenerationInstantsAsRfc3339UtcStrings() throws Exception {
        Instant createdAt = Instant.parse("2026-08-13T19:36:58Z");
        Instant updatedAt = Instant.parse("2026-08-13T19:37:05Z");
        QuizGenerationStatusResponse response = QuizGenerationStatusResponse.builder()
                .jobId(UUID.randomUUID())
                .status(QuizGenerationStatus.PROCESSING)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertThat(json)
                .contains("\"createdAt\":\"2026-08-13T19:36:58Z\"")
                .contains("\"updatedAt\":\"2026-08-13T19:37:05Z\"");
    }
}
