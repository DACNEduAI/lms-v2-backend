package edu.ai.lms.core.course.adapter.in.response;

import java.util.List;
import java.util.UUID;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddLearningOutcomesToChapterResponse {
    private UUID courseId;
    private Long chapterNumber;
    private List<UUID> addedLearningOutcomeIds;
    private int totalAdded;
    private String message;
}
