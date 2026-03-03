package edu.ai.lms.core.course.adapter.out.exception;

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
import edu.ai.lms.core.course.app.exception.ChapterNotFoundException;
import edu.ai.lms.core.course.app.exception.CourseCodeAlreadyExistsException;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.StudentNotEnrolledInCourseException;
import edu.ai.lms.core.course.app.exception.StudentsNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Global exception handler for course-related exceptions.
 * Handles validation errors and custom course exceptions.
 */
@RestControllerAdvice(basePackages = "edu.ai.lms.core.course.adapter.in")
public class CourseExceptionHandler {

    /**
     * Handles validation errors from @Valid annotations.
     * Returns a custom message when any required field is blank.
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
     * Handles CourseCodeAlreadyExistsException.
     */
    @ApiResponse(responseCode = "400", description = "Course code already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(CourseCodeAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCourseCodeAlreadyExistsException(CourseCodeAlreadyExistsException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Course Creation Failed")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles CourseNotFoundException.
     */
    @ApiResponse(responseCode = "404", description = "Course not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCourseNotFoundException(CourseNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Course Not Found")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles ChapterNotFoundException.
     */
    @ApiResponse(responseCode = "404", description = "Chapter not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(ChapterNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChapterNotFoundException(ChapterNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Chapter Not Found")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles TeacherNotFoundException.
     */
    @ApiResponse(responseCode = "404", description = "Teacher not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(TeacherNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTeacherNotFoundException(TeacherNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Teacher Not Found")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles StudentsNotFoundException.
     * This occurs when some student emails in the request are not found in the system.
     */
    @ApiResponse(responseCode = "404", description = "Students not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(StudentsNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStudentsNotFoundException(StudentsNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Students Not Found")
                .message(ex.getMessage())
                .details(ex.getMissingEmails())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles StudentNotEnrolledInCourseException.
     * This occurs when trying to remove students who are not enrolled in the course.
     */
    @ApiResponse(responseCode = "400", description = "Students not enrolled in course",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ExceptionHandler(StudentNotEnrolledInCourseException.class)
    public ResponseEntity<ErrorResponse> handleStudentNotEnrolledInCourseException(StudentNotEnrolledInCourseException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Students Not Enrolled")
                .message(ex.getMessage())
                .details(ex.getNotEnrolledEmails())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
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
     * Handles IllegalArgumentException.
     * This occurs when a teacher tries to access/modify/delete a course they don't own.
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
}
