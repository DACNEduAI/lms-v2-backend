package edu.ai.lms.core.quiz.adapter.out.exception;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import edu.ai.lms.common.ErrorResponse;
import edu.ai.lms.core.quiz.app.exception.QuestionNotFoundException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Exception handler for question-related exceptions.
 */
@RestControllerAdvice(basePackages = "edu.ai.lms.core.quiz.adapter.in")
public class QuestionExceptionHandler {

    /**
     * Handles QuestionNotFoundException.
     */
    @ApiResponse(responseCode = "404", description = "Question not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(QuestionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleQuestionNotFoundException(QuestionNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Question Not Found")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
}
