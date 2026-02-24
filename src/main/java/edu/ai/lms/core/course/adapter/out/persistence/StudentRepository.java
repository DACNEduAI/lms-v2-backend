package edu.ai.lms.core.course.adapter.out.persistence;

import edu.ai.lms.database.TeacherJdbcTable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository {
    @Query("SELECT * FROM students WHERE email = :email")
    Optional<TeacherJdbcTable> findByEmail(@Param("email") String email);
}
