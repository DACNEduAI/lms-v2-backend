package edu.ai.lms.core.auth.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import edu.ai.lms.core.auth.app.domain.Student;

/**
 * Package-private repository interface for students.
 * Only classes within the persistence package can access this repository.
 */
@Mapper
interface StudentRepository {

    Optional<Student> findById(@Param("userId") UUID userId);

    Optional<Student> findByEmail(@Param("email") String email);

    void insertStudent(Student student);
}
