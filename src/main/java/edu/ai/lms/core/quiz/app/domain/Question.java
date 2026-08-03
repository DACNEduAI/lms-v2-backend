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
public class Question {

    public enum QuestionType {
        MULTIPLE_CHOICE,
        TRUE_FALSE,
        ESSAY,
        CODE_RUNNER
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Image {
        public enum SourceType { AUTO, LATEX, SVG, UPLOAD }
        public enum Status { PENDING, PROCESSING, READY, FAILED }
        private UUID id;
        private SourceType sourceType;
        private String sourceText;
        private String generationBrief;
        private String altText;
        private Status status;
        private String url;
        private String imageName;
        private String storageKey;
        private String mediaType;
        private String contentHash;
        private Long byteSize;
        private String errorCode;
        private String errorMessage;
    }

    private UUID id;

    private String content;

    private QuestionType type;

    private UUID quizId;

    @Builder.Default
    private List<Answer> answers = new ArrayList<>();

    private List<Image> images;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
