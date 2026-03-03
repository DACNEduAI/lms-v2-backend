package edu.ai.lms.core.auth.adapter.out.persistence;

import java.util.Optional;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.adapter.out.persistence.mapper.UserJdbcMapper;
import edu.ai.lms.database.UserJdbcTable;
import org.springframework.stereotype.Component;

import edu.ai.lms.core.auth.app.domain.Student;
import edu.ai.lms.core.auth.app.port.out.LoadStudentPort;
import edu.ai.lms.core.auth.app.port.out.SaveStudentPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class StudentPersistenceAdapter implements
        SaveStudentPort,
        LoadStudentPort
{
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final UserJdbcMapper userJdbcMapper;

    @Override
    public boolean save(Student student) {
        UserJdbcTable userEntity = userJdbcMapper.mapToUserJdbcEntity(student);
        UserJdbcTable savedUser = userRepository.save(userEntity);
        studentRepository.insertStudent(
                savedUser.getId(),
                student.getEnrollmentYear()
        );
        return true;
    }

    @Override
    public Optional<Student> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getRole() == Roles.STUDENT)
                .flatMap(user -> studentRepository.findById(user.getId())
                        .map(student -> userJdbcMapper.mapToStudent(user, student)));
    }
}
