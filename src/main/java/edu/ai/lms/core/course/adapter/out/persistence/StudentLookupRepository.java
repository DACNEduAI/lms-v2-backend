package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import edu.ai.lms.database.UserJdbcTable;

interface StudentLookupRepository extends Repository<UserJdbcTable, UUID> {

    @Query("SELECT u.* FROM users u INNER JOIN students s ON u.id = s.user_id WHERE u.email IN (:emails)")
    List<UserJdbcTable> findStudentsByEmailIn(@Param("emails") List<String> emails);
}
