package edu.ai.lms.core.course.adapter.in.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifyLearningOutcomeResponse {
    private UUID id;
    private String code;
    private String description;
    private UUID courseId;
    private LocalDateTime updatedAt;
}
