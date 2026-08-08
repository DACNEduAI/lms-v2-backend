package edu.ai.lms.core.course.adapter.in.response;

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
public class ExamMatrixRowResponse {
    private UUID id;
    private UUID chapterId;
    private String topic;
    private int orderIndex;
    private List<UUID> learningOutcomeIds;
    private List<LearningLevelCountResponse> levelCounts;
}
