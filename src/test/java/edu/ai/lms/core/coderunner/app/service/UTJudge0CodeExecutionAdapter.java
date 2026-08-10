package edu.ai.lms.core.coderunner.app.service;

import edu.ai.lms.core.coderunner.adapter.out.execution.Judge0CodeExecutionAdapter;
import edu.ai.lms.core.coderunner.app.port.out.CodeExecutionEngine;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UTJudge0CodeExecutionAdapter {
    @Test
    void submitWithoutExpectedOutputReturnsOpaqueJudgeToken() {
        var adapter = adapter("{\"token\":\"opaque-123\"}");
        var ticket = adapter.submit(new CodeExecutionEngine.ExecutionRequest(
                "int main(){}", 54, null, 2000, 5000, 131072, 64, 16));
        assertEquals("opaque-123", ticket.engineSubmissionId());
    }

    @Test
    void normalizesCompileErrorWithoutPollingInsideAdapter() {
        String body = "{\"status\":{\"id\":6},\"compile_output\":\"ZXJyb3I=\","
                + "\"time\":\"0.01\",\"memory\":1024}";
        var result = adapter(body).getResult("opaque-123");
        assertEquals(CodeExecutionEngine.EngineState.FINISHED, result.state());
        assertEquals(CodeExecutionEngine.Verdict.COMPILE_ERROR, result.verdict());
        assertEquals("error", result.compileOutput());
        assertEquals(10.0, result.timeMs());
    }

    @Test
    void queuedSubmissionHasNoVerdictYet() {
        var result = adapter("{\"status\":{\"id\":1}}").getResult("opaque-123");
        assertEquals(CodeExecutionEngine.EngineState.QUEUED, result.state());
        assertNull(result.verdict());
    }

    @Test
    void decodesBase64ThatJudge0WrapsAcrossLines() {
        var result = adapter("{\"status\":{\"id\":3},\"stdout\":\"Qjox\\n\"}")
                .getResult("opaque-123");

        assertEquals("B:1", result.stdout());
    }

    private Judge0CodeExecutionAdapter adapter(String responseJson) {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body(responseJson)
                        .build()));
        return new Judge0CodeExecutionAdapter(
                builder, "http://judge0.invalid", "", "X-Judge0-Token", Duration.ofSeconds(1));
    }
}
