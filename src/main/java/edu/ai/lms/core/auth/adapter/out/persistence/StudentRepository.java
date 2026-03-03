package edu.ai.lms.core.auth.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.ai.lms.database.StudentJdbcTable;

/**
 * Package-private repository interface for students.
 * Only classes within the persistence package can access this repository.
 */
@Repository
interface StudentRepository extends CrudRepository<StudentJdbcTable, UUID> {

    @Modifying
    @Query("INSERT INTO students (user_id, enrollment_year) VALUES (:userId, :enrollmentYear)")
    void insertStudent(@Param("userId") UUID userId, @Param("enrollmentYear") Long enrollmentYear);
}
