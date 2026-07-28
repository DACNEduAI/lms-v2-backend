package edu.ai.lms.core.user.api;

public interface UserRegistrationApi {

    LoginAccountView registerStudent(StudentRegistration registration);

    LoginAccountView registerTeacher(TeacherRegistration registration);
}
