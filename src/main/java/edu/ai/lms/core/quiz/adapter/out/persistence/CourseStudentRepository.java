package edu.ai.lms.core.quiz.adapter.out.persistence;

import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
interface CourseStudentRepository {

    boolean existsByCourseIdAndStudentId(@Param("courseId") UUID courseId, @Param("studentId") UUID studentId);
}
