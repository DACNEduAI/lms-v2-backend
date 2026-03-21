package edu.ai.lms.core.auth.adapter.out.persistence;

import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import edu.ai.lms.core.auth.app.domain.Teacher;

/**
 * Package-private repository interface for teachers.
 * Only classes within the persistence package can access this repository.
 */
@Mapper
interface TeacherRepository {

    java.util.Optional<Teacher> findById(@Param("userId") UUID userId);

    java.util.Optional<Teacher> findByEmail(@Param("email") String email);

    void insertTeacher(Teacher teacher);
}
