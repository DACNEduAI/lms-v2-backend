package edu.ai.lms.core.auth.adapter.in.http.mapper;

import edu.ai.lms.core.auth.adapter.in.http.request.TeacherRegisterRequest;
import edu.ai.lms.core.auth.app.domain.Teacher;
import edu.ai.lms.core.auth.app.port.in.command.TeacherRegisterCommand;
import org.springframework.stereotype.Component;

@Component
public class TeacherRegisterCommandMapper {

    public TeacherRegisterCommand toCommand(TeacherRegisterRequest request) {
        Teacher teacher = Teacher.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .department(request.getDepartment())
                .specialization(request.getSpecialization())
                .build();

        return TeacherRegisterCommand.builder()
                .teacher(teacher)
                .build();
    }
}
