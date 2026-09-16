package edu.ai.lms.core.learnermodel.adapter.in.http;

import edu.ai.lms.common.api.ErrorResponse;
import edu.ai.lms.core.learnermodel.app.exception.AmbiguousLearningOutcomeHistoryException;
import edu.ai.lms.core.learnermodel.app.exception.LearnerModelAccessDeniedException;
import edu.ai.lms.core.learnermodel.app.exception.LearnerModelNotFoundException;
import edu.ai.lms.core.learnermodel.app.exception.LearnerModelUpstreamException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = LearnerModelController.class)
public class LearnerModelExceptionHandler {
    @ExceptionHandler(LearnerModelNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(LearnerModelNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "LEARNING_OUTCOME_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(LearnerModelAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(LearnerModelAccessDeniedException exception) {
        return response(HttpStatus.FORBIDDEN, "LEARNER_MODEL_ACCESS_DENIED", exception.getMessage());
    }

    @ExceptionHandler(AmbiguousLearningOutcomeHistoryException.class)
    public ResponseEntity<ErrorResponse> handleAmbiguous(AmbiguousLearningOutcomeHistoryException exception) {
        return response(HttpStatus.CONFLICT, "AMBIGUOUS_LEARNING_OUTCOME_HISTORY", exception.getMessage());
    }

    @ExceptionHandler(LearnerModelUpstreamException.class)
    public ResponseEntity<ErrorResponse> handleUpstream(LearnerModelUpstreamException exception) {
        HttpStatus status = exception.kind() == LearnerModelUpstreamException.Kind.UNAVAILABLE
                ? HttpStatus.SERVICE_UNAVAILABLE
                : HttpStatus.BAD_GATEWAY;
        return response(status, "LEARNER_MODEL_" + exception.kind(), exception.getMessage());
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .code(code)
                .build());
    }
}
