package edu.ai.lms.core.auth.app.service;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.core.auth.adapter.in.http.response.StudentRegisterResponse;
import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.Student;
import edu.ai.lms.core.auth.app.port.in.command.StudentRegisterCommand;
import edu.ai.lms.core.auth.app.port.out.LoadStudentPort;
import edu.ai.lms.core.auth.app.port.out.SaveStudentPort;
import edu.ai.lms.core.auth.app.port.out.TokenGenerationPort;
import edu.ai.lms.core.auth.app.exception.EmailAlreadyExistsException;

@ExtendWith(MockitoExtension.class)
class StudentRegisterServiceUT {

    @Mock
    private SaveStudentPort saveStudentPort;

    @Mock
    private LoadStudentPort loadStudentPort;

    @Mock
    private TokenGenerationPort tokenGenerationPort;

    @InjectMocks
    private StudentRegisterService studentRegisterService;

    private StudentRegisterCommand validCommand;

    @BeforeEach
    void setUp() {
        Student student = Student.builder()
                .email("student@example.com")
                .password("Password123!")
                .firstName("John")
                .lastName("Doe")
                .studentId("STU001")
                .enrollmentYear(2024L)
                .build();

        validCommand = StudentRegisterCommand.builder()
                .student(student)
                .build();
    }

    @Test
    @Order(1)
    @DisplayName("Should successfully register a new student with valid details")
    void testStudentRegister_WithValidDetails_ShouldSucceed() {
        // Arrange
        Student student = validCommand.student();
        when(loadStudentPort.findByEmail(student.getEmail())).thenReturn(Optional.empty());
        when(tokenGenerationPort.generateStudentToken(any(Student.class))).thenReturn("mock-jwt-token");

        // Act
        StudentRegisterResponse response = studentRegisterService.studentRegister(validCommand);

        // Assert
        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("student@example.com", response.getEmail());
        
        verify(loadStudentPort, times(1)).findByEmail("student@example.com");
        verify(saveStudentPort, times(1)).save(any(Student.class));
        verify(tokenGenerationPort, times(1)).generateStudentToken(any(Student.class));
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when email is already registered")
    void testStudentRegister_WithExistingEmail_ShouldThrowException() {
        // Arrange
        Student student = validCommand.student();
        Student existingStudent = Student.builder()
                .id(UUID.randomUUID())
                .email("student@example.com")
                .build();
        
        when(loadStudentPort.findByEmail(student.getEmail())).thenReturn(Optional.of(existingStudent));

        // Act & Assert
        EmailAlreadyExistsException exception = assertThrows(EmailAlreadyExistsException.class,
                () -> studentRegisterService.studentRegister(validCommand));
        
        assertEquals("Email already registered: student@example.com", exception.getMessage());
        
        verify(loadStudentPort, times(1)).findByEmail("student@example.com");
        verify(saveStudentPort, never()).save(any(Student.class));
        verify(tokenGenerationPort, never()).generateStudentToken(any(Student.class));
    }

    @Test
    @DisplayName("Should save student with correct role and all provided details")
    void testStudentRegister_ShouldSaveStudentWithCorrectDetails() {
        // Arrange
        Student student = validCommand.student();
        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        when(loadStudentPort.findByEmail(student.getEmail())).thenReturn(Optional.empty());
        when(tokenGenerationPort.generateStudentToken(any(Student.class))).thenReturn("mock-jwt-token");

        // Act
        studentRegisterService.studentRegister(validCommand);

        // Assert
        verify(saveStudentPort, times(1)).save(studentCaptor.capture());
        Student savedStudent = studentCaptor.getValue();
        
        assertEquals("student@example.com", savedStudent.getEmail());
        assertEquals("Password123!", savedStudent.getPassword());
        assertEquals("John", savedStudent.getFirstName());
        assertEquals("Doe", savedStudent.getLastName());
        assertEquals("STU001", savedStudent.getStudentId());
        assertEquals(2024L, savedStudent.getEnrollmentYear());
        assertEquals(Roles.STUDENT, savedStudent.getRole());
    }

    @Test
    @DisplayName("Should generate token with the saved student object")
    void testStudentRegister_ShouldGenerateTokenWithStudent() {
        // Arrange
        Student student = validCommand.student();
        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        when(loadStudentPort.findByEmail(student.getEmail())).thenReturn(Optional.empty());
        when(tokenGenerationPort.generateStudentToken(any(Student.class))).thenReturn("generated-token-123");

        // Act
        StudentRegisterResponse response = studentRegisterService.studentRegister(validCommand);

        // Assert
        verify(tokenGenerationPort, times(1)).generateStudentToken(studentCaptor.capture());
        Student studentForToken = studentCaptor.getValue();
        
        assertEquals("student@example.com", studentForToken.getEmail());
        assertEquals("generated-token-123", response.getToken());
    }

    @Test
    @DisplayName("Should handle registration with minimal required fields")
    void testStudentRegister_WithMinimalFields_ShouldSucceed() {
        // Arrange
        Student minimalStudent = Student.builder()
                .email("minimal@example.com")
                .password("Pass123!")
                .firstName("Jane")
                .lastName("Smith")
                .studentId("STU002")
                .enrollmentYear(2025L)
                .build();
        StudentRegisterCommand minimalCommand = StudentRegisterCommand.builder()
                .student(minimalStudent)
                .build();

        when(loadStudentPort.findByEmail(minimalStudent.getEmail())).thenReturn(Optional.empty());
        when(tokenGenerationPort.generateStudentToken(any(Student.class))).thenReturn("minimal-token");

        // Act
        StudentRegisterResponse response = studentRegisterService.studentRegister(minimalCommand);

        // Assert
        assertNotNull(response);
        assertEquals("minimal-token", response.getToken());
        assertEquals("minimal@example.com", response.getEmail());
        
        verify(saveStudentPort, times(1)).save(any(Student.class));
    }
}