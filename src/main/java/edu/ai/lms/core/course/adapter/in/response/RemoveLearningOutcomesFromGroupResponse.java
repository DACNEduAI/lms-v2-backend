package edu.ai.lms.core.course.adapter.in.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemoveLearningOutcomesFromGroupResponse {
    private UUID groupId;
    private String name;
    private UUID courseId;
    private int totalRemoved;
    private String message;
}
