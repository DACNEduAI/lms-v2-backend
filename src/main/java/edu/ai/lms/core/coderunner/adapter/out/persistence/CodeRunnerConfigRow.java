package edu.ai.lms.core.coderunner.adapter.out.persistence;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CodeRunnerConfigRow {
    private UUID id;
    private String schemaVersion;
    private String contractId;
    private String contractVersion;
    private String contractStatus;
    private String title;
    private String statement;
    private String executionProfileId;
    private String executionProfileVersion;
    private String archetypeId;
    private String archetypeVersion;
    private String comparatorId;
    private String comparatorVersion;
    private String entryPoint;
    private String signatureJson;
    private String starterCode;
    private String bannedApiPatternsJson;
    private String referenceSolution;
    private int wallTimeLimitMs;
    private int memoryLimitKb;
    private int revision;
}
