package edu.ai.lms.core.course.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.Teacher;
import edu.ai.lms.database.TeacherJdbcTable;

import java.util.ArrayList;

@Component
public class TeacherJdbcMapper {

    /// This function will map TeacherJdbcEntity to Teacher domain entity WITHOUT courses list
    public Teacher mapToTeacher(TeacherJdbcTable entity) {
        if (entity == null) {
            return null;
        }
        return Teacher.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployeeId())
                .phoneNumber(entity.getPhoneNumber())
                .department(entity.getDepartment())
                .specialization(entity.getSpecialization())
                .courses(new ArrayList<>())
                .build();
    }
}
