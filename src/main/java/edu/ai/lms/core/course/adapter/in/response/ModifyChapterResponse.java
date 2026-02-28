package edu.ai.lms.core.course.adapter.in.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifyChapterResponse {
    private Long chapterNumber;
    private String title;
    private String description;
    private UUID courseId;
    private List<MaterialDto> materials;
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
        private String sourceUrl;
        private boolean downloadable;
    }
}
