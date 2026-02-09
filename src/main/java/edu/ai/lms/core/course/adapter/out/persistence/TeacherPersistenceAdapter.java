package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.Teacher;
import edu.ai.lms.core.course.app.port.out.LoadTeacherPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class TeacherPersistenceAdapter implements LoadTeacherPort {

    private final TeacherRepository teacherRepository;
    private final TeacherJdbcMapper teacherJdbcMapper;

    @Override
    public Optional<Teacher> findById(UUID teacherId) {
        return teacherRepository.findById(teacherId)
                .map(teacherJdbcMapper::mapToTeacher);
    }
}
