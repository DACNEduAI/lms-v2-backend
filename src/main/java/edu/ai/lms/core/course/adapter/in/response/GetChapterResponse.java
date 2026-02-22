package edu.ai.lms.core.course.adapter.in.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetChapterResponse {
    private UUID chapterId;
    private String title;
    private String description;
    private Integer orderIndex;
    private UUID courseId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
