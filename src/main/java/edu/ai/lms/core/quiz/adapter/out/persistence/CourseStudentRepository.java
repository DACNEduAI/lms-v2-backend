package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.ai.lms.database.CourseStudentJdbcTable;

@Repository
interface CourseStudentRepository extends CrudRepository<CourseStudentJdbcTable, UUID> {

    @Query("SELECT COUNT(*) > 0 FROM course_students WHERE course_id = :courseId AND student_id = :studentId")
    boolean existsByCourseIdAndStudentId(@Param("courseId") UUID courseId, @Param("studentId") UUID studentId);
}
