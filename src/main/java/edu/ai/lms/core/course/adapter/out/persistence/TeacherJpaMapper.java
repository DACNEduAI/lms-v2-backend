package edu.ai.lms.core.course.adapter.out.persistence;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.Teacher;
import edu.ai.lms.database.TeacherJpaEntity;

import java.util.ArrayList;

@Component
public class TeacherJpaMapper {

    public Teacher mapToTeacher(TeacherJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Teacher.builder()
                .employeeId(entity.getEmployeeId())
                .phoneNumber(entity.getPhoneNumber())
                .department(entity.getDepartment())
                .specialization(entity.getSpecialization())
                .courses(new ArrayList<>()) // Avoid circular reference
                .build();
    }
}
