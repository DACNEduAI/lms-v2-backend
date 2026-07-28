package edu.ai.lms.core.course.app.domain;

import java.time.LocalDateTime;
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
public class ExamMatrixSummary {
    private UUID id;
    private UUID courseId;
    private String title;
    private String description;
    private long rowCount;
    private long totalQuestionCount;
    private String configurationStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
