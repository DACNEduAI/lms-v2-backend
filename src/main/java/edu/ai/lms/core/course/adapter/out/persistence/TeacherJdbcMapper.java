package edu.ai.lms.core.course.adapter.out.persistence;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.Teacher;
import edu.ai.lms.database.TeacherJdbcEntity;

import java.util.ArrayList;

@Component
public class TeacherJdbcMapper {

    public Teacher mapToTeacher(TeacherJdbcEntity entity) {
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
