package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
interface CourseRepository {

    boolean existsByCourseIdAndTeacherId(@Param("courseId") UUID courseId, @Param("teacherId") UUID teacherId);

    boolean existsByCourseIdAndStudentId(@Param("courseId") UUID courseId, @Param("studentId") UUID studentId);
}