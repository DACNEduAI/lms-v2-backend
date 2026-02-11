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
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
