package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.Optional;

import edu.ai.lms.core.course.app.domain.Student;
import edu.ai.lms.core.course.app.domain.Teacher;
import org.springframework.stereotype.Component;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.course.app.port.out.LoadSignedInStudentPort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInTeacherPort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class SignedInUserPersistenceAdapter implements LoadSignedInUserPort {

    private final LoadSignedInTeacherPort loadSignedInTeacherPort;
    private final LoadSignedInStudentPort loadSignedInStudentPort;

    @Override
    public Optional<SignedInUser> findAuthenticatedUser() {
        // First try to find as teacher
        Optional<Teacher> teacherOpt = loadSignedInTeacherPort.findAuthenticatedTeacher();
        if (teacherOpt.isPresent()) {
            return Optional.of(new SignedInUser(teacherOpt.get().getId(), Roles.TEACHER));
        }
        
        // Then try to find as student
        Optional<Student> studentOpt = loadSignedInStudentPort.findAuthenticatedStudent();
        return studentOpt.map(student -> new SignedInUser(student.getId(), Roles.STUDENT));

    }
}
