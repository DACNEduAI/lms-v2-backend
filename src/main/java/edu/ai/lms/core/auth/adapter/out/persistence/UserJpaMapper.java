package edu.ai.lms.core.auth.adapter.out.persistence;

import edu.ai.lms.database.StudentJpaEntity;
import edu.ai.lms.core.auth.domain.Student;
import org.springframework.stereotype.Component;

@Component
class UserJpaMapper {
    public StudentJpaEntity mapToStudentJpaEntity(Student student) {
        return StudentJpaEntity
                .builder()
                .enrollmentYear(student.getEnrollmentYear())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .email(student.getEmail())
                .build();
    }
}
