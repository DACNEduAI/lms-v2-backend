package edu.ai.lms.core.course.adapter.in.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteLearningOutcomesResponse {
    private UUID courseId;
    private List<UUID> deletedIds;
    private int totalDeleted;
    private String message;
}
