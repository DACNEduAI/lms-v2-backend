package edu.ai.lms.core.course.adapter.out.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import edu.ai.lms.common.api.ErrorResponse;
import edu.ai.lms.core.course.app.exception.CcctConflictException;
import edu.ai.lms.core.course.app.exception.CcctPayloadTooLargeException;
import edu.ai.lms.core.course.app.exception.CcctProfileNotFoundException;
import edu.ai.lms.core.course.app.exception.CcctValidationException;

@RestControllerAdvice(assignableTypes = edu.ai.lms.core.course.adapter.in.CcctProfileController.class)
public class CcctExceptionHandler {

    @ExceptionHandler(CcctValidationException.class)
    public ResponseEntity<ErrorResponse> validation(CcctValidationException exception) {
        return error(HttpStatus.BAD_REQUEST, "Invalid CCCT", exception.getMessage());
    }

    @ExceptionHandler(CcctProfileNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(CcctProfileNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "CCCT Profile Not Found", exception.getMessage());
    }

    @ExceptionHandler(CcctConflictException.class)
    public ResponseEntity<ErrorResponse> conflict(CcctConflictException exception) {
        return error(HttpStatus.CONFLICT, "CCCT Conflict", exception.getMessage());
    }

    @ExceptionHandler(CcctPayloadTooLargeException.class)
    public ResponseEntity<ErrorResponse> payloadTooLarge(CcctPayloadTooLargeException exception) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "CCCT Payload Too Large", exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> databaseConflict(DataIntegrityViolationException exception) {
        return error(
                HttpStatus.CONFLICT,
                "CCCT Conflict",
                "The CCCT changed concurrently or violates a unique constraint");
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status,
            String error,
            String message) {
        return ResponseEntity.status(status).body(ErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .build());
    }
}
