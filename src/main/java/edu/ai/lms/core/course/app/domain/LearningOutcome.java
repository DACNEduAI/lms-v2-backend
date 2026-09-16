package edu.ai.lms.core.course.app.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningOutcome {

    private UUID id;

    private Long ktLearningOutcomeId;

    private String code;
    private Integer prefixNumber;
    private String description;
    private Integer sortOrder;

    private UUID courseId;

    private UUID groupLearningOutcomeId;

    private Course course;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getCode() {
        return prefixNumber == null ? code : String.valueOf(prefixNumber);
    }

    public void setCode(String code) {
        this.code = code;
        if (code == null || code.isBlank()) {
            this.prefixNumber = null;
            return;
        }

        String numericSuffix = code.replaceAll("^.*?(\\d+)$", "$1");
        try {
            this.prefixNumber = Integer.parseInt(numericSuffix);
        } catch (NumberFormatException ignored) {
            this.prefixNumber = null;
        }
    }
}
