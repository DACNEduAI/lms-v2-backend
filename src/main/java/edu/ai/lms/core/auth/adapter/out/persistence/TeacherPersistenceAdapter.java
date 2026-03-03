package edu.ai.lms.core.auth.adapter.out.persistence;

import java.util.Optional;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.adapter.out.persistence.mapper.UserJdbcMapper;
import edu.ai.lms.database.UserJdbcTable;
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
    private final UserJdbcMapper userJdbcMapper;

    @Override
    public boolean save(Teacher teacher) {
        UserJdbcTable userEntity = userJdbcMapper.mapToUserJdbcEntity(teacher);
        UserJdbcTable savedUser = userRepository.save(userEntity);
        teacherRepository.insertTeacher(
                savedUser.getId(),
                teacher.getEmployeeId(),
                teacher.getPhoneNumber(),
                teacher.getDepartment(),
                teacher.getSpecialization()
        );
        return true;
    }

    @Override
    public Optional<Teacher> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getRole() == Roles.TEACHER)
                .flatMap(user -> teacherRepository.findById(user.getId())
                        .map(teacher -> userJdbcMapper.mapToTeacher(user, teacher)));
    }
}
