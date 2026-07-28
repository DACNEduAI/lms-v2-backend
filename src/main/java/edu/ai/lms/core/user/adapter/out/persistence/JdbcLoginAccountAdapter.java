package edu.ai.lms.core.user.adapter.out.persistence;

import edu.ai.lms.common.api.Roles;
import edu.ai.lms.core.user.api.LoginAccountApi;
import edu.ai.lms.core.user.api.LoginAccountView;
import edu.ai.lms.core.user.api.StudentRegistration;
import edu.ai.lms.core.user.api.TeacherRegistration;
import edu.ai.lms.core.user.api.UserRegistrationApi;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class JdbcLoginAccountAdapter implements LoginAccountApi, UserRegistrationApi {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Optional<LoginAccountView> findByEmailAndRole(String email, Roles role) {
        var accounts = jdbc.query("""
                        SELECT u.id, u.email, u.password, u.first_name, u.last_name, u.avatar_url, u.role,
                               s.student_id, s.enrollment_year,
                               t.employee_id, t.phone_number, t.department, t.specialization
                        FROM edu.users u
                        LEFT JOIN edu.students s ON s.user_id = u.id
                        LEFT JOIN edu.teachers t ON t.user_id = u.id
                        WHERE u.email = :email AND u.role = :role
                        """,
                new MapSqlParameterSource(Map.of("email", email, "role", role.name())),
                (rs, rowNum) -> new LoginAccountView(
                        rs.getObject("id", UUID.class),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("avatar_url"),
                        Roles.valueOf(rs.getString("role")),
                        rs.getString("student_id"),
                        rs.getObject("enrollment_year", Long.class),
                        rs.getString("employee_id"),
                        rs.getString("phone_number"),
                        rs.getString("department"),
                        rs.getString("specialization")));
        return accounts.stream().findFirst();
    }

    @Override
    @Transactional
    public LoginAccountView registerStudent(StudentRegistration registration) {
        UUID id = insertUser(registration.email(), registration.passwordHash(), registration.firstName(),
                registration.lastName(), registration.avatarUrl(), Roles.STUDENT);
        jdbc.update("""
                        INSERT INTO edu.students (user_id, student_id, enrollment_year)
                        VALUES (:id, :studentId, :enrollmentYear)
                        """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("studentId", registration.studentId())
                        .addValue("enrollmentYear", registration.enrollmentYear()));
        return findByEmailAndRole(registration.email(), Roles.STUDENT).orElseThrow();
    }

    @Override
    @Transactional
    public LoginAccountView registerTeacher(TeacherRegistration registration) {
        UUID id = insertUser(registration.email(), registration.passwordHash(), registration.firstName(),
                registration.lastName(), registration.avatarUrl(), Roles.TEACHER);
        jdbc.update("""
                        INSERT INTO edu.teachers
                            (user_id, employee_id, phone_number, department, specialization)
                        VALUES (:id, :employeeId, :phoneNumber, :department, :specialization)
                        """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("employeeId", registration.employeeId())
                        .addValue("phoneNumber", registration.phoneNumber())
                        .addValue("department", registration.department())
                        .addValue("specialization", registration.specialization()));
        return findByEmailAndRole(registration.email(), Roles.TEACHER).orElseThrow();
    }

    private UUID insertUser(
            String email,
            String passwordHash,
            String firstName,
            String lastName,
            String avatarUrl,
            Roles role
    ) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                        INSERT INTO edu.users
                            (id, email, password, first_name, last_name, avatar_url, role)
                        VALUES (:id, :email, :password, :firstName, :lastName, :avatarUrl, :role)
                        """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("email", email)
                        .addValue("password", passwordHash)
                        .addValue("firstName", firstName)
                        .addValue("lastName", lastName)
                        .addValue("avatarUrl", avatarUrl)
                        .addValue("role", role.name()));
        return id;
    }
}
