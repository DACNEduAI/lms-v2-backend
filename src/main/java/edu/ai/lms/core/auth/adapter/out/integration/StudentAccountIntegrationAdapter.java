package edu.ai.lms.core.auth.adapter.out.integration;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.auth.app.domain.Student;
import edu.ai.lms.core.auth.app.port.out.LoadStudentPort;
import edu.ai.lms.core.auth.app.port.out.SaveStudentPort;
import edu.ai.lms.core.user.api.LoginAccountApi;
import edu.ai.lms.core.user.api.LoginAccountView;
import edu.ai.lms.core.user.api.StudentRegistration;
import edu.ai.lms.core.user.api.UserRegistrationApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class StudentAccountIntegrationAdapter implements LoadStudentPort, SaveStudentPort {

    private final LoginAccountApi loginAccountApi;
    private final UserRegistrationApi userRegistrationApi;

    @Override
    public Optional<Student> findByEmail(String email) {
        return loginAccountApi.findByEmailAndRole(email, Roles.STUDENT).map(this::toStudent);
    }

    @Override
    public boolean save(Student student) {
        LoginAccountView saved = userRegistrationApi.registerStudent(new StudentRegistration(
                student.getEmail(), student.getPassword(), student.getFirstName(), student.getLastName(),
                student.getAvatarUrl(), student.getStudentId(), student.getEnrollmentYear()));
        student.setId(saved.id());
        return true;
    }

    private Student toStudent(LoginAccountView account) {
        return Student.builder()
                .id(account.id())
                .email(account.email())
                .password(account.passwordHash())
                .firstName(account.firstName())
                .lastName(account.lastName())
                .avatarUrl(account.avatarUrl())
                .role(account.role())
                .studentId(account.studentId())
                .enrollmentYear(account.enrollmentYear())
                .build();
    }
}
