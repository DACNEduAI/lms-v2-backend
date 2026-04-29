package edu.ai.lms.core.course.app.port.out;

import java.util.List;
import java.util.UUID;
import edu.ai.lms.core.auth.app.domain.UserAuth;

/**
 * Port for loading user details from the authentication/user module.
 * Used by the course module to display participant names and emails.
 */
public interface LoadUsersPort {
    List<UserAuth> findAllByIds(List<UUID> ids);
}
