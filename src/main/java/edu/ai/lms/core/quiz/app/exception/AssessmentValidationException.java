package edu.ai.lms.core.quiz.app.exception;

import java.util.UUID;

/**
 * A publish or edit invariant that the teacher UI can map back to a precise
 * assessment field. This remains separate from generic validation so clients
 * do not have to parse an English error message.
 */
public class AssessmentValidationException extends RuntimeException {
    private final String code;
    private final String fieldPath;
    private final UUID variantId;
    private final UUID questionId;

    public AssessmentValidationException(String code, String fieldPath, UUID variantId,
            UUID questionId, String message) {
        super(message);
        this.code = code;
        this.fieldPath = fieldPath;
        this.variantId = variantId;
        this.questionId = questionId;
    }

    public String getCode() { return code; }
    public String getFieldPath() { return fieldPath; }
    public UUID getVariantId() { return variantId; }
    public UUID getQuestionId() { return questionId; }
}
