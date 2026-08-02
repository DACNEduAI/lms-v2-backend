package edu.ai.lms.core.course.adapter.in.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Full exam matrix response including rows.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamMatrixResponse {

    private UUID id;
    private UUID courseId;
    private String title;
    private String description;
    private List<ExamMatrixRowResponse> rows;
    private List<CustomLearningLevelResponse> customLearningLevels;
    private String configurationStatus;
    private List<UUID> uncoveredLearningOutcomeIds;
    private List<UUID> emptyCustomLearningLevelIds;
    private List<UUID> invalidRowIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
