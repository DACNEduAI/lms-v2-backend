package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.ai.lms.database.CourseJdbcEntity;

/**
 * Package-private repository interface for courses.
 * Only classes within the persistence package can access this repository.
 */
@Repository
interface CourseRepository extends CrudRepository<CourseJdbcEntity, UUID> {

    @Query("SELECT * FROM courses WHERE course_code = :courseCode")
    Optional<CourseJdbcEntity> findByCourseCode(@Param("courseCode") String courseCode);

    @Query("SELECT * FROM courses WHERE teacher_id = :teacherId")
    List<CourseJdbcEntity> findAllByTeacherId(@Param("teacherId") UUID teacherId);
}
