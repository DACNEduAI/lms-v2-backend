package edu.ai.lms.core.course.adapter.out.integration;

import edu.ai.lms.core.course.app.port.out.LoadUsersPort;
import edu.ai.lms.core.course.app.port.out.UserSummaryView;
import edu.ai.lms.core.user.api.UserDirectoryApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class UserDirectoryAdapter implements LoadUsersPort {

    private final UserDirectoryApi userDirectoryApi;

    @Override
    public List<UserSummaryView> findAllByIds(List<UUID> ids) {
        return userDirectoryApi.findAllByIds(ids).stream()
                .map(user -> new UserSummaryView(
                        user.id(),
                        user.email(),
                        user.firstName(),
                        user.lastName(),
                        user.avatarUrl(),
                        user.role()))
                .toList();
    }
}
