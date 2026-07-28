package edu.ai.lms.core.quiz.adapter.out.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import edu.ai.lms.common.api.ErrorResponse;
import edu.ai.lms.core.quiz.app.exception.AnswerNotFoundException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Exception handler for answer-related exceptions.
 */
@RestControllerAdvice(basePackages = "edu.ai.lms.core.quiz.adapter.in")
public class AnswerExceptionHandler {

    /**
     * Handles AnswerNotFoundException.
     */
    @ApiResponse(responseCode = "404", description = "Answer not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(AnswerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAnswerNotFoundException(AnswerNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Answer Not Found")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
}
