package edu.ai.lms.core.coderunner.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.core.coderunner.app.domain.ExecutableAssessmentContract;
import edu.ai.lms.core.coderunner.app.domain.StoredCodeRunnerContract;
import edu.ai.lms.core.coderunner.app.port.out.LoadCodeRunnerContractPort;
import edu.ai.lms.core.coderunner.app.port.out.SaveCodeRunnerContractPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CodeRunnerContractPersistenceAdapter
        implements LoadCodeRunnerContractPort, SaveCodeRunnerContractPort {

    private final CodeRunnerContractRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public UUID saveRevision(UUID questionId, ExecutableAssessmentContract contract) {
        contract.validateForPersistence();
        int revision = repository.nextRevision(questionId);
        UUID configId = UUID.randomUUID();
        var runtime = contract.runtime();
        repository.insertConfig(
                configId,
                questionId,
                contract.schemaVersion(),
                contract.contractId(),
                contract.contractVersion(),
                contract.status(),
                contract.title(),
                contract.statement(),
                runtime.executionProfile().id(),
                runtime.executionProfile().version(),
                runtime.archetype().id(),
                runtime.archetype().version(),
                runtime.comparator().id(),
                runtime.comparator().version(),
                runtime.entryPoint(),
                json(runtime.signature()),
                runtime.starterCode(),
                json(runtime.bannedApiPatterns()),
                contract.privateGrading().referenceSolution(),
                revision);

        int ordinal = 0;
        for (var testCase : runtime.publicTests()) {
            insertTestCase(configId, testCase, ordinal++);
        }
        for (var testCase : contract.privateGrading().hiddenTests()) {
            insertTestCase(configId, testCase, ordinal++);
        }
        return configId;
    }

    @Override
    public boolean markVerified(UUID questionId, UUID configId) {
        return repository.markVerified(questionId, configId) == 1;
    }

    @Override
    public void saveVerification(edu.ai.lms.core.coderunner.app.domain.CodeRunnerVerificationRecord record) {
        repository.insertVerification(
                record.id(),
                record.revisionId(),
                record.verifiedBy(),
                record.verifiedAt(),
                record.executionProfileJson(),
                record.judge0SubmissionId(),
                record.verdict(),
                record.stdout(),
                record.stderr(),
                record.timeMs(),
                record.memoryKb()
        );
    }

    private void insertTestCase(UUID configId, ExecutableAssessmentContract.TestCase testCase, int ordinal) {
        repository.insertTestCase(
                UUID.randomUUID(),
                configId,
                testCase.id(),
                testCase.category(),
                testCase.visibility(),
                json(testCase.arguments()),
                json(testCase.expectedOutput()),
                testCase.points(),
                ordinal);
    }

    @Override
    public Optional<ExecutableAssessmentContract> findLatestByQuestionId(UUID questionId) {
        return repository.findLatestConfig(questionId).map(this::toContract);
    }

    @Override
    public Optional<StoredCodeRunnerContract> findLatestStoredByQuestionId(UUID questionId) {
        return repository.findLatestConfig(questionId).map(this::storedContract);
    }

    @Override
    public Optional<StoredCodeRunnerContract> findReleasedForAttempt(UUID attemptId, UUID questionId) {
        return repository.findReleasedConfigForAttempt(attemptId, questionId).map(this::storedContract);
    }

    private StoredCodeRunnerContract storedContract(CodeRunnerConfigRow row) {
            Map<String, UUID> testCaseIds = new LinkedHashMap<>();
            repository.findTestCases(row.getId())
                    .forEach(test -> testCaseIds.put(test.getExternalTestId(), test.getId()));
            return new StoredCodeRunnerContract(
                    row.getId(), toContract(row), Map.copyOf(testCaseIds));
    }

    private ExecutableAssessmentContract toContract(CodeRunnerConfigRow row) {
        List<ExecutableAssessmentContract.TestCase> publicTests = new ArrayList<>();
        List<ExecutableAssessmentContract.TestCase> hiddenTests = new ArrayList<>();
        for (CodeRunnerTestCaseRow testRow : repository.findTestCases(row.getId())) {
            var testCase = new ExecutableAssessmentContract.TestCase(
                    testRow.getExternalTestId(),
                    testRow.getCategory(),
                    testRow.getVisibility(),
                    read(testRow.getArgumentsJson(), new TypeReference<>() { }),
                    readTree(testRow.getExpectedOutputJson()),
                    testRow.getPoints());
            if ("PUBLIC".equals(testCase.visibility())) {
                publicTests.add(testCase);
            } else {
                hiddenTests.add(testCase);
            }
        }

        var signature = read(row.getSignatureJson(),
                new TypeReference<ExecutableAssessmentContract.FunctionSignature>() { });
        List<String> bannedPatterns = read(row.getBannedApiPatternsJson(), new TypeReference<>() { });
        var runtime = new ExecutableAssessmentContract.RuntimeContract(
                new ExecutableAssessmentContract.VersionedRef(
                        row.getExecutionProfileId(), row.getExecutionProfileVersion()),
                new ExecutableAssessmentContract.VersionedRef(
                        row.getArchetypeId(), row.getArchetypeVersion()),
                new ExecutableAssessmentContract.VersionedRef(
                        row.getComparatorId(), row.getComparatorVersion()),
                row.getEntryPoint(),
                signature,
                row.getStarterCode(),
                List.copyOf(publicTests),
                List.copyOf(bannedPatterns));
        return new ExecutableAssessmentContract(
                row.getSchemaVersion(),
                row.getContractId(),
                row.getContractVersion(),
                row.getContractStatus(),
                row.getTitle(),
                row.getStatement(),
                runtime,
                new ExecutableAssessmentContract.PrivateGrading(
                        row.getReferenceSolution(), List.copyOf(hiddenTests)));
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Code Runner contract contains invalid JSON", exception);
        }
    }

    private <T> T read(String value, TypeReference<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored Code Runner contract is invalid", exception);
        }
    }

    private JsonNode readTree(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored Code Runner test output is invalid", exception);
        }
    }
}
