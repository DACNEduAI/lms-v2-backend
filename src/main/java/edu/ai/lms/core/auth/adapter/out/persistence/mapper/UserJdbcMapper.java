package edu.ai.lms.core.auth.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.Student;
import edu.ai.lms.core.auth.app.domain.Teacher;
import edu.ai.lms.database.StudentJdbcEntity;
import edu.ai.lms.database.TeacherJdbcEntity;

@Component
public class UserJdbcMapper {

    public StudentJdbcEntity mapToStudentJdbcEntity(Student student) {
        return StudentJdbcEntity.builder()
                .id(student.getId())
                .email(student.getEmail())
                .password(student.getPassword())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .avatarUrl(student.getAvatarUrl())
                .enrollmentYear(student.getEnrollmentYear())
                .role(mapToJdbcRole(student.getRole()))
                .build();
    }

    public Student mapToStudent(StudentJdbcEntity entity) {
        return Student.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .avatarUrl(entity.getAvatarUrl())
                .enrollmentYear(entity.getEnrollmentYear())
                .role(mapToDomainRole(entity.getRole()))
                .build();
    }

    public TeacherJdbcEntity mapToTeacherJdbcEntity(Teacher teacher) {
        return TeacherJdbcEntity.builder()
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
                .role(mapToTeacherJdbcRole(teacher.getRole()))
                .build();
    }

    public Teacher mapToTeacher(TeacherJdbcEntity entity) {
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
                .role(mapToDomainRole(entity.getRole()))
                .build();
    }

    private StudentJdbcEntity.Roles mapToJdbcRole(Roles role) {
        if (role == null) {
            return StudentJdbcEntity.Roles.STUDENT;
        }
        return switch (role) {
            case ADMIN -> StudentJdbcEntity.Roles.ADMIN;
            case TEACHER -> StudentJdbcEntity.Roles.TEACHER;
            case STUDENT -> StudentJdbcEntity.Roles.STUDENT;
        };
    }

    private TeacherJdbcEntity.Roles mapToTeacherJdbcRole(Roles role) {
        if (role == null) {
            return TeacherJdbcEntity.Roles.TEACHER;
        }
        return switch (role) {
            case ADMIN -> TeacherJdbcEntity.Roles.ADMIN;
            case TEACHER -> TeacherJdbcEntity.Roles.TEACHER;
            case STUDENT -> TeacherJdbcEntity.Roles.STUDENT;
        };
    }

    private Roles mapToDomainRole(StudentJdbcEntity.Roles role) {
        if (role == null) {
            return Roles.STUDENT;
        }
        return switch (role) {
            case ADMIN -> Roles.ADMIN;
            case TEACHER -> Roles.TEACHER;
            case STUDENT -> Roles.STUDENT;
        };
    }

    private Roles mapToDomainRole(TeacherJdbcEntity.Roles role) {
        if (role == null) {
            return Roles.TEACHER;
        }
        return switch (role) {
            case ADMIN -> Roles.ADMIN;
            case TEACHER -> Roles.TEACHER;
            case STUDENT -> Roles.STUDENT;
        };
    }
}
