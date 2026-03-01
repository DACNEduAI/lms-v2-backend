package edu.ai.lms.core.auth.adapter.in.http.mapper;

import edu.ai.lms.core.auth.adapter.in.http.request.StudentRegisterRequest;
import edu.ai.lms.core.auth.app.domain.Student;
import edu.ai.lms.core.auth.app.port.in.command.StudentRegisterCommand;
import org.springframework.stereotype.Component;

@Component
public class StudentRegisterCommandMapper {

    public StudentRegisterCommand toCommand(StudentRegisterRequest request) {
        Student student = Student.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .enrollmentYear(request.getEnrollmentYear())
                .build();

        return StudentRegisterCommand.builder()
                .student(student)
                .build();
    }
}
