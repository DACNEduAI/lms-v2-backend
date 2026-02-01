package edu.ai.lms.core.auth.app.service;

import org.springframework.beans.factory.annotation.Autowired;

import edu.ai.lms.core.auth.adapter.in.request.StudentRegisterRequest;
import edu.ai.lms.core.auth.domain.Roles;
import edu.ai.lms.core.auth.domain.Student;
import edu.ai.lms.core.auth.app.port.in.usecase.StudentRegisterUseCase;
import edu.ai.lms.core.auth.app.port.out.SaveStudentPort;
import org.springframework.stereotype.Service;

@Service
public class StudentRegisterService implements StudentRegisterUseCase {

    @Autowired
    private final SaveStudentPort saveStudentPort;

    public StudentRegisterService (SaveStudentPort saveStudent) {
        this.saveStudentPort = saveStudent;
    }

    @Override
    public boolean studentRegister(StudentRegisterRequest request) {

        saveStudentPort.save(Student.builder()
                .email(request.getEmail())
                .role(Roles.STUDENT)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .enrollmentYear(request.getEnrollmentYear())
                .build());



        return true;
    }
}