package edu.ai.lms.core.auth.adapter.out.persistence;

import java.util.Optional;

import edu.ai.lms.common.Roles;
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

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final UserJdbcMapper userJdbcMapper;

    @Override
    public Optional<User> getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            String email = userDetails.getUsername();

            return userRepository.findByEmail(email)
                    .flatMap(userEntity -> {
                        if (userEntity.getRole() == Roles.TEACHER) {
                            return teacherRepository.findById(userEntity.getId())
                                    .map(teacher -> (User) userJdbcMapper.mapToTeacher(userEntity, teacher));
                        } else if (userEntity.getRole() == Roles.STUDENT) {
                            return studentRepository.findById(userEntity.getId())
                                    .map(student -> (User) userJdbcMapper.mapToStudent(userEntity, student));
                        }
                        return Optional.<User>empty();
                    });
        }

        return Optional.empty();
    }
}
