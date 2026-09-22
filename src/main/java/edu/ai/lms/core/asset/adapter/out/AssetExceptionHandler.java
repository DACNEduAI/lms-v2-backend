package edu.ai.lms.core.asset.adapter.out;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import edu.ai.lms.core.asset.app.exception.AssetNotFoundException;
import edu.ai.lms.core.asset.app.exception.AssetValidationException;

@RestControllerAdvice
public class AssetExceptionHandler {
    @ExceptionHandler(AssetNotFoundException.class) @ResponseStatus(HttpStatus.NOT_FOUND) public java.util.Map<String, String> notFound(RuntimeException e) { return java.util.Map.of("message", e.getMessage()); }
    @ExceptionHandler(AssetValidationException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) public java.util.Map<String, String> invalid(RuntimeException e) { return java.util.Map.of("message", e.getMessage()); }
}
