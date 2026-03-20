package edu.ai.lms.core.auth.adapter.out.persistence;

import java.util.Optional;

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
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;

    @Override
    public boolean save(Teacher teacher) {
        teacher.setId(userRepository.save(teacher).getId());
        teacherRepository.insertTeacher(teacher);
        return true;
    }

    @Override
    public Optional<Teacher> findByEmail(String email) {
        return teacherRepository.findByEmail(email);
    }
}
