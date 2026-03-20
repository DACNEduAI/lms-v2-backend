package edu.ai.lms.core.auth.adapter.out.persistence;

import java.util.Optional;

import edu.ai.lms.common.Roles;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import edu.ai.lms.core.auth.adapter.in.internal.LoadAuthenticatedUserApi;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class AuthenticatedUserPersistenceAdapter implements LoadAuthenticatedUserApi {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    @Override
    public Optional<UserAuth> getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserAuth user) {
            return Optional.of(user);
        }

        if (principal instanceof UserDetails userDetails) {
            String email = userDetails.getUsername();

            return userRepository.findByEmail(email)
                    .flatMap(user -> {
                        if (user.getRole() == Roles.TEACHER) {
                            return teacherRepository.findById(user.getId()).map(teacher -> (UserAuth) teacher);
                        } else if (user.getRole() == Roles.STUDENT) {
                            return studentRepository.findById(user.getId()).map(student -> (UserAuth) student);
                        }
                        return Optional.<UserAuth>empty();
                    });
        }

        return Optional.empty();
    }
}
