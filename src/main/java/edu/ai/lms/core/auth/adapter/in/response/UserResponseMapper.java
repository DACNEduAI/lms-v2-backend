package edu.ai.lms.core.auth.adapter.in.response;

import edu.ai.lms.core.auth.domain.Student;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserResponseMapper {
    StudentRegisterResponse toStudentRegisterResponseMapper(Student student) {
        return null;
    }
}
