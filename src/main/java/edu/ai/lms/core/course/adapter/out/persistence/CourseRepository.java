package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.ai.lms.database.CourseJdbcTable;

/**
 * Package-private repository interface for courses.
 * Only classes within the persistence package can access this repository.
 */
@Repository
interface CourseRepository extends CrudRepository<CourseJdbcTable, UUID> {

    @Query("SELECT * FROM courses WHERE course_code = :courseCode")
    Optional<CourseJdbcTable> findByCourseCode(@Param("courseCode") String courseCode);

    @Query("SELECT c.* FROM courses c INNER JOIN course_teachers ct ON c.id = ct.course_id WHERE ct.teacher_id = :teacherId")
    List<CourseJdbcTable> findAllByTeacherId(@Param("teacherId") UUID teacherId);

    @Query("SELECT c.* FROM courses c INNER JOIN course_students cs ON c.id = cs.course_id WHERE cs.student_id = :studentId")
    List<CourseJdbcTable> findAllByStudentId(@Param("studentId") UUID studentId);
}
