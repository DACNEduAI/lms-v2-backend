package edu.ai.lms.core.material.adapter.out.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import edu.ai.lms.common.api.ErrorResponse;
import edu.ai.lms.core.material.app.exception.MaterialAlreadyUploadedException;
import edu.ai.lms.core.material.app.exception.MaterialNotFoundException;
import edu.ai.lms.core.material.app.exception.MaterialUploadPendingException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Global exception handler for material-related exceptions.
 */
@RestControllerAdvice(basePackages = "edu.ai.lms.core.material.adapter.in")
public class MaterialExceptionHandler {

    /**
     * Handles MaterialNotFoundException.
     */
    @ApiResponse(responseCode = "404", description = "Material not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(MaterialNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMaterialNotFoundException(MaterialNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Material Not Found")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles MaterialAlreadyUploadedException.
     * Thrown when a material with the same original filename already exists and has UPLOADED status.
     */
    @ApiResponse(responseCode = "409", description = "Material already uploaded",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(MaterialAlreadyUploadedException.class)
    public ResponseEntity<ErrorResponse> handleMaterialAlreadyUploadedException(MaterialAlreadyUploadedException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .error("Material Already Uploaded")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles MaterialUploadPendingException.
     * Thrown when a material with the same file key exists but has PENDING status.
     */
    @ApiResponse(responseCode = "409", description = "Material upload status is pending",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(MaterialUploadPendingException.class)
    public ResponseEntity<ErrorResponse> handleMaterialUploadPendingException(MaterialUploadPendingException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .error("Material Upload Pending")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
}
