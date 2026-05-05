package edu.ai.lms.core.quiz.app.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import edu.ai.lms.common.Roles;
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
public class QuizGenerationJob {

    private UUID id;

    private UUID courseId;

    private UUID chapterId;

    private UUID quizId;

    private UUID requestedBy;

    private Roles requestedRole;

    private String quizTitle;

    private String difficulty;

    private Integer questionCount;

    private QuizGenerationStatus status;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
