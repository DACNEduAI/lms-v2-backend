package edu.ai.lms.core.auth.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.auth.adapter.in.http.response.TeacherRegisterResponse;
import edu.ai.lms.core.auth.app.domain.Teacher;
import edu.ai.lms.core.auth.app.port.in.command.TeacherRegisterCommand;
import edu.ai.lms.core.auth.app.port.in.usecase.TeacherRegisterUseCase;
import edu.ai.lms.core.auth.app.port.out.LoadTeacherPort;
import edu.ai.lms.core.auth.app.port.out.SaveTeacherPort;
import edu.ai.lms.core.auth.app.port.out.TokenGenerationPort;
import edu.ai.lms.core.auth.app.exception.EmailAlreadyExistsException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeacherRegisterService implements TeacherRegisterUseCase {

    private final SaveTeacherPort saveTeacherPort;
    private final LoadTeacherPort loadTeacherPort;
    private final TokenGenerationPort tokenGenerationPort;

    @Override
    @Transactional
    public TeacherRegisterResponse teacherRegister(TeacherRegisterCommand command) {
        Teacher teacher = command.teacher();

        // Business rule validation: Check if email already exists
        if (loadTeacherPort.findByEmail(teacher.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already registered: " + teacher.getEmail());
        }

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
