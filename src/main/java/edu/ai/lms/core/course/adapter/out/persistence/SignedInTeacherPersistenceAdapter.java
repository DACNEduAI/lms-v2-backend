package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.Teacher;
import edu.ai.lms.core.course.app.port.out.LoadSignedInTeacherPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class SignedInTeacherPersistenceAdapter implements LoadSignedInTeacherPort {

    private final TeacherRepository teacherRepository;
    private final TeacherJdbcMapper teacherJdbcMapper;

    @Override
    public Optional<Teacher> findAuthenticatedTeacher() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            
            return teacherRepository.findByEmail(username)
                    .map(teacherJdbcMapper::mapToTeacher);
        }
        
        return Optional.empty();
    }
}
