package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.out.persistence.mapper.StudentJdbcMapper;
import edu.ai.lms.core.course.app.domain.Student;
import edu.ai.lms.core.course.app.port.out.LoadSignedInStudentPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class SignedInStudentPersistenceAdapter implements LoadSignedInStudentPort {

    private final StudentRepository studentRepository;
    private final StudentJdbcMapper studentJdbcMapper;

    @Override
    public Optional<Student> findAuthenticatedStudent() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            
            return studentRepository.findByEmail(username)
                    .map(studentJdbcMapper::mapToStudent);
        }
        
        return Optional.empty();
    }
}
