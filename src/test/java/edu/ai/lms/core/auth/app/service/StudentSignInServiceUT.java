package edu.ai.lms.core.auth.app.service;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.core.auth.adapter.in.request.StudentSignInRequest;
import edu.ai.lms.core.auth.adapter.in.response.StudentSignInResponse;
import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.Student;
import edu.ai.lms.core.auth.app.port.out.AuthenticationPort;
import edu.ai.lms.core.auth.app.port.out.TokenGenerationPort;
import edu.ai.lms.core.auth.exception.InvalidCredentialsException;

@ExtendWith(MockitoExtension.class)
class StudentSignInServiceUT {

    @Mock
    private AuthenticationPort authenticationPort;

    @Mock
    private TokenGenerationPort tokenGenerationPort;

    @InjectMocks
    private StudentSignInService studentSignInService;

    private StudentSignInRequest validRequest;
    private Student validStudent;

    @BeforeEach
    void setUp() {
        validRequest = StudentSignInRequest.builder()
                .email("student@example.com")
                .password("Password123!")
                .build();

        validStudent = Student.builder()
                .id(UUID.randomUUID())
                .email("student@example.com")
                .password("encodedPassword123!")
                .firstName("John")
                .lastName("Doe")
                .enrollmentYear(2024L)
                .role(Roles.STUDENT)
                .build();
    }

    @Test
    @DisplayName("Should successfully sign in with valid credentials")
    void testSignIn_WithValidCredentials_ShouldSucceed() {
        // Arrange
        when(authenticationPort.authenticateStudent(validRequest.getEmail(), validRequest.getPassword()))
                .thenReturn(validStudent);
        when(tokenGenerationPort.generateStudentToken(validStudent)).thenReturn("mock-jwt-token");

        // Act
        StudentSignInResponse response = studentSignInService.signIn(validRequest);

        // Assert
        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("student@example.com", response.getEmail());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals("STUDENT", response.getRole());
        
        verify(authenticationPort, times(1)).authenticateStudent("student@example.com", "Password123!");
        verify(tokenGenerationPort, times(1)).generateStudentToken(validStudent);
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when authentication fails")
    void testSignIn_WithInvalidCredentials_ShouldThrowException() {
        // Arrange
        when(authenticationPort.authenticateStudent(validRequest.getEmail(), validRequest.getPassword()))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> studentSignInService.signIn(validRequest));
        
        assertEquals("Invalid email or password", exception.getMessage());
        
        verify(authenticationPort, times(1)).authenticateStudent("student@example.com", "Password123!");
        verify(tokenGenerationPort, never()).generateStudentToken(any(Student.class));
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when email does not exist")
    void testSignIn_WithNonExistentEmail_ShouldThrowException() {
        // Arrange
        StudentSignInRequest invalidRequest = StudentSignInRequest.builder()
                .email("nonexistent@example.com")
                .password("Password123!")
                .build();

        when(authenticationPort.authenticateStudent(invalidRequest.getEmail(), invalidRequest.getPassword()))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> studentSignInService.signIn(invalidRequest));
        
        assertEquals("Invalid email or password", exception.getMessage());
        verify(tokenGenerationPort, never()).generateStudentToken(any(Student.class));
    }

    @Test
    @DisplayName("Should generate token with authenticated student details")
    void testSignIn_ShouldGenerateTokenWithCorrectStudent() {
        // Arrange
        when(authenticationPort.authenticateStudent(validRequest.getEmail(), validRequest.getPassword()))
                .thenReturn(validStudent);
        when(tokenGenerationPort.generateStudentToken(validStudent)).thenReturn("specific-token-for-student");

        // Act
        StudentSignInResponse response = studentSignInService.signIn(validRequest);

        // Assert
        assertEquals("specific-token-for-student", response.getToken());
        verify(tokenGenerationPort, times(1)).generateStudentToken(argThat(student ->
                student.getEmail().equals("student@example.com") &&
                student.getFirstName().equals("John") &&
                student.getLastName().equals("Doe") &&
                student.getRoleAsString().equals("STUDENT")
        ));
    }

    @Test
    @DisplayName("Should return complete student information in response")
    void testSignIn_ShouldReturnCompleteStudentInfo() {
        // Arrange
        Student detailedStudent = Student.builder()
                .id(UUID.randomUUID())
                .email("detailed@example.com")
                .password("encodedPassword")
                .firstName("Alice")
                .lastName("Johnson")
                .enrollmentYear(2023L)
                .avatarUrl("https://example.com/avatar.jpg")
                .role(Roles.STUDENT)
                .build();

        StudentSignInRequest detailedRequest = StudentSignInRequest.builder()
                .email("detailed@example.com")
                .password("Password123!")
                .build();

        when(authenticationPort.authenticateStudent(detailedRequest.getEmail(), detailedRequest.getPassword()))
                .thenReturn(detailedStudent);
        when(tokenGenerationPort.generateStudentToken(detailedStudent)).thenReturn("detailed-token");

        // Act
        StudentSignInResponse response = studentSignInService.signIn(detailedRequest);

        // Assert
        assertNotNull(response);
        assertEquals("detailed-token", response.getToken());
        assertEquals("detailed@example.com", response.getEmail());
        assertEquals("Alice", response.getFirstName());
        assertEquals("Johnson", response.getLastName());
        assertEquals("STUDENT", response.getRole());
    }
}