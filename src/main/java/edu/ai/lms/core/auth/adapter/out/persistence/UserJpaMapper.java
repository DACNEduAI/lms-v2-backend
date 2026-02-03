package edu.ai.lms.core.auth.adapter.out.persistence;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.auth.app.domain.Roles;
import edu.ai.lms.core.auth.app.domain.Student;
import edu.ai.lms.core.auth.app.domain.Teacher;
import edu.ai.lms.database.StudentJpaEntity;
import edu.ai.lms.database.TeacherJpaEntity;
import edu.ai.lms.database.UserJpaEntity;

@Component
class UserJpaMapper {

    public StudentJpaEntity mapToStudentJpaEntity(Student student) {
        return StudentJpaEntity.builder()
                .id(student.getId())
                .email(student.getEmail())
                .password(student.getPassword())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .avatarUrl(student.getAvatarUrl())
                .enrollmentYear(student.getEnrollmentYear())
                .role(mapToJpaRole(student.getRole()))
                .build();
    }

    public Student mapToStudent(StudentJpaEntity entity) {
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

    private UserJpaEntity.Roles mapToJpaRole(Roles role) {
        if (role == null) {
            return UserJpaEntity.Roles.STUDENT;
        }
        return switch (role) {
            case ADMIN -> UserJpaEntity.Roles.ADMIN;
            case TEACHER -> UserJpaEntity.Roles.TEACHER;
            case STUDENT -> UserJpaEntity.Roles.STUDENT;
        };
    }

    private Roles mapToDomainRole(UserJpaEntity.Roles role) {
        if (role == null) {
            return Roles.STUDENT;
        }
        return switch (role) {
            case ADMIN -> Roles.ADMIN;
            case TEACHER -> Roles.TEACHER;
            case STUDENT -> Roles.STUDENT;
        };
    }

    public TeacherJpaEntity mapToTeacherJpaEntity(Teacher teacher) {
        return TeacherJpaEntity.builder()
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
                .role(mapToJpaRole(teacher.getRole()))
                .build();
    }

    public Teacher mapToTeacher(TeacherJpaEntity entity) {
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
}
