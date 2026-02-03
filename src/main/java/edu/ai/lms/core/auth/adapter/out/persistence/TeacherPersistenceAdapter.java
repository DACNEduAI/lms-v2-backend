package edu.ai.lms.core.auth.adapter.out.persistence;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.ai.lms.core.auth.app.domain.Teacher;
import edu.ai.lms.core.auth.app.port.out.LoadTeacherPort;
import edu.ai.lms.core.auth.app.port.out.SaveTeacherPort;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
class TeacherPersistenceAdapter implements
        SaveTeacherPort,
        LoadTeacherPort
{
    @Autowired
    private final UserRepository userRepository;
    private final UserJpaMapper userJpaMapper;

    @Override
    public boolean save(Teacher teacher) {
        userRepository.save(userJpaMapper.mapToTeacherJpaEntity(teacher));
        return true;
    }

    @Override
    public Optional<Teacher> findByEmail(String email) {
        return userRepository.findTeacherByEmail(email)
                .map(userJpaMapper::mapToTeacher);
    }
}
