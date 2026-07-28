package edu.ai.lms.core.course.adapter.out.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import edu.ai.lms.common.api.ErrorResponse;
import edu.ai.lms.core.course.app.exception.GroupLearningOutcomeNotFoundException;
import edu.ai.lms.core.course.app.exception.LearningOutcomeAlreadyInGroupException;
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

    /**
     * Handles GroupLearningOutcomeNotFoundException.
     */
    @ApiResponse(responseCode = "404", description = "Group learning outcome not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(GroupLearningOutcomeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGroupLearningOutcomeNotFoundException(GroupLearningOutcomeNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Group Learning Outcome Not Found")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles LearningOutcomeAlreadyInGroupException.
     */
    @ApiResponse(responseCode = "400", description = "Learning outcome already in a group",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(LearningOutcomeAlreadyInGroupException.class)
    public ResponseEntity<ErrorResponse> handleLearningOutcomeAlreadyInGroupException(LearningOutcomeAlreadyInGroupException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Learning Outcome Already In Group")
                .message(ex.getMessage())
                .details(ex.getConflictingIds().stream().map(java.util.UUID::toString).collect(java.util.stream.Collectors.toList()))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
