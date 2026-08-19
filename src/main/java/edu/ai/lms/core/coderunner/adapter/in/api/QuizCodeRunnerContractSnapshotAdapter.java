package edu.ai.lms.core.coderunner.adapter.in.api;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ai.lms.core.coderunner.app.domain.ExecutableAssessmentContract;
import edu.ai.lms.core.coderunner.app.port.out.SaveCodeRunnerContractPort;
import edu.ai.lms.core.coderunner.app.port.out.LoadCodeRunnerContractPort;
import edu.ai.lms.core.quiz.api.CodeRunnerContractSnapshotApi;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QuizCodeRunnerContractSnapshotAdapter implements CodeRunnerContractSnapshotApi {
    private final ObjectMapper objectMapper;
    private final SaveCodeRunnerContractPort contracts;
    private final LoadCodeRunnerContractPort loadedContracts;

    @Override
    public String normalizeForReview(String contractJson) {
        if (contractJson == null || contractJson.isBlank()) {
            throw new IllegalArgumentException("Code Runner contract is required");
        }
        try {
            ExecutableAssessmentContract contract = objectMapper.readValue(
                    contractJson, ExecutableAssessmentContract.class).withStatus("READY_FOR_REVIEW");
            contract.validateForPersistence();
            return objectMapper.writeValueAsString(contract);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Code Runner contract is not valid JSON", ex);
        }
    }

    @Override
    public void createQuestionRevision(UUID questionId, String contractJson) {
        try {
            ExecutableAssessmentContract contract = objectMapper.readValue(
                    normalizeForReview(contractJson), ExecutableAssessmentContract.class);
            contracts.saveRevision(questionId, contract);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Code Runner contract could not be materialized", ex);
        }
    }

    @Override
    public void cloneLatestQuestionRevision(UUID sourceQuestionId, UUID targetQuestionId) {
        ExecutableAssessmentContract source = loadedContracts.findLatestByQuestionId(sourceQuestionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Source Code Runner question has no contract revision"));
        contracts.saveRevision(targetQuestionId, source);
    }
}
