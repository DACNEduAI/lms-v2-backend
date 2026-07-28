package edu.ai.lms.core.user.adapter.out.persistence;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.user.api.UserDirectoryApi;
import edu.ai.lms.core.user.api.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class JdbcUserDirectoryAdapter implements UserDirectoryApi {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<UserSummary> findAllByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jdbc.query("""
                        SELECT id, email, first_name, last_name, avatar_url, role
                        FROM edu.users
                        WHERE id IN (:ids)
                        """,
                new MapSqlParameterSource("ids", ids),
                (rs, rowNum) -> new UserSummary(
                        rs.getObject("id", UUID.class),
                        rs.getString("email"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("avatar_url"),
                        Roles.valueOf(rs.getString("role"))));
    }

    @Override
    public Map<String, UUID> findStudentIdsByEmails(List<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return Map.of();
        }
        Map<String, UUID> result = new LinkedHashMap<>();
        jdbc.query("""
                        SELECT id, email
                        FROM edu.users
                        WHERE role = 'STUDENT' AND email IN (:emails)
                        """,
                new MapSqlParameterSource("emails", emails),
                (RowCallbackHandler) rs ->
                        result.put(rs.getString("email"), rs.getObject("id", UUID.class)));
        return Map.copyOf(result);
    }
}
