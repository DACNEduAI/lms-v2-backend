package edu.ai.lms.core.auth.adapter.out.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ai.lms.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {
    private final ObjectMapper objectMapper;

    public void unauthorized(HttpServletRequest request, HttpServletResponse response,
                             String code, String message) throws IOException {
        write(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized", code, message);
    }

    public void forbidden(HttpServletRequest request, HttpServletResponse response,
                          String code, String message) throws IOException {
        write(request, response, HttpServletResponse.SC_FORBIDDEN,
                "Forbidden", code, message);
    }

    private void write(HttpServletRequest request, HttpServletResponse response, int status,
                       String error, String code, String message) throws IOException {
        String correlationId = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = request.getHeader(CorrelationIdFilter.HEADER_NAME);
        }
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            response.setHeader(CorrelationIdFilter.HEADER_NAME, correlationId);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        body.put("code", code);
        body.put("correlationId", correlationId);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
