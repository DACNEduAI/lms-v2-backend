package edu.ai.lms.core.auth.adapter.out.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import edu.ai.lms.core.auth.app.domain.Student;
import edu.ai.lms.core.auth.app.domain.Teacher;
import edu.ai.lms.core.auth.app.port.out.AuthenticationPort;
import edu.ai.lms.core.auth.app.port.out.LoadStudentPort;
import edu.ai.lms.core.auth.app.port.out.LoadTeacherPort;
import edu.ai.lms.core.auth.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;

/**
 * Adapter that implements authentication by loading users from their respective ports
 * and verifying passwords.
 */
@Component
@RequiredArgsConstructor
public class AuthenticationAdapter implements AuthenticationPort {

    private final LoadStudentPort loadStudentPort;
    private final LoadTeacherPort loadTeacherPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Student authenticateStudent(String email, String password) {
        Student student = loadStudentPort.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email"));

        if (!passwordEncoder.matches(password, student.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        return student;
    }

    @Override
    public Teacher authenticateTeacher(String email, String password) {
        Teacher teacher = loadTeacherPort.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email"));

        if (!passwordEncoder.matches(password, teacher.getPassword())) {
            throw new InvalidCredentialsException("Invalid email password");
        }

        return teacher;
    }
}
