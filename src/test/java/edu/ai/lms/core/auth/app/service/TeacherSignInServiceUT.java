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

import edu.ai.lms.core.auth.adapter.in.request.TeacherSignInRequest;
import edu.ai.lms.core.auth.adapter.in.response.TeacherSignInResponse;
import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.Teacher;
import edu.ai.lms.core.auth.app.port.out.AuthenticationPort;
import edu.ai.lms.core.auth.app.port.out.TokenGenerationPort;
import edu.ai.lms.core.auth.app.exception.InvalidCredentialsException;

@ExtendWith(MockitoExtension.class)
class TeacherSignInServiceUT {

    @Mock
    private AuthenticationPort authenticationPort;

    @Mock
    private TokenGenerationPort tokenGenerationPort;

    @InjectMocks
    private TeacherSignInService teacherSignInService;

    private TeacherSignInRequest validRequest;
    private Teacher validTeacher;

    @BeforeEach
    void setUp() {
        validRequest = TeacherSignInRequest.builder()
                .email("teacher@example.com")
                .password("Password123!")
                .build();

        validTeacher = Teacher.builder()
                .id(UUID.randomUUID())
                .email("teacher@example.com")
                .password("encodedPassword123!")
                .firstName("Alice")
                .lastName("Smith")
                .employeeId("EMP001")
                .phoneNumber("123-456-7890")
                .department("Computer Science")
                .specialization("Artificial Intelligence")
                .role(Roles.TEACHER)
                .build();
    }

    @Test
    @DisplayName("Should successfully sign in with valid credentials")
    void testSignIn_WithValidCredentials_ShouldSucceed() {
        // Arrange
        when(authenticationPort.authenticateTeacher(validRequest.getEmail(), validRequest.getPassword()))
                .thenReturn(validTeacher);
        when(tokenGenerationPort.generateTeacherToken(validTeacher)).thenReturn("mock-jwt-token");

        // Act
        TeacherSignInResponse response = teacherSignInService.signIn(validRequest);

        // Assert
        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("teacher@example.com", response.getEmail());
        assertEquals("Alice", response.getFirstName());
        assertEquals("Smith", response.getLastName());
        assertEquals("TEACHER", response.getRole());
        
        verify(authenticationPort, times(1)).authenticateTeacher("teacher@example.com", "Password123!");
        verify(tokenGenerationPort, times(1)).generateTeacherToken(validTeacher);
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when authentication fails")
    void testSignIn_WithInvalidCredentials_ShouldThrowException() {
        // Arrange
        when(authenticationPort.authenticateTeacher(validRequest.getEmail(), validRequest.getPassword()))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> teacherSignInService.signIn(validRequest));
        
        assertEquals("Invalid email or password", exception.getMessage());
        
        verify(authenticationPort, times(1)).authenticateTeacher("teacher@example.com", "Password123!");
        verify(tokenGenerationPort, never()).generateTeacherToken(any(Teacher.class));
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when email does not exist")
    void testSignIn_WithNonExistentEmail_ShouldThrowException() {
        // Arrange
        TeacherSignInRequest invalidRequest = TeacherSignInRequest.builder()
                .email("nonexistent@example.com")
                .password("Password123!")
                .build();

        when(authenticationPort.authenticateTeacher(invalidRequest.getEmail(), invalidRequest.getPassword()))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> teacherSignInService.signIn(invalidRequest));
        
        assertEquals("Invalid email or password", exception.getMessage());
        verify(tokenGenerationPort, never()).generateTeacherToken(any(Teacher.class));
    }

    @Test
    @DisplayName("Should generate token with authenticated teacher details")
    void testSignIn_ShouldGenerateTokenWithCorrectTeacher() {
        // Arrange
        when(authenticationPort.authenticateTeacher(validRequest.getEmail(), validRequest.getPassword()))
                .thenReturn(validTeacher);
        when(tokenGenerationPort.generateTeacherToken(validTeacher)).thenReturn("specific-token-for-teacher");

        // Act
        TeacherSignInResponse response = teacherSignInService.signIn(validRequest);

        // Assert
        assertEquals("specific-token-for-teacher", response.getToken());
        verify(tokenGenerationPort, times(1)).generateTeacherToken(argThat(teacher ->
                teacher.getEmail().equals("teacher@example.com") &&
                teacher.getFirstName().equals("Alice") &&
                teacher.getLastName().equals("Smith") &&
                teacher.getRoleAsString().equals("TEACHER")
        ));
    }

    @Test
    @DisplayName("Should return complete teacher information in response")
    void testSignIn_ShouldReturnCompleteTeacherInfo() {
        // Arrange
        Teacher detailedTeacher = Teacher.builder()
                .id(UUID.randomUUID())
                .email("detailed@university.edu")
                .password("encodedPassword")
                .firstName("Dr. Robert")
                .lastName("Johnson")
                .employeeId("PROF-2023-042")
                .phoneNumber("+1-555-9876")
                .department("Mathematics")
                .specialization("Applied Statistics")
                .avatarUrl("https://example.com/avatar.jpg")
                .role(Roles.TEACHER)
                .build();

        TeacherSignInRequest detailedRequest = TeacherSignInRequest.builder()
                .email("detailed@university.edu")
                .password("Password123!")
                .build();

        when(authenticationPort.authenticateTeacher(detailedRequest.getEmail(), detailedRequest.getPassword()))
                .thenReturn(detailedTeacher);
        when(tokenGenerationPort.generateTeacherToken(detailedTeacher)).thenReturn("detailed-token");

        // Act
        TeacherSignInResponse response = teacherSignInService.signIn(detailedRequest);

        // Assert
        assertNotNull(response);
        assertEquals("detailed-token", response.getToken());
        assertEquals("detailed@university.edu", response.getEmail());
        assertEquals("Dr. Robert", response.getFirstName());
        assertEquals("Johnson", response.getLastName());
        assertEquals("TEACHER", response.getRole());
    }

    @Test
    @DisplayName("Should handle sign in with wrong password")
    void testSignIn_WithWrongPassword_ShouldThrowException() {
        // Arrange
        TeacherSignInRequest wrongPasswordRequest = TeacherSignInRequest.builder()
                .email("teacher@example.com")
                .password("WrongPassword!")
                .build();

        when(authenticationPort.authenticateTeacher(wrongPasswordRequest.getEmail(), wrongPasswordRequest.getPassword()))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> teacherSignInService.signIn(wrongPasswordRequest));
        
        assertEquals("Invalid email or password", exception.getMessage());
        verify(tokenGenerationPort, never()).generateTeacherToken(any(Teacher.class));
    }

    @Test
    @DisplayName("Should verify authentication is called with correct parameters")
    void testSignIn_ShouldCallAuthenticationWithCorrectParameters() {
        // Arrange
        when(authenticationPort.authenticateTeacher("teacher@example.com", "Password123!"))
                .thenReturn(validTeacher);
        when(tokenGenerationPort.generateTeacherToken(validTeacher)).thenReturn("test-token");

        // Act
        teacherSignInService.signIn(validRequest);

        // Assert
        verify(authenticationPort, times(1)).authenticateTeacher("teacher@example.com", "Password123!");
    }

    @Test
    @DisplayName("Should return Bearer token type by default")
    void testSignIn_ShouldReturnBearerTokenType() {
        // Arrange
        when(authenticationPort.authenticateTeacher(validRequest.getEmail(), validRequest.getPassword()))
                .thenReturn(validTeacher);
        when(tokenGenerationPort.generateTeacherToken(validTeacher)).thenReturn("bearer-token");

        // Act
        TeacherSignInResponse response = teacherSignInService.signIn(validRequest);

        // Assert
        assertEquals("Bearer", response.getType());
    }
}
