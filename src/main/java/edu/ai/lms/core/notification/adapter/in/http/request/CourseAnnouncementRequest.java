package edu.ai.lms.core.notification.adapter.in.http.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CourseAnnouncementRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    private String message;

    private String type; // INFO, WARNING, URGENT
}
