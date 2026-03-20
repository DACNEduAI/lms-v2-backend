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
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    @Override
    public boolean save(Student student) {
        student.setId(userRepository.save(student).getId());
        studentRepository.insertStudent(student);
        return true;
    }

    @Override
    public Optional<Student> findByEmail(String email) {
        return studentRepository.findByEmail(email);
    }
}
