package edu.ai.lms.core.auth.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.auth.adapter.in.http.response.StudentRegisterResponse;
import edu.ai.lms.core.auth.app.domain.Student;
import edu.ai.lms.core.auth.app.port.in.command.StudentRegisterCommand;
import edu.ai.lms.core.auth.app.port.in.usecase.StudentRegisterUseCase;
import edu.ai.lms.core.auth.app.port.out.LoadStudentPort;
import edu.ai.lms.core.auth.app.port.out.SaveStudentPort;
import edu.ai.lms.core.auth.app.port.out.TokenGenerationPort;
import edu.ai.lms.core.auth.app.exception.EmailAlreadyExistsException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentRegisterService implements StudentRegisterUseCase {

    private final SaveStudentPort saveStudentPort;
    private final LoadStudentPort loadStudentPort;
    private final TokenGenerationPort tokenGenerationPort;

    @Override
    @Transactional
    public StudentRegisterResponse studentRegister(StudentRegisterCommand command) {
        Student student = command.student();

        // Check if email already exists
        if (loadStudentPort.findByEmail(student.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already registered: " + student.getEmail());
        }

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