package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
interface CourseTeacherRepository {

    List<UUID> findTeacherIdsByCourseId(@Param("courseId") UUID courseId);

    List<UUID> findCourseIdsByTeacherId(@Param("teacherId") UUID teacherId);

    void deleteByCourseId(@Param("courseId") UUID courseId);

    boolean existsByCourseIdAndTeacherId(@Param("courseId") UUID courseId, @Param("teacherId") UUID teacherId);
}
