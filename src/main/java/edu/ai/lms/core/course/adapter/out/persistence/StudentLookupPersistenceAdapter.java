package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.port.out.LoadStudentsByEmailPort;
import edu.ai.lms.database.UserJdbcTable;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class StudentLookupPersistenceAdapter implements LoadStudentsByEmailPort {

    private final StudentLookupRepository studentLookupRepository;

    @Override
    public Map<String, UUID> findStudentIdsByEmails(List<String> emails) {
        List<UserJdbcTable> users = studentLookupRepository.findStudentsByEmailIn(emails);
        Map<String, UUID> result = new HashMap<>();
        for (UserJdbcTable user : users) {
            result.put(user.getEmail(), user.getId());
        }
        return result;
    }
}
