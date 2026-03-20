package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import edu.ai.lms.core.course.app.domain.Course;

/**
 * Package-private repository interface for courses.
 * Only classes within the persistence package can access this repository.
 */
@Mapper
interface CourseRepository {

    Optional<Course> findById(@Param("id") UUID id);

    int insert(Course course);

    int update(Course course);

    default Course save(Course course) {
        if (course.getId() == null) {
            course.setId(java.util.UUID.randomUUID());
            insert(course);
        } else {
            int updated = update(course);
            if (updated == 0) {
                insert(course);
            }
        }
        return findById(course.getId()).orElseThrow();
    }

    void deleteById(@Param("id") UUID id);

    Optional<Course> findByCourseCode(@Param("courseCode") String courseCode);

    List<Course> findAllByTeacherId(@Param("teacherId") UUID teacherId);

    List<Course> findAllByStudentId(@Param("studentId") UUID studentId);

    List<UUID> findTeacherIdsByCourseId(@Param("courseId") UUID courseId);

    List<UUID> findStudentIdsByCourseId(@Param("courseId") UUID courseId);

    void deleteCourseStudentsByCourseIdAndStudentIds(
            @Param("courseId") UUID courseId,
            @Param("studentIds") List<UUID> studentIds);

    void insertCourseTeacher(@Param("courseId") UUID courseId, @Param("teacherId") UUID teacherId);

    void insertCourseStudent(@Param("courseId") UUID courseId, @Param("studentId") UUID studentId);
}
