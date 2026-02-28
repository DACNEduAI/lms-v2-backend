package edu.ai.lms.core.course.adapter.out.persistence.mapper;

import java.util.ArrayList;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.Student;
import edu.ai.lms.database.StudentJdbcTable;

@Component
public class StudentJdbcMapper {

    /// This function will map StudentJdbcEntity to Student domain entity WITHOUT courses list
    public Student mapToStudent(StudentJdbcTable entity) {
        if (entity == null) {
            return null;
        }
        return Student.builder()
                .id(entity.getId())
                .enrollmentYear(entity.getEnrollmentYear())
                .courses(new ArrayList<>())
                .masteredLearningOutcomes(new ArrayList<>())
                .build();
    }
}
