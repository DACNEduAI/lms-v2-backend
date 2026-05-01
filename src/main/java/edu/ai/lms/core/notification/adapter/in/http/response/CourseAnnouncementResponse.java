package edu.ai.lms.core.notification.adapter.in.http.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseAnnouncementResponse {
    private UUID id;
    private String title;
    private String content;
    private String type;
    private LocalDateTime createdAt;
}
