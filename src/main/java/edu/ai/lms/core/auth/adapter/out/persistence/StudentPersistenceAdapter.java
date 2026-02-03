package edu.ai.lms.core.auth.adapter.out.persistence;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.ai.lms.core.auth.app.port.out.LoadStudentPort;
import edu.ai.lms.core.auth.app.port.out.SaveStudentPort;
import edu.ai.lms.core.auth.app.domain.Student;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
class StudentPersistenceAdapter implements
        SaveStudentPort,
        LoadStudentPort
{
    @Autowired
    private final UserRepository userRepository;
    private final UserJpaMapper userJpaMapper;

    @Override
    public boolean save(Student student) {
        userRepository.save(userJpaMapper.mapToStudentJpaEntity(student));
        return true;
    }

    @Override
    public Optional<Student> findByEmail(String email) {
        return userRepository.findStudentByEmail(email)
                .map(userJpaMapper::mapToStudent);
    }
}
