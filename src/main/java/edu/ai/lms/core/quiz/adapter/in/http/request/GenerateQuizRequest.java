package edu.ai.lms.core.quiz.adapter.in.http.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class GenerateQuizRequest {

    @NotNull(message = "CourseId is required")
    private UUID courseId;

    private UUID chapterId;

    @NotBlank(message = "Quiz title is required")
    private String quizTitle;

    private String difficulty;

    @Positive(message = "Question count must be greater than 0")
    private Integer questionCount;
}
