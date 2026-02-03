package edu.ai.lms.core.auth.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.auth.adapter.in.request.TeacherRegisterRequest;
import edu.ai.lms.core.auth.adapter.in.response.TeacherRegisterResponse;
import edu.ai.lms.core.auth.app.domain.Roles;
import edu.ai.lms.core.auth.app.domain.Teacher;
import edu.ai.lms.core.auth.app.port.in.usecase.TeacherRegisterUseCase;
import edu.ai.lms.core.auth.app.port.out.LoadTeacherPort;
import edu.ai.lms.core.auth.app.port.out.SaveTeacherPort;
import edu.ai.lms.core.auth.app.port.out.TokenGenerationPort;
import edu.ai.lms.core.auth.exception.EmailAlreadyExistsException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeacherRegisterService implements TeacherRegisterUseCase {

    private final SaveTeacherPort saveTeacherPort;
    private final LoadTeacherPort loadTeacherPort;
    private final TokenGenerationPort tokenGenerationPort;

    @Override
    @Transactional
    public TeacherRegisterResponse teacherRegister(TeacherRegisterRequest request) {
        // Business rule validation: Check if email already exists
        if (loadTeacherPort.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        // Build the Teacher domain object
        Teacher teacher = Teacher.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .department(request.getDepartment())
                .specialization(request.getSpecialization())
                .role(Roles.TEACHER)
                .build();

        // Save the teacher
        saveTeacherPort.save(teacher);

        // Generate JWT token
        String token = tokenGenerationPort.generateTeacherToken(teacher);

        // Return response with token
        return TeacherRegisterResponse.builder()
                .token(token)
                .email(teacher.getEmail())
                .build();
    }
}
