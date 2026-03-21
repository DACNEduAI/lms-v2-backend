package edu.ai.lms.core.course.adapter.in.response;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import edu.ai.lms.config.DatePatternConstants;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetCourseResponse {
    private UUID courseId;
    private String courseName;
    private String courseCode;
    private String description;
    private UUID teacherId;
    private Boolean active;
    private String coverImage;
    @Schema(type = "string", pattern = DatePatternConstants.LOCAL_DATE_PATTERN, example = DatePatternConstants.LOCAL_DATE_EXAMPLE)
    @NotNull(message = "Start date cannot be blank")
    private LocalDate startDate;
    @Schema(type = "string", pattern = DatePatternConstants.LOCAL_DATE_PATTERN, example = DatePatternConstants.LOCAL_DATE_EXAMPLE)
    @NotNull(message = "Start date cannot be blank")
    private LocalDate endDate;
    private List<GroupLearningOutcomeDto> groupLearningOutcomes;
    private List<LearningOutcomeDto> ungroupedLearningOutcomes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GroupLearningOutcomeDto {
        private UUID id;
        private String name;
        private String description;
        private List<LearningOutcomeDto> learningOutcomes;
    }

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
