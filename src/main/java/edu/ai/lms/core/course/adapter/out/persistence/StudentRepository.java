package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.ai.lms.database.StudentJdbcTable;

@Repository
interface StudentRepository extends CrudRepository<StudentJdbcTable, UUID> {
    @Query("SELECT * FROM students WHERE email = :email")
    Optional<StudentJdbcTable> findByEmail(@Param("email") String email);
}
