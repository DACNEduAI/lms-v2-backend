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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CcctAiJob {
    private UUID id;
    private UUID courseId;
    private UUID profileId;
    private CcctAiJobType type;
    private CcctAiJobStatus status;
    private String stage;
    private long expectedVersion;
    private String fileKey;
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}
