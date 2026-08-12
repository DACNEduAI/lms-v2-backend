package edu.ai.lms.core.quiz.app.exception;

import java.util.List;
import java.util.UUID;

import lombok.Getter;

@Getter
public class VariantPdfImageException extends RuntimeException {
    private final String fieldPath;
    private final UUID variantId;
    private final UUID questionId;
    private final List<String> details;

    public VariantPdfImageException(String message, String fieldPath, UUID variantId, UUID questionId,
            List<String> details, Throwable cause) {
        super(message, cause);
        this.fieldPath = fieldPath;
        this.variantId = variantId;
        this.questionId = questionId;
        this.details = details;
    }

    public VariantPdfImageException(String message, String fieldPath, UUID variantId, UUID questionId,
            List<String> details) {
        this(message, fieldPath, variantId, questionId, details, null);
    }
}
