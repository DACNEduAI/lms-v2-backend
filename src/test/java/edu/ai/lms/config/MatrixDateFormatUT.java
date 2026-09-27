package edu.ai.lms.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import edu.ai.lms.core.course.app.service.matrix.MatrixService;

class MatrixDateFormatUT {

    private final ObjectMapper objectMapper = objectMapper();

    @Test
    void serializesMatrixDatesAsIso8601WhileKeepingTheGlobalFormatForOtherDtos() throws Exception {
        LocalDateTime timestamp = LocalDateTime.of(2026, 9, 26, 0, 0);
        MatrixService.MatrixDetail matrix = new MatrixService.MatrixDetail(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), timestamp,
                "Matrix", null, MatrixService.MatrixMode.MANUAL, null,
                MatrixService.LifecycleStatus.PUBLISHED, 1, UUID.randomUUID(), timestamp,
                List.of(), List.of(), List.of(), timestamp, timestamp);

        var matrixJson = objectMapper.readTree(objectMapper.writeValueAsString(matrix));
        assertEquals("2026-09-26T00:00:00", matrixJson.get("ccctPublishedAt").asText());
        assertEquals("2026-09-26T00:00:00", matrixJson.get("publishedAt").asText());
        assertEquals("2026-09-26T00:00:00", matrixJson.get("createdAt").asText());
        assertEquals("2026-09-26T00:00:00", matrixJson.get("updatedAt").asText());

        MatrixService.MatrixSummary summary = new MatrixService.MatrixSummary(
                UUID.randomUUID(), UUID.randomUUID(), "Matrix", null,
                MatrixService.MatrixMode.MANUAL, null, MatrixService.LifecycleStatus.PUBLISHED,
                1, UUID.randomUUID(), timestamp, 1, 1, timestamp, timestamp);
        var summaryJson = objectMapper.readTree(objectMapper.writeValueAsString(summary));
        assertEquals("2026-09-26T00:00:00", summaryJson.get("publishedAt").asText());
        assertEquals("2026-09-26T00:00:00", summaryJson.get("createdAt").asText());
        assertEquals("2026-09-26T00:00:00", summaryJson.get("updatedAt").asText());

        var nonMatrixJson = objectMapper.readTree(objectMapper.writeValueAsString(new NonMatrixDate(timestamp)));
        assertEquals("26-09-2026 00:00:00", nonMatrixJson.get("timestamp").asText());
    }

    @Test
    void serializesMatrixRowsWithChapterFieldsAndNoLegacyTopicGroupFields() throws Exception {
        MatrixService.Row row = new MatrixService.Row(
                UUID.randomUUID(), UUID.randomUUID(), "Trees", UUID.randomUUID(), "binaryTrees",
                "Traverse a binary tree", "binaryTrees.traversal", UUID.randomUUID(), "LO1", 0, List.of());

        var rowJson = objectMapper.readTree(objectMapper.writeValueAsString(row));
        assertEquals("binaryTrees", rowJson.get("chapterCode").asText());
        assertEquals("Traverse a binary tree", rowJson.get("contentDetail").asText());
        assertFalse(rowJson.has("topicGroupCode"));
        assertFalse(rowJson.has("topicGroupName"));
    }

    private static ObjectMapper objectMapper() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonDateFormatConfig().dateTimeCustomizer(
                new AppDateFormatProperties("dd-MM-yyyy", "dd-MM-yyyy HH:mm:ss")).customize(builder);
        return builder.build();
    }

    private record NonMatrixDate(LocalDateTime timestamp) { }
}
