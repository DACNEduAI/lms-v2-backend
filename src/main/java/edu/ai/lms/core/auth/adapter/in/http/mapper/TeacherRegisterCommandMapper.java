package edu.ai.lms.core.auth.adapter.in.http.mapper;

import edu.ai.lms.core.auth.adapter.in.http.request.TeacherRegisterRequest;
import edu.ai.lms.core.auth.app.domain.Teacher;
import edu.ai.lms.core.auth.app.port.in.command.TeacherRegisterCommand;
import edu.ai.lms.common.api.Roles;
import org.springframework.stereotype.Component;

@Component
public class TeacherRegisterCommandMapper {

    public TeacherRegisterCommand toCommand(TeacherRegisterRequest request) {
        Teacher teacher = Teacher.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .employeeId(request.getEmployeeId())
                .phoneNumber(request.getPhoneNumber())
                .department(request.getDepartment())
                .specialization(request.getSpecialization())
            .role(Roles.TEACHER)
                .build();

        return TeacherRegisterCommand.builder()
                .teacher(teacher)
                .build();
    }
}
