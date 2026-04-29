package edu.ai.lms.core.auth.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.course.app.port.out.LoadUsersPort;
import lombok.RequiredArgsConstructor;

/**
 * Persistence adapter that implements the LoadUsersPort from the course module.
 * This bridge allows the course module to load user details without knowing the auth module's internals.
 */
@Component
@RequiredArgsConstructor
class UserPersistenceAdapter implements LoadUsersPort {

    private final UserRepository userRepository;

    @Override
    public List<UserAuth> findAllByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return userRepository.findByIds(ids);
    }
}
