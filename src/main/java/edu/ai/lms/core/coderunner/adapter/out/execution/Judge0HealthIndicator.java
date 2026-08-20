package edu.ai.lms.core.coderunner.adapter.out.execution;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Makes Judge0 availability observable through the standard actuator health endpoint. */
@Component("judge0")
public class Judge0HealthIndicator implements HealthIndicator {
    private final WebClient client;
    private final String authHeader;
    private final String authToken;
    private final Duration timeout;

    public Judge0HealthIndicator(
            WebClient.Builder builder,
            @Value("${code-runner.judge0.base-url:http://localhost:2358}") String baseUrl,
            @Value("${code-runner.judge0.auth-header:X-Judge0-Token}") String authHeader,
            @Value("${code-runner.judge0.auth-token:}") String authToken,
            @Value("${code-runner.judge0.health-timeout:3s}") Duration timeout) {
        this.client = builder.baseUrl(baseUrl).build();
        this.authHeader = authHeader;
        this.authToken = authToken;
        this.timeout = timeout;
    }

    @Override
    public Health health() {
        try {
            WebClient.RequestHeadersSpec<?> request = client.get().uri("/about");
            if (!authToken.isBlank()) request = request.header(authHeader, authToken);
            int status = request.exchangeToMono(response ->
                    reactor.core.publisher.Mono.just(response.statusCode().value()))
                    .block(timeout);
            if (status >= 200 && status < 300) {
                return Health.up().withDetail("service", "judge0").build();
            }
            return Health.down().withDetail("service", "judge0").withDetail("httpStatus", status).build();
        } catch (RuntimeException unavailable) {
            return Health.down().withDetail("service", "judge0")
                    .withDetail("errorType", unavailable.getClass().getSimpleName()).build();
        }
    }
}
