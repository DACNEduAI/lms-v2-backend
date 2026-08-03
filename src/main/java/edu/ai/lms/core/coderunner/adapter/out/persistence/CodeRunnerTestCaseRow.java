package edu.ai.lms.core.coderunner.adapter.out.persistence;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CodeRunnerTestCaseRow {
    private String externalTestId;
    private String category;
    private String visibility;
    private String argumentsJson;
    private String expectedOutputJson;
    private double points;
    private int ordinal;
}
