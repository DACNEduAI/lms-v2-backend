package edu.ai.lms.core.auth.adapter.out.persistence;

import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import edu.ai.lms.core.auth.adapter.in.internal.LoadAuthenticatedUserApi;
import edu.ai.lms.core.auth.adapter.out.persistence.mapper.UserJdbcMapper;
import edu.ai.lms.core.auth.app.domain.User;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class AuthenticatedUserPersistenceAdapter implements LoadAuthenticatedUserApi {

    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final UserJdbcMapper userJdbcMapper;

    @Override
    public Optional<User> getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            String email = userDetails.getUsername();

            // Try to find as teacher first
            Optional<User> teacher = teacherRepository.findByEmail(email)
                    .map(userJdbcMapper::mapToTeacher)
                    .map(t -> (User) t);
            if (teacher.isPresent()) {
                return teacher;
            }

            // Then try to find as student
            return studentRepository.findByEmail(email)
                    .map(userJdbcMapper::mapToStudent)
                    .map(s -> (User) s);
        }

        return Optional.empty();
    }
}
