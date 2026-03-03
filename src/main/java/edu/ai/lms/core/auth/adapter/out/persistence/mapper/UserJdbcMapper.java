package edu.ai.lms.core.auth.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.auth.app.domain.Student;
import edu.ai.lms.core.auth.app.domain.Teacher;
import edu.ai.lms.core.auth.app.domain.User;
import edu.ai.lms.database.StudentJdbcTable;
import edu.ai.lms.database.TeacherJdbcTable;
import edu.ai.lms.database.UserJdbcTable;

@Component
public class UserJdbcMapper {

    public UserJdbcTable mapToUserJdbcEntity(User user) {
        return UserJdbcTable.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .build();
    }

    public Student mapToStudent(UserJdbcTable userEntity, StudentJdbcTable studentEntity) {
        return Student.builder()
                .id(userEntity.getId())
                .email(userEntity.getEmail())
                .password(userEntity.getPassword())
                .firstName(userEntity.getFirstName())
                .lastName(userEntity.getLastName())
                .avatarUrl(userEntity.getAvatarUrl())
                .studentId(studentEntity.getStudentId())
                .enrollmentYear(studentEntity.getEnrollmentYear())
                .role(userEntity.getRole())
                .build();
    }

    public Teacher mapToTeacher(UserJdbcTable userEntity, TeacherJdbcTable teacherEntity) {
        return Teacher.builder()
                .id(userEntity.getId())
                .email(userEntity.getEmail())
                .password(userEntity.getPassword())
                .firstName(userEntity.getFirstName())
                .lastName(userEntity.getLastName())
                .avatarUrl(userEntity.getAvatarUrl())
                .employeeId(teacherEntity.getEmployeeId())
                .phoneNumber(teacherEntity.getPhoneNumber())
                .department(teacherEntity.getDepartment())
                .specialization(teacherEntity.getSpecialization())
                .role(userEntity.getRole())
                .build();
    }
}
