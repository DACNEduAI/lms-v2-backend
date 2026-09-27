package edu.ai.lms.core.auth.adapter.out.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import edu.ai.lms.core.auth.api.TokenAuthenticationApi;

class JwtAuthenticationFilterUT {

    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            mock(TokenAuthenticationApi.class), mock(SecurityErrorResponseWriter.class));

    @Test
    void skipsJwtParsingForImageGenerationCallback() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/internal/assets/image-generations/callback");
        request.addHeader("Authorization", "Bearer asset-generation");

        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void parsesJwtForOrdinaryApplicationRequests() {
        assertFalse(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/api/courses")));
    }
}
