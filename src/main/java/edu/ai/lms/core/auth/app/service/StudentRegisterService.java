package edu.ai.lms.core.auth.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.auth.adapter.in.request.StudentRegisterRequest;
import edu.ai.lms.core.auth.adapter.in.response.StudentRegisterResponse;
import edu.ai.lms.core.auth.app.domain.Roles;
import edu.ai.lms.core.auth.app.domain.Student;
import edu.ai.lms.core.auth.app.port.in.usecase.StudentRegisterUseCase;
import edu.ai.lms.core.auth.app.port.out.LoadStudentPort;
import edu.ai.lms.core.auth.app.port.out.SaveStudentPort;
import edu.ai.lms.core.auth.app.port.out.TokenGenerationPort;
import edu.ai.lms.core.auth.exception.EmailAlreadyExistsException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentRegisterService implements StudentRegisterUseCase {

    private final SaveStudentPort saveStudentPort;
    private final LoadStudentPort loadStudentPort;
    private final TokenGenerationPort tokenGenerationPort;

    @Override
    @Transactional
    public StudentRegisterResponse studentRegister(StudentRegisterRequest request) {
        // Check if email already exists
        if (loadStudentPort.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        // Build the Student domain object with encoded password
        Student student = Student.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .enrollmentYear(request.getEnrollmentYear())
                .role(Roles.STUDENT)
                .build();

        // Save the student
        saveStudentPort.save(student);

        // Generate JWT token
        String token = tokenGenerationPort.generateStudentToken(student);

        // Return response with token
        return StudentRegisterResponse.builder()
                .token(token)
                .email(student.getEmail())
                .build();
    }
}