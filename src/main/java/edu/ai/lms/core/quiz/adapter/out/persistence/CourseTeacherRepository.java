package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.ai.lms.database.CourseTeacherJdbcTable;

@Repository
interface CourseTeacherRepository extends CrudRepository<CourseTeacherJdbcTable, UUID> {

    @Query("SELECT COUNT(*) > 0 FROM course_teachers WHERE course_id = :courseId AND teacher_id = :teacherId")
    boolean existsByCourseIdAndTeacherId(@Param("courseId") UUID courseId, @Param("teacherId") UUID teacherId);
}
