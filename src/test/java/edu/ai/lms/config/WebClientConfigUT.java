package edu.ai.lms.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WebClientConfigUT {

    @Test
    void aiWebClientSendsConfiguredServiceToken() {
        AtomicReference<String> authorization = new AtomicReference<>();
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            authorization.set(request.headers().getFirst(HttpHeaders.AUTHORIZATION));
            return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        });
        WebClientConfig config = new WebClientConfig();
        ReflectionTestUtils.setField(config, "aiServiceUrl", "http://localhost:8000");
        ReflectionTestUtils.setField(config, "aiServiceToken", "test-service-token");
        ReflectionTestUtils.setField(config, "responseTimeoutSeconds", 30L);

        config.aiWebClient(builder).get().uri("/api/ingest")
                .retrieve().toBodilessEntity().block();

        assertEquals("Bearer test-service-token", authorization.get());
    }

    @Test
    void aiWebClientPropagatesExplicitCorrelationId() {
        AtomicReference<String> correlationId = new AtomicReference<>();
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            correlationId.set(request.headers().getFirst(CorrelationIdFilter.HEADER_NAME));
            return Mono.just(ClientResponse.create(HttpStatus.ACCEPTED).build());
        });
        WebClientConfig config = configured();

        config.aiWebClient(builder).post().uri("/quiz-gen/drafts/generate")
                .header(CorrelationIdFilter.HEADER_NAME, "batch-456")
                .retrieve().toBodilessEntity().block();

        assertEquals("batch-456", correlationId.get());
    }

    @Test
    void aiWebClientGeneratesCorrelationIdWhenMissing() {
        AtomicReference<String> correlationId = new AtomicReference<>();
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            correlationId.set(request.headers().getFirst(CorrelationIdFilter.HEADER_NAME));
            return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        });

        configured().aiWebClient(builder).get().uri("/api/ingest")
                .retrieve().toBodilessEntity().block();

        assertNotNull(correlationId.get());
        assertEquals(36, correlationId.get().length());
    }

    private WebClientConfig configured() {
        WebClientConfig config = new WebClientConfig();
        ReflectionTestUtils.setField(config, "aiServiceUrl", "http://localhost:8000");
        ReflectionTestUtils.setField(config, "aiServiceToken", "test-service-token");
        ReflectionTestUtils.setField(config, "responseTimeoutSeconds", 30L);
        return config;
    }
}
