package edu.ai.lms.core.auth.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import edu.ai.lms.database.StudentJdbcEntity;
import org.springframework.stereotype.Repository;

/**
 * Package-private repository interface for students.
 * Only classes within the persistence package can access this repository.
 */
@Repository
interface StudentRepository extends CrudRepository<StudentJdbcEntity, UUID> {

    @Query("SELECT * FROM students WHERE email = :email")
    Optional<StudentJdbcEntity> findByEmail(@Param("email") String email);
}
