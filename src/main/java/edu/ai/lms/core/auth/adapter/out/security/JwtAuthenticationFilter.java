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

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final LoadStudentPort loadStudentPort;
    private final LoadTeacherPort loadTeacherPort;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;
        final String userRole;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try {
            userEmail = jwtUtil.extractEmail(jwt);
            userRole = jwtUtil.extractRole(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = loadUserByEmailAndRole(userEmail, userRole);

                if (jwtUtil.validateToken(jwt, userDetails)) {
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
            logger.error("Could not set user authentication in security context", e);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Loads a user by email based on the role specified in the JWT token.
     * This ensures that students and teachers are loaded from their respective tables,
     * preventing cross-authentication between user types.
     *
     * @param email the user's email
     * @param role the user's role from JWT ("STUDENT" or "TEACHER")
     * @return UserDetails for Spring Security authentication
     * @throws UsernameNotFoundException if the user is not found
     */
    private UserDetails loadUserByEmailAndRole(String email, String role) {
        if ("TEACHER".equals(role)) {
            return loadTeacherPort.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Teacher not found with email: " + email));
        } else {
            return loadStudentPort.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Student not found with email: " + email));
        }
    }
}

