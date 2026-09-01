package edu.ai.lms.core.coderunner.adapter.out.persistence;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor
class CodeSubmissionTestResultRow {
    private UUID id;
    private UUID submissionId;
    private UUID testCaseId;
    private String externalTestId;
    private String visibility;
    private String executionStatus;
    private String verdict;
    private double pointsEarned;
    private Double timeMs;
    private Long memoryKb;
    private String diagnostic;
    private String receivedOutput;
    private String engineSubmissionId;
}
