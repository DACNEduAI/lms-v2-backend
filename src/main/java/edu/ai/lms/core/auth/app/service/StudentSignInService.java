package edu.ai.lms.core.auth.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.auth.adapter.in.http.response.StudentSignInResponse;
import edu.ai.lms.core.auth.app.domain.Student;
import edu.ai.lms.core.auth.app.port.in.command.StudentSignInCommand;
import edu.ai.lms.core.auth.app.port.in.usecase.StudentSignInUseCase;
import edu.ai.lms.core.auth.app.port.out.AuthenticationPort;
import edu.ai.lms.core.auth.app.port.out.TokenGenerationPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentSignInService implements StudentSignInUseCase {

    private final AuthenticationPort authenticationPort;
    private final TokenGenerationPort tokenGenerationPort;

    @Override
    @Transactional
    public StudentSignInResponse signIn(StudentSignInCommand command) {
        // Authenticate student using the authentication port
        Student student = authenticationPort.authenticateStudent(
                command.email(),
                command.password()
        );

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
    }
}
