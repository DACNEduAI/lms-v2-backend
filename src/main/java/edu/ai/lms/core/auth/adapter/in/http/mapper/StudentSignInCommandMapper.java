package edu.ai.lms.core.auth.adapter.in.http.mapper;

import edu.ai.lms.core.auth.adapter.in.http.request.StudentSignInRequest;
import edu.ai.lms.core.auth.app.port.in.command.StudentSignInCommand;
import org.springframework.stereotype.Component;

@Component
public class StudentSignInCommandMapper {

    public StudentSignInCommand toCommand(StudentSignInRequest request) {
        return StudentSignInCommand.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build();
    }
}
