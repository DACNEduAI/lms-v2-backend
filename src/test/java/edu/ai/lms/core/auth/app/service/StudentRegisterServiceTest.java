package edu.ai.lms.core.auth.app.service;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import edu.ai.lms.core.auth.adapter.in.request.StudentRegisterRequest;
import edu.ai.lms.core.auth.adapter.in.response.StudentRegisterResponse;
import edu.ai.lms.core.auth.app.domain.Roles;
import edu.ai.lms.core.auth.app.domain.Student;
import edu.ai.lms.core.auth.app.port.out.LoadStudentPort;
import edu.ai.lms.core.auth.app.port.out.SaveStudentPort;
import edu.ai.lms.core.auth.app.port.out.TokenGenerationPort;
import edu.ai.lms.core.auth.exception.EmailAlreadyExistsException;

@ExtendWith(MockitoExtension.class)
class StudentRegisterServiceTest {

    @Mock
    private SaveStudentPort saveStudentPort;

    @Mock
    private LoadStudentPort loadStudentPort;

    @Mock
    private TokenGenerationPort tokenGenerationPort;

    @InjectMocks
    private StudentRegisterService studentRegisterService;

    private StudentRegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = StudentRegisterRequest.builder()
                .email("student@example.com")
                .password("Password123!")
                .firstName("John")
                .lastName("Doe")
                .enrollmentYear(2024L)
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new student with valid details")
    void testStudentRegister_WithValidDetails_ShouldSucceed() {
        // Arrange
        when(loadStudentPort.findByEmail(validRequest.getEmail())).thenReturn(Optional.empty());
        when(tokenGenerationPort.generateStudentToken(any(Student.class))).thenReturn("mock-jwt-token");

        // Act
        StudentRegisterResponse response = studentRegisterService.studentRegister(validRequest);

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
        Student existingStudent = Student.builder()
                .id(UUID.randomUUID())
                .email("student@example.com")
                .build();
        
        when(loadStudentPort.findByEmail(validRequest.getEmail())).thenReturn(Optional.of(existingStudent));

        // Act & Assert
        EmailAlreadyExistsException exception = assertThrows(EmailAlreadyExistsException.class,
                () -> studentRegisterService.studentRegister(validRequest));
        
        assertEquals("Email already registered: student@example.com", exception.getMessage());
        
        verify(loadStudentPort, times(1)).findByEmail("student@example.com");
        verify(saveStudentPort, never()).save(any(Student.class));
        verify(tokenGenerationPort, never()).generateStudentToken(any(Student.class));
    }

    @Test
    @DisplayName("Should save student with correct role and all provided details")
    void testStudentRegister_ShouldSaveStudentWithCorrectDetails() {
        // Arrange
        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        when(loadStudentPort.findByEmail(validRequest.getEmail())).thenReturn(Optional.empty());
        when(tokenGenerationPort.generateStudentToken(any(Student.class))).thenReturn("mock-jwt-token");

        // Act
        studentRegisterService.studentRegister(validRequest);

        // Assert
        verify(saveStudentPort, times(1)).save(studentCaptor.capture());
        Student savedStudent = studentCaptor.getValue();
        
        assertEquals("student@example.com", savedStudent.getEmail());
        assertEquals("Password123!", savedStudent.getPassword());
        assertEquals("John", savedStudent.getFirstName());
        assertEquals("Doe", savedStudent.getLastName());
        assertEquals(2024L, savedStudent.getEnrollmentYear());
        assertEquals(Roles.STUDENT, savedStudent.getRole());
    }

    @Test
    @DisplayName("Should generate token with the saved student object")
    void testStudentRegister_ShouldGenerateTokenWithStudent() {
        // Arrange
        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        when(loadStudentPort.findByEmail(validRequest.getEmail())).thenReturn(Optional.empty());
        when(tokenGenerationPort.generateStudentToken(any(Student.class))).thenReturn("generated-token-123");

        // Act
        StudentRegisterResponse response = studentRegisterService.studentRegister(validRequest);

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
        StudentRegisterRequest minimalRequest = StudentRegisterRequest.builder()
                .email("minimal@example.com")
                .password("Pass123!")
                .firstName("Jane")
                .lastName("Smith")
                .enrollmentYear(2025L)
                .build();

        when(loadStudentPort.findByEmail(minimalRequest.getEmail())).thenReturn(Optional.empty());
        when(tokenGenerationPort.generateStudentToken(any(Student.class))).thenReturn("minimal-token");

        // Act
        StudentRegisterResponse response = studentRegisterService.studentRegister(minimalRequest);

        // Assert
        assertNotNull(response);
        assertEquals("minimal-token", response.getToken());
        assertEquals("minimal@example.com", response.getEmail());
        
        verify(saveStudentPort, times(1)).save(any(Student.class));
    }
}