package edu.ai.lms.core.auth.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import edu.ai.lms.database.TeacherJdbcEntity;
import org.springframework.stereotype.Repository;

/**
 * Package-private repository interface for teachers.
 * Only classes within the persistence package can access this repository.
 */
@Repository
interface TeacherRepository extends CrudRepository<TeacherJdbcEntity, UUID> {

    @Query("SELECT * FROM teachers WHERE email = :email")
    Optional<TeacherJdbcEntity> findByEmail(@Param("email") String email);
}
