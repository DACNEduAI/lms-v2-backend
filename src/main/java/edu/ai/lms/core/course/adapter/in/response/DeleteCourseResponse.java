package edu.ai.lms.core.course.adapter.in.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteCourseResponse {
    private UUID courseId;
    private String message;
    private Boolean success;
}
