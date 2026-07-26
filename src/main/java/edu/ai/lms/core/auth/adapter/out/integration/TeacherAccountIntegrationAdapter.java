package edu.ai.lms.core.auth.adapter.out.integration;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.auth.app.domain.Teacher;
import edu.ai.lms.core.auth.app.port.out.LoadTeacherPort;
import edu.ai.lms.core.auth.app.port.out.SaveTeacherPort;
import edu.ai.lms.core.user.api.LoginAccountApi;
import edu.ai.lms.core.user.api.LoginAccountView;
import edu.ai.lms.core.user.api.TeacherRegistration;
import edu.ai.lms.core.user.api.UserRegistrationApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class TeacherAccountIntegrationAdapter implements LoadTeacherPort, SaveTeacherPort {

    private final LoginAccountApi loginAccountApi;
    private final UserRegistrationApi userRegistrationApi;

    @Override
    public Optional<Teacher> findByEmail(String email) {
        return loginAccountApi.findByEmailAndRole(email, Roles.TEACHER).map(this::toTeacher);
    }

    @Override
    public boolean save(Teacher teacher) {
        LoginAccountView saved = userRegistrationApi.registerTeacher(new TeacherRegistration(
                teacher.getEmail(), teacher.getPassword(), teacher.getFirstName(), teacher.getLastName(),
                teacher.getAvatarUrl(), teacher.getEmployeeId(), teacher.getPhoneNumber(),
                teacher.getDepartment(), teacher.getSpecialization()));
        teacher.setId(saved.id());
        return true;
    }

    private Teacher toTeacher(LoginAccountView account) {
        return Teacher.builder()
                .id(account.id())
                .email(account.email())
                .password(account.passwordHash())
                .firstName(account.firstName())
                .lastName(account.lastName())
                .avatarUrl(account.avatarUrl())
                .role(account.role())
                .employeeId(account.employeeId())
                .phoneNumber(account.phoneNumber())
                .department(account.department())
                .specialization(account.specialization())
                .build();
    }
}
