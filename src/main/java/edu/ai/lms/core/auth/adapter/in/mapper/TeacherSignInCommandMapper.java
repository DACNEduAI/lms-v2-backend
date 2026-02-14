package edu.ai.lms.core.auth.adapter.in.mapper;

import edu.ai.lms.core.auth.adapter.in.request.TeacherSignInRequest;
import edu.ai.lms.core.auth.app.port.in.command.TeacherSignInCommand;
import org.springframework.stereotype.Component;

@Component
public class TeacherSignInCommandMapper {

    public TeacherSignInCommand toCommand(TeacherSignInRequest request) {
        return TeacherSignInCommand.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build();
    }
}
