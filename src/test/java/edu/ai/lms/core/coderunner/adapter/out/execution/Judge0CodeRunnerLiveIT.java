package edu.ai.lms.core.coderunner.adapter.out.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.core.coderunner.app.domain.ExecutableAssessmentContract;
import edu.ai.lms.core.coderunner.app.port.out.CodeExecutionEngine;
import edu.ai.lms.core.coderunner.app.service.CodeRunnerVerificationService;
import edu.ai.lms.core.coderunner.app.service.CppCodeRunnerHarnessBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Explicit live gate. By default this test calls the project-controlled
 * Judge0 instance on localhost and is excluded from normal *Test conventions.
 */
class Judge0CodeRunnerLiveIT {

    private static final String JUDGE0_URL = System.getProperty(
            "judge0.live.url", "http://127.0.0.1:2358");
    private static final String JUDGE0_TOKEN = System.getProperty(
            "judge0.live.token", "lms-local-judge0-token");
    private static final int JUDGE0_CPP_LANGUAGE_ID = Integer.getInteger(
            "judge0.live.cpp-language-id", 54);

    private final Judge0CodeExecutionAdapter engine = new Judge0CodeExecutionAdapter(
            WebClient.builder(),
            JUDGE0_URL,
            JUDGE0_TOKEN,
            "X-Judge0-Token",
            Duration.ofSeconds(20));
    private final CppCodeRunnerHarnessBuilder harnessBuilder =
            new CppCodeRunnerHarnessBuilder(new ObjectMapper());

    @Test
    void compilesAndExecutesARealCplusplusReferenceAndRejectsAWrongSolution() {
        ExecutableAssessmentContract contract = contract();
        var verificationService = new CodeRunnerVerificationService(
                engine,
                harnessBuilder,
                JUDGE0_CPP_LANGUAGE_ID,
                Duration.ofSeconds(60),
                Duration.ofMillis(350));

        var verified = verificationService.verifyReference(contract);
        assertThat(verified.passed()).isTrue();
        assertThat(verified.passedTests()).isEqualTo(2);

        String wrongHarness = harnessBuilder.buildCase(
                contract,
                "bool isEven(int value) { return true; }",
                contract.privateGrading().hiddenTests().getFirst());
        var wrongTicket = engine.submit(request(wrongHarness));
        assertThat(await(wrongTicket.engineSubmissionId()).verdict())
                .isEqualTo(CodeExecutionEngine.Verdict.WRONG_ANSWER);
    }

    private CodeExecutionEngine.ExecutionRequest request(String source) {
        return new CodeExecutionEngine.ExecutionRequest(
                source,
                JUDGE0_CPP_LANGUAGE_ID,
                "PASS\n",
                2_000,
                5_000,
                131_072,
                64,
                16);
    }

    private CodeExecutionEngine.ExecutionResult await(String token) {
        Instant deadline = Instant.now().plusSeconds(60);
        while (Instant.now().isBefore(deadline)) {
            var result = engine.getResult(token);
            if (result.state() == CodeExecutionEngine.EngineState.FINISHED) {
                return result;
            }
            try {
                Thread.sleep(350);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
        }
        throw new IllegalStateException("Judge0 live test timed out");
    }

    private ExecutableAssessmentContract contract() {
        ObjectMapper mapper = new ObjectMapper();
        var integer = new ExecutableAssessmentContract.VersionedRef("integer", "1.0.0");
        var bool = new ExecutableAssessmentContract.VersionedRef("boolean", "1.0.0");
        var signature = new ExecutableAssessmentContract.FunctionSignature(
                bool,
                List.of(new ExecutableAssessmentContract.ArgumentSpec("value", integer, Map.of())));
        Map<String, com.fasterxml.jackson.databind.JsonNode> publicArguments = new LinkedHashMap<>();
        publicArguments.put("value", mapper.valueToTree(2));
        Map<String, com.fasterxml.jackson.databind.JsonNode> hiddenArguments = new LinkedHashMap<>();
        hiddenArguments.put("value", mapper.valueToTree(-3));
        var publicTest = new ExecutableAssessmentContract.TestCase(
                "public_001", "positive", "PUBLIC", publicArguments,
                mapper.valueToTree(true), 0);
        var hiddenTest = new ExecutableAssessmentContract.TestCase(
                "hidden_001", "negative", "HIDDEN", hiddenArguments,
                mapper.valueToTree(false), 1);
        var runtime = new ExecutableAssessmentContract.RuntimeContract(
                new ExecutableAssessmentContract.VersionedRef("dsa_cpp17", "1.0.0"),
                new ExecutableAssessmentContract.VersionedRef("scalar_function", "1.0.0"),
                new ExecutableAssessmentContract.VersionedRef("exact_typed", "1.0.0"),
                "isEven",
                signature,
                "bool isEven(int value) { return false; }",
                List.of(publicTest),
                List.of("\\bsystem\\s*\\("));
        return new ExecutableAssessmentContract(
                "2.0.0", "is_even_live_v1", "1.0.0", "READY_FOR_REVIEW",
                "Even number", "Return true if value is even.", runtime,
                new ExecutableAssessmentContract.PrivateGrading(
                        "bool isEven(int value) { return value % 2 == 0; }",
                        List.of(hiddenTest)));
    }
}
