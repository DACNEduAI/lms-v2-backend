package edu.ai.lms.core.course.adapter.out.integration;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.port.out.LoadStudentsByEmailPort;
import edu.ai.lms.core.user.api.UserDirectoryApi;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class StudentLookupIntegrationAdapter implements LoadStudentsByEmailPort {

    private final UserDirectoryApi userDirectoryApi;

    @Override
    public Map<String, UUID> findStudentIdsByEmails(List<String> emails) {
        return userDirectoryApi.findStudentIdsByEmails(emails);
    }
}
