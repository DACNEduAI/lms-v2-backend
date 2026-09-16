package edu.ai.lms.core.learnermodel.adapter.out.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import edu.ai.lms.core.learnermodel.app.domain.LearnerInteraction;
import edu.ai.lms.core.learnermodel.app.domain.LearnerModelType;
import edu.ai.lms.core.learnermodel.app.domain.LearnerQuizHistory;
import edu.ai.lms.core.learnermodel.app.domain.LearningOutcomeTarget;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LearnerModelWebClientAdapterUT {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendsEachModelsExpectedIdentifierBaseAndMapsAktEvidenceBack() throws Exception {
        List<CapturedRequest> captured = new ArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode request = objectMapper.readTree(body);
            String path = exchange.getRequestURI().getPath();
            String model = path.split("/")[3];
            long tagId = request.path("target_tag_ids").get(0).asLong();
            JsonNode interaction = request.path("quiz_histories").get(0).path("interactions").get(0);
            boolean akt = model.startsWith("akt-");
            long questionId = akt ? interaction.path("question_id").asLong() : -1;
            captured.add(new CapturedRequest(
                    model,
                    tagId,
                    questionId,
                    interaction.has("question_id"),
                    exchange.getRequestHeaders().getFirst("X-Correlation-ID")));
            String prediction = akt
                    ? "{\"tag_id\":" + tagId + ",\"question_ids\":[" + questionId
                            + "],\"correctness_probability\":0.75,\"status\":\"PREDICTED\"}"
                    : "{\"tag_id\":" + tagId + ",\"correctness_probability\":0.75}";
            String response = "{\"model\":\"" + model + "\",\"student_id\":\""
                    + request.path("student_id").asText() + "\",\"predictions\":[" + prediction + "]}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        try {
            var adapter = new LearnerModelWebClientAdapter(
                    WebClient.builder(),
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    Duration.ofSeconds(2));
            UUID studentId = UUID.randomUUID();
            UUID questionId = UUID.randomUUID();
            var target = new LearningOutcomeTarget(UUID.randomUUID(), 12L);
            var history = new LearnerQuizHistory(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    OffsetDateTime.parse("2026-09-16T10:00:00+07:00"),
                    List.of(new LearnerInteraction(
                            questionId, 20L, true, target.id(), target.ktLearningOutcomeId(), 1)));
            MDC.put("correlation_id", "learner-model-test");

            for (LearnerModelType model : LearnerModelType.values()) {
                var predictions = adapter.predict(model, studentId, List.of(history), List.of(target));
                assertThat(predictions).singleElement().satisfies(prediction -> {
                    assertThat(prediction.ktLearningOutcomeId()).isEqualTo(12L);
                    assertThat(prediction.probability()).isEqualTo(0.75d);
                    if (model.isAkt()) {
                        assertThat(prediction.basisKtQuestionIds()).containsExactly(20L);
                    } else {
                        assertThat(prediction.basisKtQuestionIds()).isEmpty();
                    }
                });
            }
        } finally {
            MDC.remove("correlation_id");
            server.stop(0);
        }

        assertThat(captured).extracting(CapturedRequest::tagId)
                .containsExactly(11L, 11L, 12L, 11L);
        assertThat(captured).filteredOn(request -> request.model().startsWith("akt-"))
                .extracting(CapturedRequest::questionId)
                .containsExactly(20L, 19L);
        assertThat(captured).filteredOn(request -> request.model().startsWith("dkt-"))
                .allMatch(request -> !request.hasQuestionId());
        assertThat(captured).extracting(CapturedRequest::correlationId)
                .containsOnly("learner-model-test");
    }

    private record CapturedRequest(
            String model,
            long tagId,
            long questionId,
            boolean hasQuestionId,
            String correlationId) {
    }
}
