package edu.ai.lms.core.course.adapter.in.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGroupLearningOutcomeResponse {
    private UUID id;
    private String name;
    private String description;
    private UUID courseId;
    private List<LearningOutcomeDto> learningOutcomes;
    private LocalDateTime createdAt;

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
