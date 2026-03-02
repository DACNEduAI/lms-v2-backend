package edu.ai.lms.core.course.adapter.in.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifyLearningOutcomeRequest {
    private String code;
    private String description;
}
