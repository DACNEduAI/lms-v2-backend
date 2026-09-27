package edu.ai.lms.core.auth.adapter.out.security;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import edu.ai.lms.core.auth.api.AuthenticatedPrincipal;
import edu.ai.lms.core.auth.api.TokenAuthenticationApi;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ASSET_GENERATION_CALLBACK = "/api/internal/assets/image-generations/callback";

    private final TokenAuthenticationApi tokenAuthenticationApi;
    private final SecurityErrorResponseWriter errorResponseWriter;

    /**
     * The image-generation callback authenticates with its own shared secret in
     * the Authorization header.  It is not a user JWT; letting this filter parse
     * it as one would reject a valid callback before its controller can verify
     * that shared secret.
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return ASSET_GENERATION_CALLBACK.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Authorization header is missing or does not start with Bearer");
            logger.info(request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try {
            AuthenticatedPrincipal principal = tokenAuthenticationApi.authenticate(jwt);
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityUserPrincipal userDetails = new SecurityUserPrincipal(
                        principal.userId(), principal.email(), null, principal.role());
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            logger.error("Could not set user authentication in security context", e);
            SecurityContextHolder.clearContext();
            errorResponseWriter.unauthorized(request, response, "INVALID_ACCESS_TOKEN",
                    "Access token is expired, malformed, or unverifiable");
            return;
        }
        filterChain.doFilter(request, response);
    }

}
