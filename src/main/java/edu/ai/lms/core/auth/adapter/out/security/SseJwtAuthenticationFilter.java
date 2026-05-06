package edu.ai.lms.core.auth.adapter.out.security;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import edu.ai.lms.core.auth.app.port.out.LoadStudentPort;
import edu.ai.lms.core.auth.app.port.out.LoadTeacherPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String TOKEN_PARAM = "token";

    private final JwtUtil jwtUtil;
    private final LoadStudentPort loadStudentPort;
    private final LoadTeacherPort loadTeacherPort;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.contains("/stream");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getParameter(TOKEN_PARAM);
        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {
            String userEmail = jwtUtil.extractEmail(token);
            String userRole = jwtUtil.extractRole(token);

            if (userEmail != null) {
                UserDetails userDetails = loadUserByEmailAndRole(userEmail, userRole);

                if (jwtUtil.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.error("Could not set SSE user authentication in security context", e);
        }

        filterChain.doFilter(request, response);
    }

    private UserDetails loadUserByEmailAndRole(String email, String role) {
        if ("TEACHER".equals(role)) {
            return loadTeacherPort.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Teacher not found with email: " + email));
        }
        return loadStudentPort.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Student not found with email: " + email));
    }
}
