package edu.ai.lms.core.coderunner.adapter.out.execution;

import com.fasterxml.jackson.databind.JsonNode;
import edu.ai.lms.core.coderunner.app.port.out.CodeExecutionEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
public class Judge0CodeExecutionAdapter implements CodeExecutionEngine {

    private final WebClient client;
    private final Duration requestTimeout;

    public Judge0CodeExecutionAdapter(
            WebClient.Builder builder,
            @Value("${code-runner.judge0.base-url:http://localhost:2358}") String baseUrl,
            @Value("${code-runner.judge0.auth-token:}") String authToken,
            @Value("${code-runner.judge0.auth-header:X-Judge0-Token}") String authHeader,
            @Value("${code-runner.judge0.request-timeout:15s}") Duration requestTimeout) {
        WebClient.Builder configured = builder.baseUrl(baseUrl);
        if (authToken != null && !authToken.isBlank()) {
            configured.defaultHeader(authHeader, authToken);
        }
        this.client = configured.build();
        this.requestTimeout = requestTimeout;
    }

    @Override
    public ExecutionTicket submit(ExecutionRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("source_code", base64(request.sourceCode()));
        body.put("language_id", request.languageId());
        if (request.expectedOutput() != null) {
            body.put("expected_output", base64(request.expectedOutput()));
        }
        body.put("cpu_time_limit", request.cpuTimeMs() / 1000.0);
        body.put("wall_time_limit", request.wallTimeMs() / 1000.0);
        body.put("memory_limit", request.memoryKb());
        body.put("max_file_size", request.outputLimitKb());
        body.put("max_processes_and_or_threads", request.maxProcessesAndThreads());
        // Judge0 1.13.1's bundled isolate uses cgroup v1. Docker Desktop exposes
        // cgroup v2, so per-process RLIMITs keep the sandbox functional on the
        // supported local-development deployment without weakening isolation.
        body.put("enable_per_process_and_thread_time_limit", true);
        body.put("enable_per_process_and_thread_memory_limit", true);
        body.put("enable_network", false);

        JsonNode response = bounded(client.post()
                .uri(uri -> uri.path("/submissions")
                        .queryParam("base64_encoded", true)
                        .queryParam("wait", false)
                        .build())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)).block();
        String token = response == null ? null : response.path("token").asText(null);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Judge0 did not return a submission token");
        }
        return new ExecutionTicket(token);
    }

    @Override
    public ExecutionResult getResult(String engineSubmissionId) {
        JsonNode node = bounded(client.get()
                .uri(uri -> uri.path("/submissions/{token}")
                        .queryParam("base64_encoded", true)
                        .queryParam("fields", "stdout,stderr,compile_output,message,status,time,memory")
                        .build(engineSubmissionId))
                .header(HttpHeaders.ACCEPT, "application/json")
                .retrieve()
                .bodyToMono(JsonNode.class)).block();
        if (node == null) {
            throw new IllegalStateException("Judge0 returned an empty response");
        }
        int statusId = node.path("status").path("id").asInt(13);
        EngineState state = statusId == 1
                ? EngineState.QUEUED
                : statusId == 2 ? EngineState.PROCESSING : EngineState.FINISHED;
        Verdict verdict = state == EngineState.FINISHED ? verdict(statusId, node) : null;
        return new ExecutionResult(
                state,
                verdict,
                decoded(node, "stdout"),
                decoded(node, "compile_output"),
                decoded(node, "stderr"),
                secondsToMilliseconds(node.path("time").asText(null)),
                node.path("memory").isNumber() ? node.path("memory").asLong() : null,
                decoded(node, "message"));
    }

    private Verdict verdict(int statusId, JsonNode node) {
        if (statusId == 8 || statusId == 11 && containsFileLimitDiagnostic(node)) {
            return Verdict.OUTPUT_LIMIT_EXCEEDED;
        }
        return switch (statusId) {
            case 3 -> Verdict.ACCEPTED;
            case 4 -> Verdict.WRONG_ANSWER;
            case 5 -> Verdict.TIME_LIMIT_EXCEEDED;
            case 6 -> Verdict.COMPILE_ERROR;
            case 7, 9, 10, 11, 12 -> Verdict.RUNTIME_ERROR;
            default -> Verdict.INTERNAL_ERROR;
        };
    }

    private boolean containsFileLimitDiagnostic(JsonNode node) {
        String combined = String.join(" ",
                value(node, "stderr"), value(node, "message"), value(node, "compile_output"));
        return combined.toLowerCase().contains("file size limit exceeded");
    }

    private String value(JsonNode node, String field) {
        String decoded = decoded(node, field);
        return decoded == null ? "" : decoded;
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decoded(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            // Judge0 may wrap Base64 output across lines. The MIME decoder
            // accepts those line breaks while still returning the exact bytes.
            return new String(Base64.getMimeDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return value;
        }
    }

    private static Double secondsToMilliseconds(String seconds) {
        if (seconds == null || seconds.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(seconds) * 1000.0;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Mono<JsonNode> bounded(Mono<JsonNode> request) {
        return request.timeout(requestTimeout)
                .retryWhen(Retry.backoff(1, Duration.ofMillis(150)).filter(this::retryable));
    }

    private boolean retryable(Throwable error) {
        return error instanceof TimeoutException
                || error instanceof WebClientRequestException
                || error instanceof WebClientResponseException response
                && response.getStatusCode().is5xxServerError();
    }
}
