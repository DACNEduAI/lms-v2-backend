package edu.ai.lms.core.course.adapter.out.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import edu.ai.lms.common.ErrorResponse;
import edu.ai.lms.core.course.app.exception.LearningOutcomeNotFoundException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Exception handler for learning outcome-related exceptions.
 */
@RestControllerAdvice(basePackages = "edu.ai.lms.core.course.adapter.in")
public class LearningOutcomeExceptionHandler {

    /**
     * Handles LearningOutcomeNotFoundException.
     */
    @ApiResponse(responseCode = "404", description = "Learning outcome not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(LearningOutcomeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLearningOutcomeNotFoundException(LearningOutcomeNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Learning Outcome Not Found")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
}
