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
    private Long chapterNumber;
    private String title;
    private String description;
    private UUID courseId;
    private List<MaterialDto> materials;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MaterialDto {
        private UUID id;
        private String title;
        private String type;
        private String fileKey;
        private boolean downloadable;
    }
}
