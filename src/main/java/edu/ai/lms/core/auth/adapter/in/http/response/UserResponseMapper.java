package edu.ai.lms.core.auth.adapter.in.http.response;

import edu.ai.lms.core.auth.app.domain.Student;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserResponseMapper {
    StudentRegisterResponse toStudentRegisterResponseMapper(Student student) {
        return null;
    }
}
