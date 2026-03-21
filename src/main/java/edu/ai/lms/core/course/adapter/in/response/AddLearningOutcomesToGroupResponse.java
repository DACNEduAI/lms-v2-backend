package edu.ai.lms.core.course.adapter.in.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddLearningOutcomesToGroupResponse {
    private UUID groupId;
    private String name;
    private UUID courseId;
    private List<LearningOutcomeDto> learningOutcomes;
    private int totalAdded;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LearningOutcomeDto {
        private UUID id;
        private String code;
        private String description;
    }
}
