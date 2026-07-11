package edu.ai.lms.core.course.adapter.in.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import edu.ai.lms.config.DatePatternConstants;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetChapterResponse {
    private UUID chapterId;
    private String title;
    private String description;
    private UUID courseId;
    private List<UUID> learningOutcomeIds;
    @Builder.Default
    private List<LearningOutcomeSummary> ungroupedLearningOutcomes = new ArrayList<>();
    @Builder.Default
    private List<GroupLearningOutcomeSummary> groupedLearningOutcomes = new ArrayList<>();
    @Schema(type = "string", pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN, example = DatePatternConstants.LOCAL_DATE_TIME_EXAMPLE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN)
    private LocalDateTime createdAt;
    @Schema(type = "string", pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN, example = DatePatternConstants.LOCAL_DATE_TIME_EXAMPLE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DatePatternConstants.LOCAL_DATE_TIME_PATTERN)
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LearningOutcomeSummary {
        private UUID id;
        private String code;
        private Integer prefixNumber;
        private Integer sortOrder;
        private String displayCode;
        private String description;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GroupLearningOutcomeSummary {
        private UUID id;
        private String name;
        private String code;
        private String displayCode;
        private String description;
        @Builder.Default
        private List<LearningOutcomeSummary> learningOutcomes = new ArrayList<>();
    }
}
