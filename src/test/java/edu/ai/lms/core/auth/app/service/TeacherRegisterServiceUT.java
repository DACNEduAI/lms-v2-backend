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

import edu.ai.lms.core.auth.adapter.in.http.response.TeacherRegisterResponse;
import edu.ai.lms.core.auth.app.port.in.command.TeacherRegisterCommand;
import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.Teacher;
import edu.ai.lms.core.auth.app.port.out.LoadTeacherPort;
import edu.ai.lms.core.auth.app.port.out.SaveTeacherPort;
import edu.ai.lms.core.auth.app.port.out.TokenGenerationPort;
import edu.ai.lms.core.auth.app.exception.EmailAlreadyExistsException;

@ExtendWith(MockitoExtension.class)
class TeacherRegisterServiceUT {

    @Mock
    private SaveTeacherPort saveTeacherPort;

    @Mock
    private LoadTeacherPort loadTeacherPort;

    @Mock
    private TokenGenerationPort tokenGenerationPort;

    @InjectMocks
    private TeacherRegisterService teacherRegisterService;

    private TeacherRegisterCommand validCommand;

    @BeforeEach
    void setUp() {
        validCommand = TeacherRegisterCommand.builder()
                .teacher(Teacher.builder()
                        .email("teacher@example.com")
                        .password("Password123!")
                        .firstName("Alice")
                        .lastName("Smith")
                        .phoneNumber("123-456-7890")
                        .department("Computer Science")
                        .specialization("Artificial Intelligence")
                        .build())
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new teacher with valid details")
    void testTeacherRegister_WithValidDetails_ShouldSucceed() {
        // Arrange
        when(loadTeacherPort.findByEmail(validCommand.teacher().getEmail())).thenReturn(Optional.empty());
        when(tokenGenerationPort.generateTeacherToken(any(Teacher.class))).thenReturn("mock-jwt-token");

        // Act
        TeacherRegisterResponse response = teacherRegisterService.teacherRegister(validCommand);

        // Assert
        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("teacher@example.com", response.getEmail());
        
        verify(loadTeacherPort, times(1)).findByEmail("teacher@example.com");
        verify(saveTeacherPort, times(1)).save(any(Teacher.class));
        verify(tokenGenerationPort, times(1)).generateTeacherToken(any(Teacher.class));
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when email is already registered")
    void testTeacherRegister_WithExistingEmail_ShouldThrowException() {
        // Arrange
        Teacher existingTeacher = Teacher.builder()
                .id(UUID.randomUUID())
                .email("teacher@example.com")
                .build();
        
        when(loadTeacherPort.findByEmail(validCommand.teacher().getEmail())).thenReturn(Optional.of(existingTeacher));

        // Act & Assert
        EmailAlreadyExistsException exception = assertThrows(EmailAlreadyExistsException.class,
                () -> teacherRegisterService.teacherRegister(validCommand));
        
        assertEquals("Email already registered: teacher@example.com", exception.getMessage());
        
        verify(loadTeacherPort, times(1)).findByEmail("teacher@example.com");
        verify(saveTeacherPort, never()).save(any(Teacher.class));
        verify(tokenGenerationPort, never()).generateTeacherToken(any(Teacher.class));
    }

    @Test
    @DisplayName("Should save teacher with correct role and all provided details")
    void testTeacherRegister_ShouldSaveTeacherWithCorrectDetails() {
        // Arrange
        ArgumentCaptor<Teacher> teacherCaptor = ArgumentCaptor.forClass(Teacher.class);
        when(loadTeacherPort.findByEmail(validCommand.teacher().getEmail())).thenReturn(Optional.empty());
        when(tokenGenerationPort.generateTeacherToken(any(Teacher.class))).thenReturn("mock-jwt-token");

        // Act
        teacherRegisterService.teacherRegister(validCommand);

        // Assert
        verify(saveTeacherPort, times(1)).save(teacherCaptor.capture());
        Teacher savedTeacher = teacherCaptor.getValue();
        
        assertEquals("teacher@example.com", savedTeacher.getEmail());
        assertEquals("Password123!", savedTeacher.getPassword());
        assertEquals("Alice", savedTeacher.getFirstName());
        assertEquals("Smith", savedTeacher.getLastName());
        assertEquals("123-456-7890", savedTeacher.getPhoneNumber());
        assertEquals("Computer Science", savedTeacher.getDepartment());
        assertEquals("Artificial Intelligence", savedTeacher.getSpecialization());
        assertEquals(Roles.TEACHER, savedTeacher.getRole());
    }

    @Test
    @DisplayName("Should generate token with the saved teacher object")
    void testTeacherRegister_ShouldGenerateTokenWithTeacher() {
        // Arrange
        ArgumentCaptor<Teacher> teacherCaptor = ArgumentCaptor.forClass(Teacher.class);
        when(loadTeacherPort.findByEmail(validCommand.teacher().getEmail())).thenReturn(Optional.empty());
        when(tokenGenerationPort.generateTeacherToken(any(Teacher.class))).thenReturn("generated-token-123");

        // Act
        TeacherRegisterResponse response = teacherRegisterService.teacherRegister(validCommand);

        // Assert
        verify(tokenGenerationPort, times(1)).generateTeacherToken(teacherCaptor.capture());
        Teacher teacherForToken = teacherCaptor.getValue();
        
        assertEquals("teacher@example.com", teacherForToken.getEmail());
        assertEquals("generated-token-123", response.getToken());
    }

    @Test
    @DisplayName("Should handle registration with minimal required fields")
    void testTeacherRegister_WithMinimalFields_ShouldSucceed() {
        // Arrange
        TeacherRegisterCommand minimalCommand = TeacherRegisterCommand.builder()
                .teacher(Teacher.builder()
                        .email("minimal@example.com")
                        .password("Pass123!")
                        .firstName("Bob")
                        .lastName("Johnson")
                        .build())
                .build();

        when(loadTeacherPort.findByEmail(minimalCommand.teacher().getEmail())).thenReturn(Optional.empty());
        when(tokenGenerationPort.generateTeacherToken(any(Teacher.class))).thenReturn("minimal-token");

        // Act
        TeacherRegisterResponse response = teacherRegisterService.teacherRegister(minimalCommand);

        // Assert
        assertNotNull(response);
        assertEquals("minimal-token", response.getToken());
        assertEquals("minimal@example.com", response.getEmail());
        
        verify(saveTeacherPort, times(1)).save(any(Teacher.class));
    }

    @Test
    @DisplayName("Should handle registration with optional fields set to null")
    void testTeacherRegister_WithNullOptionalFields_ShouldSucceed() {
        // Arrange
        TeacherRegisterCommand commandWithNulls = TeacherRegisterCommand.builder()
                .teacher(Teacher.builder()
                        .email("nullfields@example.com")
                        .password("Password123!")
                        .firstName("Charlie")
                        .lastName("Brown")
                        .phoneNumber(null)
                        .department(null)
                        .specialization(null)
                        .build())
                .build();

        ArgumentCaptor<Teacher> teacherCaptor = ArgumentCaptor.forClass(Teacher.class);
        when(loadTeacherPort.findByEmail(commandWithNulls.teacher().getEmail())).thenReturn(Optional.empty());
        when(tokenGenerationPort.generateTeacherToken(any(Teacher.class))).thenReturn("token-with-nulls");

        // Act
        TeacherRegisterResponse response = teacherRegisterService.teacherRegister(commandWithNulls);

        // Assert
        assertNotNull(response);
        verify(saveTeacherPort, times(1)).save(teacherCaptor.capture());
        
        Teacher savedTeacher = teacherCaptor.getValue();
        assertEquals("nullfields@example.com", savedTeacher.getEmail());
        assertEquals(Roles.TEACHER, savedTeacher.getRole());
    }

    @Test
    @DisplayName("Should preserve all teacher details through registration flow")
    void testTeacherRegister_ShouldPreserveAllDetails() {
        // Arrange
        TeacherRegisterCommand detailedCommand = TeacherRegisterCommand.builder()
                .teacher(Teacher.builder()
                        .email("detailed@university.edu")
                        .password("SecurePass456!")
                        .firstName("Dr. Emily")
                        .lastName("Martinez")
                        .phoneNumber("+1-555-0123")
                        .department("Data Science")
                        .specialization("Machine Learning & Deep Learning")
                        .build())
                .build();

        ArgumentCaptor<Teacher> teacherCaptor = ArgumentCaptor.forClass(Teacher.class);
        when(loadTeacherPort.findByEmail(detailedCommand.teacher().getEmail())).thenReturn(Optional.empty());
        when(tokenGenerationPort.generateTeacherToken(any(Teacher.class))).thenReturn("detailed-token");

        // Act
        TeacherRegisterResponse response = teacherRegisterService.teacherRegister(detailedCommand);

        // Assert
        verify(saveTeacherPort, times(1)).save(teacherCaptor.capture());
        Teacher savedTeacher = teacherCaptor.getValue();
        
        assertEquals("detailed@university.edu", savedTeacher.getEmail());
        assertEquals("SecurePass456!", savedTeacher.getPassword());
        assertEquals("Dr. Emily", savedTeacher.getFirstName());
        assertEquals("Martinez", savedTeacher.getLastName());
        assertEquals("+1-555-0123", savedTeacher.getPhoneNumber());
        assertEquals("Data Science", savedTeacher.getDepartment());
        assertEquals("Machine Learning & Deep Learning", savedTeacher.getSpecialization());
        assertEquals("detailed-token", response.getToken());
    }
}
