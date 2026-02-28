package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.ai.lms.database.CourseTeacherJdbcTable;

@Repository
interface CourseTeacherRepository extends CrudRepository<CourseTeacherJdbcTable, UUID> {

    @Query("SELECT teacher_id FROM course_teachers WHERE course_id = :courseId")
    List<UUID> findTeacherIdsByCourseId(@Param("courseId") UUID courseId);

    @Query("SELECT course_id FROM course_teachers WHERE teacher_id = :teacherId")
    List<UUID> findCourseIdsByTeacherId(@Param("teacherId") UUID teacherId);

    @Modifying
    @Query("DELETE FROM course_teachers WHERE course_id = :courseId")
    void deleteByCourseId(@Param("courseId") UUID courseId);

    @Query("SELECT COUNT(*) > 0 FROM course_teachers WHERE course_id = :courseId AND teacher_id = :teacherId")
    boolean existsByCourseIdAndTeacherId(@Param("courseId") UUID courseId, @Param("teacherId") UUID teacherId);
}
