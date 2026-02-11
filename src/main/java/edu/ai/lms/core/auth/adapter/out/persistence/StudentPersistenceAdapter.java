package edu.ai.lms.core.auth.adapter.out.persistence;

import java.util.Optional;

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
    private final StudentRepository studentRepository;
    private final UserJdbcMapper userJdbcMapper;

    @Override
    public boolean save(Student student) {
        studentRepository.save(userJdbcMapper.mapToStudentJdbcEntity(student));
        return true;
    }

    @Override
    public Optional<Student> findByEmail(String email) {
        return studentRepository.findByEmail(email)
                .map(userJdbcMapper::mapToStudent);
    }
}
