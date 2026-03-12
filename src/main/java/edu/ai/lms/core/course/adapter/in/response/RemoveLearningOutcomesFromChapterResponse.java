package edu.ai.lms.core.course.adapter.in.response;

import java.util.List;
import java.util.UUID;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemoveLearningOutcomesFromChapterResponse {
    private UUID courseId;
    private Long chapterNumber;
    private List<UUID> removedLearningOutcomeIds;
    private int totalRemoved;
    private String message;
}
