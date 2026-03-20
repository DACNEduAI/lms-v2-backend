package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import edu.ai.lms.core.course.app.domain.Student;

@Mapper
interface StudentLookupRepository {

    List<Student> findStudentsByEmailIn(@Param("emails") List<String> emails);
}
