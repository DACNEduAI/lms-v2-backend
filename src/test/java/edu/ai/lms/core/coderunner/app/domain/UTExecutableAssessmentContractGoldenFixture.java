package edu.ai.lms.core.coderunner.app.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class UTExecutableAssessmentContractGoldenFixture {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesAndValidatesTheVersionTwoGoldenContract() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/contracts/code_runner_contract_v2.sample.json")) {
            assertThat(input).isNotNull();
            ExecutableAssessmentContract contract = objectMapper.readValue(input, ExecutableAssessmentContract.class);

            contract.validateForPersistence();

            assertThat(contract.effectiveExecutionLimits().wallTimeLimitMs()).isEqualTo(5_000);
            assertThat(contract.effectiveExecutionLimits().memoryLimitKb()).isEqualTo(131_072);
        }
    }
}
