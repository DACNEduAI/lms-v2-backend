package edu.ai.lms.core.user.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface UserDirectoryApi {

    List<UserSummary> findAllByIds(List<UUID> ids);

    Map<String, UUID> findStudentIdsByEmails(List<String> emails);
}
