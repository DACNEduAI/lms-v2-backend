package edu.ai.lms.core.course.app.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
public class CcctProfile {
    private UUID id;
    private UUID courseId;
    private String name;
    private String templateTitle;
    private CcctProfileStatus status;
    private long version;
    private CcctMode lastMode;
    private String markdownContent;
    private UUID createdBy;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<CcctChapter> chapters = new ArrayList<>();
}
