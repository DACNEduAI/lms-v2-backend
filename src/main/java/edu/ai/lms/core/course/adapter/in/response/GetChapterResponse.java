package edu.ai.lms.core.course.adapter.in.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetChapterResponse {
    private Long chapterNumber;
    private String title;
    private String description;
    private UUID courseId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
