package edu.ai.lms.core.coderunner.adapter.in.http;

import edu.ai.lms.core.coderunner.app.exception.CodeRunnerAccessDeniedException;
import edu.ai.lms.core.coderunner.app.exception.CodeRunnerConflictException;
import edu.ai.lms.core.coderunner.app.exception.CodeRunnerNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "edu.ai.lms.core.coderunner")
public class CodeRunnerExceptionHandler {

    @ExceptionHandler(CodeRunnerNotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(CodeRunnerNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "CODE_RUNNER_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(CodeRunnerAccessDeniedException.class)
    ResponseEntity<Map<String, String>> forbidden(CodeRunnerAccessDeniedException exception) {
        return response(HttpStatus.FORBIDDEN, "CODE_RUNNER_ACCESS_DENIED", exception.getMessage());
    }

    @ExceptionHandler(CodeRunnerConflictException.class)
    ResponseEntity<Map<String, String>> conflict(CodeRunnerConflictException exception) {
        return response(HttpStatus.CONFLICT, "CODE_RUNNER_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> invalid(IllegalArgumentException exception) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_CODE_RUNNER_CONTRACT", exception.getMessage());
    }

    private ResponseEntity<Map<String, String>> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("code", code, "message", message));
    }
}
