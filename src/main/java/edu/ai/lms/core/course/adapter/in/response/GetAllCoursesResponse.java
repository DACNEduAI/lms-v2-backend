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
public class GetAllCoursesResponse {
    private List<CourseSummary> courses;
    private int totalCourses;
    private int pageSize;
    private String sortDirection;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseSummary {
        private UUID courseId;
        private String courseName;
        private String courseCode;
        private String description;
        private Boolean active;
        private String coverImage;
    @Schema(type = "string", pattern = DatePatternConstants.LOCAL_DATE_PATTERN, example = DatePatternConstants.LOCAL_DATE_EXAMPLE)
    @NotNull(message = "Start date cannot be blank")
        private LocalDate startDate;
    @Schema(type = "string", pattern = DatePatternConstants.LOCAL_DATE_PATTERN, example = DatePatternConstants.LOCAL_DATE_EXAMPLE)
    @NotNull(message = "Start date cannot be blank")
        private LocalDate endDate;
    }
}
