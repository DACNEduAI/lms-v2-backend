package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Quiz module's specific repository for course membership.
 * Renamed to QuizCourseRepository to avoid bean name collision with Course module's CourseRepository.
 */
@Mapper
interface QuizCourseRepository {

    @Select("SELECT COUNT(*) > 0 FROM enrollments WHERE course_id = #{courseId} AND user_id = #{teacherId}")
    boolean existsByCourseIdAndTeacherId(@Param("courseId") UUID courseId, @Param("teacherId") UUID teacherId);

    @Select("SELECT COUNT(*) > 0 FROM enrollments WHERE course_id = #{courseId} AND user_id = #{studentId}")
    boolean existsByCourseIdAndStudentId(@Param("courseId") UUID courseId, @Param("studentId") UUID studentId);

    /**
     * Returns all student (user) IDs enrolled in the given course.
     * Used when broadcasting quiz notifications to all students in a course.
     */
    @Select("SELECT e.user_id FROM enrollments e INNER JOIN users u ON e.user_id = u.id WHERE e.course_id = #{courseId} AND u.role = 'STUDENT'")
    List<UUID> findStudentIdsByCourseId(@Param("courseId") UUID courseId);
}
