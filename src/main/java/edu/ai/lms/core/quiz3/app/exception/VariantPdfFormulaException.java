package edu.ai.lms.core.quiz3.app.exception;

import java.util.List;
import java.util.UUID;

import lombok.Getter;

@Getter
public class VariantPdfFormulaException extends RuntimeException {
    private final String fieldPath;
    private final UUID variantId;
    private final UUID questionId;
    private final List<String> details;

    public VariantPdfFormulaException(String message, String fieldPath, UUID variantId, UUID questionId,
            List<String> details) {
        super(message);
        this.fieldPath = fieldPath;
        this.variantId = variantId;
        this.questionId = questionId;
        this.details = details;
    }
}
