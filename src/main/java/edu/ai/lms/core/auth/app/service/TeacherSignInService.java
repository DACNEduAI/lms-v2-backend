package edu.ai.lms.core.auth.app.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.auth.adapter.in.request.TeacherSignInRequest;
import edu.ai.lms.core.auth.adapter.in.response.TeacherSignInResponse;
import edu.ai.lms.core.auth.app.domain.Teacher;
import edu.ai.lms.core.auth.app.port.in.usecase.TeacherSignInUseCase;
import edu.ai.lms.core.auth.app.port.out.AuthenticationPort;
import edu.ai.lms.core.auth.app.port.out.TokenGenerationPort;
import edu.ai.lms.core.auth.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeacherSignInService implements TeacherSignInUseCase {

    private final AuthenticationPort authenticationPort;
    private final TokenGenerationPort tokenGenerationPort;

    @Override
    @Transactional
    public TeacherSignInResponse signIn(TeacherSignInRequest request) {
        try {
            // Authenticate using the authentication port
            Authentication authentication = authenticationPort.authenticate(
                    request.getEmail(),
                    request.getPassword()
            );

            // Get the authenticated teacher (principal is our Teacher domain object)
            Teacher teacher = (Teacher) authentication.getPrincipal();

            // Generate JWT token
            String token = tokenGenerationPort.generateTeacherToken(teacher);

            // Return response with token and user info
            return TeacherSignInResponse.builder()
                    .token(token)
                    .email(teacher.getEmail())
                    .firstName(teacher.getFirstName())
                    .lastName(teacher.getLastName())
                    .role(teacher.getRoleAsString())
                    .build();

        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }
}
