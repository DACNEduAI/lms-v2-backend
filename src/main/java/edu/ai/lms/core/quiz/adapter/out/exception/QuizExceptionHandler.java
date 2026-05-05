package edu.ai.lms.core.quiz.adapter.out.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import edu.ai.lms.common.ErrorResponse;
import edu.ai.lms.core.quiz.app.exception.InvalidLearningOutcomeException;
import edu.ai.lms.core.quiz.app.exception.MissingLearningOutcomeException;
import edu.ai.lms.core.quiz.app.exception.QuizGenerationJobNotFoundException;
import edu.ai.lms.core.quiz.app.exception.QuizNotFoundException;
import edu.ai.lms.core.quiz.app.exception.QuizNotVisible;
import edu.ai.lms.core.quiz.app.exception.TimeValidationException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthorizedException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Exception handler for quiz-related exceptions.
 * Handles validation errors, quiz not found, authentication, and authorization exceptions.
 */
@RestControllerAdvice(basePackages = "edu.ai.lms.core.quiz.adapter.in")
public class QuizExceptionHandler {

    /**
     * Handles validation errors from @Valid annotations.
     */
    @ApiResponse(responseCode = "400", description = "Validation Failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String message = error.getDefaultMessage();
                    return fieldName + ": " + message;
                })
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("All required fields must not be blank")
                .details(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles QuizNotFoundException.
     */
    @ApiResponse(responseCode = "404", description = "Quiz not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(QuizNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleQuizNotFoundException(QuizNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Quiz Not Found")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

        /**
         * Handles QuizGenerationJobNotFoundException.
         */
        @ApiResponse(responseCode = "404", description = "Quiz generation job not found",
                        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        @ExceptionHandler(QuizGenerationJobNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleQuizGenerationJobNotFound(QuizGenerationJobNotFoundException ex) {
                ErrorResponse errorResponse = ErrorResponse.builder()
                                .status(HttpStatus.NOT_FOUND.value())
                                .error("Quiz Generation Job Not Found")
                                .message(ex.getMessage())
                                .build();

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

    /**
     * Handles UserNotAuthenticatedException.
     */
    @ApiResponse(responseCode = "401", description = "User not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(UserNotAuthenticatedException.class)
    public ResponseEntity<ErrorResponse> handleUserNotAuthenticatedException(UserNotAuthenticatedException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Not Authenticated")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handles HttpMessageNotReadableException.
     * This occurs when the request body is malformed or cannot be read.
     */
    @ApiResponse(responseCode = "400", description = "Malformed JSON request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Malformed Request")
                .message("Request body is malformed or cannot be read. Please check your JSON format.")
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles TimeValidationException.
     */
    @ApiResponse(responseCode = "400", description = "Invalid quiz time configuration",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(TimeValidationException.class)
    public ResponseEntity<ErrorResponse> handleTimeValidationException(TimeValidationException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Time Validation Failed")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles MissingLearningOutcomeException.
     */
    @ApiResponse(responseCode = "400", description = "Missing learning outcome",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(MissingLearningOutcomeException.class)
    public ResponseEntity<ErrorResponse> handleMissingLearningOutcomeException(MissingLearningOutcomeException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Missing Learning Outcome")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles InvalidLearningOutcomeException.
     */
    @ApiResponse(responseCode = "400", description = "Invalid learning outcome",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(InvalidLearningOutcomeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLearningOutcomeException(InvalidLearningOutcomeException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Learning Outcome")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles UserNotAuthorizedException.
     */
    @ApiResponse(responseCode = "403", description = "Not authorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(UserNotAuthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUserNotAuthorizedException(UserNotAuthorizedException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error("Access Denied")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handles IllegalArgumentException.
     * This occurs for authorization failures and business rule violations.
     */
    @ApiResponse(responseCode = "403", description = "Not authorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error("Access Denied")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

        /**
         * Handles QuizNotVisible.
         * This occurs when a student tries to access a quiz with display=false.
         */
        @ApiResponse(responseCode = "403", description = "Quiz is not visible",
                        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        @ExceptionHandler(QuizNotVisible.class)
        public ResponseEntity<ErrorResponse> handleQuizNotVisibleException(QuizNotVisible ex) {
                ErrorResponse errorResponse = ErrorResponse.builder()
                                .status(HttpStatus.FORBIDDEN.value())
                                .error("Access Denied")
                                .message(ex.getMessage())
                                .build();

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }
}
