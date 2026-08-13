package edu.ai.lms.config;

import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.MDC;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class WebClientConfig {

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${quiz.generation.ai.token:}")
    private String aiServiceToken;

    @Value("${ai.service.response-timeout-seconds:900}")
    private long responseTimeoutSeconds;

    @Bean
    public WebClient aiWebClient(WebClient.Builder webClientBuilder) {
        // Cấu hình HttpClient với Netty để thiết lập Timeout chuyên sâu
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(responseTimeoutSeconds))
                .doOnConnected(conn -> 
                        conn.addHandlerLast(new ReadTimeoutHandler(responseTimeoutSeconds, TimeUnit.SECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(responseTimeoutSeconds, TimeUnit.SECONDS)));

        WebClient.Builder builder = webClientBuilder
                .baseUrl(aiServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(aiRequestLoggingFilter());
        if (aiServiceToken != null && !aiServiceToken.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + aiServiceToken);
        } else {
            log.warn("AI_SERVICE_TOKEN is not configured; protected AI service calls will be rejected");
        }
        return builder.build();
    }

    ExchangeFilterFunction aiRequestLoggingFilter() {
        return (request, next) -> {
            String correlationId = CorrelationIdFilter.normalize(
                    request.headers().getFirst(CorrelationIdFilter.HEADER_NAME));
            if (correlationId == null) {
                correlationId = CorrelationIdFilter.normalize(MDC.get(CorrelationIdFilter.MDC_KEY));
            }
            if (correlationId == null) correlationId = UUID.randomUUID().toString();

            String resolvedCorrelationId = correlationId;
            ClientRequest correlated = ClientRequest.from(request)
                    .headers(headers -> headers.set(CorrelationIdFilter.HEADER_NAME, resolvedCorrelationId))
                    .build();
            long startedAt = System.nanoTime();
            String method = correlated.method().name();
            String path = correlated.url().getPath();
            String finalCorrelationId = resolvedCorrelationId;

            log.info("event=ai.http.request.started correlation_id={} method={} path={}",
                    finalCorrelationId, method, path);
            return next.exchange(correlated)
                    .doOnNext(response -> {
                        long duration = elapsedMillis(startedAt);
                        if (response.statusCode().isError()) {
                            log.warn("event=ai.http.request.completed correlation_id={} method={} path={} status={} duration_ms={}",
                                    finalCorrelationId, method, path, response.statusCode().value(), duration);
                        } else {
                            log.info("event=ai.http.request.completed correlation_id={} method={} path={} status={} duration_ms={}",
                                    finalCorrelationId, method, path, response.statusCode().value(), duration);
                        }
                    })
                    .doOnError(exception -> log.error(
                            "event=ai.http.request.failed correlation_id={} method={} path={} duration_ms={} error_type={}",
                            finalCorrelationId, method, path, elapsedMillis(startedAt),
                            exception.getClass().getSimpleName(), exception));
        };
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
