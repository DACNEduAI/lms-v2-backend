package edu.ai.lms.core.auth.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.auth.app.domain.Student;
import edu.ai.lms.core.auth.app.domain.Teacher;
import edu.ai.lms.database.StudentJdbcTable;
import edu.ai.lms.database.TeacherJdbcTable;

@Component
public class UserJdbcMapper {

    public StudentJdbcTable mapToStudentJdbcEntity(Student student) {
        return StudentJdbcTable.builder()
                .id(student.getId())
                .email(student.getEmail())
                .password(student.getPassword())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .avatarUrl(student.getAvatarUrl())
                .enrollmentYear(student.getEnrollmentYear())
                .role(student.getRole())
                .build();
    }

    public Student mapToStudent(StudentJdbcTable entity) {
        return Student.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .avatarUrl(entity.getAvatarUrl())
                .enrollmentYear(entity.getEnrollmentYear())
                .role(entity.getRole())
                .build();
    }

    public TeacherJdbcTable mapToTeacherJdbcEntity(Teacher teacher) {
        return TeacherJdbcTable.builder()
                .id(teacher.getId())
                .email(teacher.getEmail())
                .password(teacher.getPassword())
                .firstName(teacher.getFirstName())
                .lastName(teacher.getLastName())
                .avatarUrl(teacher.getAvatarUrl())
                .employeeId(teacher.getEmployeeId())
                .phoneNumber(teacher.getPhoneNumber())
                .department(teacher.getDepartment())
                .specialization(teacher.getSpecialization())
                .role(teacher.getRole())
                .build();
    }

    public Teacher mapToTeacher(TeacherJdbcTable entity) {
        return Teacher.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .avatarUrl(entity.getAvatarUrl())
                .employeeId(entity.getEmployeeId())
                .phoneNumber(entity.getPhoneNumber())
                .department(entity.getDepartment())
                .specialization(entity.getSpecialization())
                .role(entity.getRole())
                .build();
    }
}
