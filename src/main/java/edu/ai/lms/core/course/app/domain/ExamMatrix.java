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

/**
 * Domain entity representing an exam matrix (ma trận đề thi).
 * An exam matrix belongs to one course and contains ordered rows that
 * describe how many questions at each cognitive level should be drawn
 * from each learning outcome.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamMatrix {

    private UUID id;

    /** The course this exam matrix belongs to. */
    private UUID courseId;

    private String title;

    private String description;

    @Builder.Default
    private List<ExamMatrixRow> rows = new ArrayList<>();

    @Builder.Default
    private List<CustomLearningLevel> customLearningLevels = new ArrayList<>();

    @Builder.Default
    private List<UUID> uncoveredLearningOutcomeIds = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
