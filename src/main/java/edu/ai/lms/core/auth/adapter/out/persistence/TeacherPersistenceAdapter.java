package edu.ai.lms.core.auth.adapter.out.persistence;

import java.util.Optional;

import edu.ai.lms.core.auth.adapter.out.persistence.mapper.UserJdbcMapper;
import org.springframework.stereotype.Component;

import edu.ai.lms.core.auth.app.domain.Teacher;
import edu.ai.lms.core.auth.app.port.out.LoadTeacherPort;
import edu.ai.lms.core.auth.app.port.out.SaveTeacherPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class TeacherPersistenceAdapter implements
        SaveTeacherPort,
        LoadTeacherPort
{
    private final TeacherRepository teacherRepository;
    private final UserJdbcMapper userJdbcMapper;

    @Override
    public boolean save(Teacher teacher) {
        teacherRepository.save(userJdbcMapper.mapToTeacherJdbcEntity(teacher));
        return true;
    }

    @Override
    public Optional<Teacher> findByEmail(String email) {
        return teacherRepository.findByEmail(email)
                .map(userJdbcMapper::mapToTeacher);
    }
}
