package edu.ai.lms.core.auth.adapter.out.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import edu.ai.lms.core.auth.app.port.out.AuthenticationPort;
import lombok.RequiredArgsConstructor;

/**
 * Adapter that implements authentication using Spring Security's AuthenticationManager.
 */
@Component
@RequiredArgsConstructor
public class AuthenticationAdapter implements AuthenticationPort {

    private final AuthenticationManager authenticationManager;

    @Override
    public Authentication authenticate(String email, String password) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );
    }
}
