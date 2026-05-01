package edu.ai.lms.core.notification.app.domain;

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
public class CourseAnnouncement {
    private UUID id;
    private UUID courseId;
    private String title;
    private String content;
    private UUID teacherId;
    private String type;
    private LocalDateTime createdAt;
}
