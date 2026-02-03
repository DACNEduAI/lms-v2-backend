package edu.ai.lms.core.auth.app.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.auth.adapter.in.request.StudentSignInRequest;
import edu.ai.lms.core.auth.adapter.in.response.StudentSignInResponse;
import edu.ai.lms.core.auth.app.domain.Student;
import edu.ai.lms.core.auth.app.port.in.usecase.StudentSignInUseCase;
import edu.ai.lms.core.auth.app.port.out.AuthenticationPort;
import edu.ai.lms.core.auth.app.port.out.TokenGenerationPort;
import edu.ai.lms.core.auth.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentSignInService implements StudentSignInUseCase {

    private final AuthenticationPort authenticationPort;
    private final TokenGenerationPort tokenGenerationPort;

    @Override
    @Transactional
    public StudentSignInResponse signIn(StudentSignInRequest request) {
        try {
            
            // Authenticate using the authentication port
            Authentication authentication = authenticationPort.authenticate(
                    request.getEmail(),
                    request.getPassword()
            );

            // Get the authenticated student (principal is our Student domain object)
            Student student = (Student) authentication.getPrincipal();

            // Generate JWT token
            String token = tokenGenerationPort.generateStudentToken(student);

            // Return response with token and user info
            return StudentSignInResponse.builder()
                    .token(token)
                    .email(student.getEmail())
                    .firstName(student.getFirstName())
                    .lastName(student.getLastName())
                    .role(student.getRoleAsString())
                    .build();

        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }
}
