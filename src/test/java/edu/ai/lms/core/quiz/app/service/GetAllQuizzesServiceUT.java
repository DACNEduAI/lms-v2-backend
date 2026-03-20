package edu.ai.lms.core.quiz.app.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.quiz.adapter.in.http.response.GetAllQuizzesResponse;
import edu.ai.lms.core.quiz.app.domain.Quiz;
import edu.ai.lms.core.quiz.app.exception.UserNotAuthenticatedException;
import edu.ai.lms.core.quiz.app.port.out.LoadCourseMembershipPort;
import edu.ai.lms.core.quiz.app.port.out.LoadQuizPort;
import edu.ai.lms.core.quiz.app.port.out.LoadSignedInUserPort;

@ExtendWith(MockitoExtension.class)
class GetAllQuizzesServiceUT {

    @Mock
    private LoadQuizPort loadQuizPort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @Mock
    private LoadCourseMembershipPort loadCourseMembershipPort;

    @InjectMocks
    private GetAllQuizzesService getAllQuizzesService;

    private UserAuth mockTeacher;
    private UserAuth mockStudent;
    private UUID teacherId;
    private UUID studentId;
    private UUID courseId;
    private List<Quiz> quizzes;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        mockTeacher = UserAuth.builder()
                .id(teacherId)
                .role(Roles.TEACHER)
                .build();

        mockStudent = UserAuth.builder()
                .id(studentId)
                .role(Roles.STUDENT)
                .build();

        quizzes = new ArrayList<>();
        quizzes.add(Quiz.builder()
                .id(UUID.randomUUID())
                .quizTitle("Alpha Quiz")
                .timeLimitMinutes(30)
                .passScore(60.0)
                .courseId(courseId)
                .teacherId(teacherId)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());
        quizzes.add(Quiz.builder()
                .id(UUID.randomUUID())
                .quizTitle("Beta Quiz")
                .timeLimitMinutes(45)
                .passScore(70.0)
                .courseId(courseId)
                .teacherId(teacherId)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());
        quizzes.add(Quiz.builder()
                .id(UUID.randomUUID())
                .quizTitle("Charlie Quiz")
                .timeLimitMinutes(60)
                .passScore(80.0)
                .courseId(courseId)
                .teacherId(teacherId)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("Should return all quizzes for a student in ascending order")
    void testGetAllQuizzes_AsStudent_ShouldReturnAllQuizzes() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockStudent));
        when(loadCourseMembershipPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizPort.getAllByCourseId(courseId)).thenReturn(quizzes);

        // Act
        GetAllQuizzesResponse response = getAllQuizzesService.getAllQuizzes(courseId, 10, "asc");

        // Assert
        assertNotNull(response);
        assertEquals(3, response.getTotalQuizzes());
        assertEquals(3, response.getQuizzes().size());
        assertEquals("asc", response.getSortDirection());

        // Verify ascending order
        assertEquals("Alpha Quiz", response.getQuizzes().get(0).getQuizTitle());
        assertEquals("Beta Quiz", response.getQuizzes().get(1).getQuizTitle());
        assertEquals("Charlie Quiz", response.getQuizzes().get(2).getQuizTitle());

        verify(loadQuizPort, times(1)).getAllByCourseId(courseId);
    }

    @Test
    @DisplayName("Should return only teacher's own quizzes for a teacher")
    void testGetAllQuizzes_AsTeacher_ShouldReturnOnlyOwnQuizzes() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(true);
        when(loadQuizPort.getByCourseIdAndTeacherId(courseId, teacherId)).thenReturn(quizzes);

        // Act
        GetAllQuizzesResponse response = getAllQuizzesService.getAllQuizzes(courseId, 10, "asc");

        // Assert
        assertNotNull(response);
        assertEquals(3, response.getTotalQuizzes());

        verify(loadQuizPort, times(1)).getByCourseIdAndTeacherId(courseId, teacherId);
        verify(loadQuizPort, never()).getAllByCourseId(courseId);
    }

    @Test
    @DisplayName("Should return quizzes in descending order when sortDirection is desc")
    void testGetAllQuizzes_WithDescSort_ShouldReturnDescending() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockStudent));
        when(loadCourseMembershipPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizPort.getAllByCourseId(courseId)).thenReturn(quizzes);

        // Act
        GetAllQuizzesResponse response = getAllQuizzesService.getAllQuizzes(courseId, 10, "desc");

        // Assert
        assertNotNull(response);
        assertEquals("desc", response.getSortDirection());
        assertEquals("Charlie Quiz", response.getQuizzes().get(0).getQuizTitle());
        assertEquals("Beta Quiz", response.getQuizzes().get(1).getQuizTitle());
        assertEquals("Alpha Quiz", response.getQuizzes().get(2).getQuizTitle());
    }

    @Test
    @DisplayName("Should limit results by pageSize")
    void testGetAllQuizzes_WithPageSize_ShouldLimitResults() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockStudent));
        when(loadCourseMembershipPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizPort.getAllByCourseId(courseId)).thenReturn(quizzes);

        // Act
        GetAllQuizzesResponse response = getAllQuizzesService.getAllQuizzes(courseId, 2, "asc");

        // Assert
        assertNotNull(response);
        assertEquals(3, response.getTotalQuizzes());
        assertEquals(2, response.getQuizzes().size());
        assertEquals(2, response.getPageSize());
    }

    @Test
    @DisplayName("Should return empty list when no quizzes exist")
    void testGetAllQuizzes_WithNoQuizzes_ShouldReturnEmptyList() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockStudent));
        when(loadCourseMembershipPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizPort.getAllByCourseId(courseId)).thenReturn(Collections.emptyList());

        // Act
        GetAllQuizzesResponse response = getAllQuizzesService.getAllQuizzes(courseId, 10, "asc");

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getTotalQuizzes());
        assertTrue(response.getQuizzes().isEmpty());
    }

    @Test
    @DisplayName("Should throw UserNotAuthenticatedException when user is not authenticated")
    void testGetAllQuizzes_WithUnauthenticatedUser_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        UserNotAuthenticatedException exception = assertThrows(UserNotAuthenticatedException.class,
                () -> getAllQuizzesService.getAllQuizzes(courseId, 10, "asc"));

        assertEquals("User not authenticated", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when user is not a member of the course")
    void testGetAllQuizzes_WithNonMember_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockTeacher));
        when(loadCourseMembershipPort.isTeacherInCourse(teacherId, courseId)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> getAllQuizzesService.getAllQuizzes(courseId, 10, "asc"));

        assertEquals("User is not a member of this course", exception.getMessage());
    }

    @Test
    @DisplayName("Should default sortDirection to asc when null")
    void testGetAllQuizzes_WithNullSortDirection_ShouldDefaultToAsc() {
        // Arrange
        when(loadSignedInUserPort.getAuthenticatedUser()).thenReturn(Optional.of(mockStudent));
        when(loadCourseMembershipPort.isStudentInCourse(studentId, courseId)).thenReturn(true);
        when(loadQuizPort.getAllByCourseId(courseId)).thenReturn(quizzes);

        // Act
        GetAllQuizzesResponse response = getAllQuizzesService.getAllQuizzes(courseId, 10, null);

        // Assert
        assertNotNull(response);
        assertEquals("asc", response.getSortDirection());
    }
}
