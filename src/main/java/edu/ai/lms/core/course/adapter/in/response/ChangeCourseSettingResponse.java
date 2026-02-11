package edu.ai.lms.core.course.adapter.in.response;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeCourseSettingResponse {
    private UUID courseId;
    private String courseName;
    private String courseCode;
    private String description;
    private Boolean active;
    private String coverImage;
    private LocalDate startDate;
    private LocalDate endDate;
}
