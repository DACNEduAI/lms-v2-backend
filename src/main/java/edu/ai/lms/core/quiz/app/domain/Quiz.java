package edu.ai.lms.core.quiz.app.domain;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quiz {

    private UUID id;

    private String quizTitle;

    @Builder.Default
    private Boolean unlimited = false;

    private Integer timeLimitMinutes;

    private Double passScore;

    private LocalDateTime startTime;

    private LocalDateTime dueTime;

    private Integer attemptLimited;

    private UUID courseId;

    private UUID studentId;

    private UUID teacherId;

    @Builder.Default
    private Boolean display = true;

    @Builder.Default
    private QuizLifecycleStatus lifecycleStatus = QuizLifecycleStatus.DRAFT;

    @Builder.Default
    private QuizSourceType sourceType = QuizSourceType.MANUAL;

    private UUID originalQuizId;

    @Builder.Default
    private long version = 0;

    private UUID publishedBy;

    private LocalDateTime publishedAt;

    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
