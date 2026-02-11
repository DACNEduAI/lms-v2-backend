package edu.ai.lms.core.course.adapter.in.response;

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
    private LocalDate startDate;
    private LocalDate endDate;
    private List<LearningOutcomeDto> learningOutcomes;

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
