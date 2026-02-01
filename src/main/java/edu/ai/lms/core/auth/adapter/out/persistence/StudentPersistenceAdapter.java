package edu.ai.lms.core.auth.adapter.out.persistence;

import edu.ai.lms.core.auth.adapter.out.repository.UserRepository;
import edu.ai.lms.core.auth.domain.Student;
import edu.ai.lms.core.auth.app.port.out.SaveStudentPort;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
class StudentPersistenceAdapter implements
        SaveStudentPort
{
    @Autowired
    private final UserRepository userRepository;
    private final UserJpaMapper userJpaMapper;

    public boolean save(Student student) {
        userRepository.save(userJpaMapper.mapToStudentJpaEntity(student));
        return true;
    }
}
