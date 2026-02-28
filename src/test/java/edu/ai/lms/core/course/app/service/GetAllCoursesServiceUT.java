package edu.ai.lms.core.course.app.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.ai.lms.common.Roles;
import edu.ai.lms.core.course.adapter.in.response.GetAllCoursesResponse;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort.SignedInUser;

@ExtendWith(MockitoExtension.class)
class GetAllCoursesServiceUT {

    @Mock
    private LoadCoursePort loadCoursePort;

    @Mock
    private LoadSignedInUserPort loadSignedInUserPort;

    @InjectMocks
    private GetAllCoursesService getAllCoursesService;

    private SignedInUser mockTeacherUser;
    private SignedInUser mockStudentUser;
    private UUID teacherId;
    private UUID studentId;
    private List<Course> mockCourses;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        studentId = UUID.randomUUID();

        mockTeacherUser = new SignedInUser(teacherId, Roles.TEACHER);
        mockStudentUser = new SignedInUser(studentId, Roles.STUDENT);

        mockCourses = new ArrayList<>();
        mockCourses.add(Course.builder()
                .id(UUID.randomUUID())
                .courseName("Advanced Java Programming")
                .courseCode("CS301")
                .description("Advanced Java concepts")
                .active(true)
                .coverImage("https://example.com/java-adv.jpg")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .teacherIds(List.of(teacherId))
                .build());

        mockCourses.add(Course.builder()
                .id(UUID.randomUUID())
                .courseName("Basic Python")
                .courseCode("CS101")
                .description("Learn Python basics")
                .active(true)
                .coverImage("https://example.com/python.jpg")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(2))
                .teacherIds(List.of(teacherId))
                .build());

        mockCourses.add(Course.builder()
                .id(UUID.randomUUID())
                .courseName("Cloud Computing")
                .courseCode("CS401")
                .description("Learn cloud technologies")
                .active(true)
                .coverImage("https://example.com/cloud.jpg")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(4))
                .teacherIds(List.of(teacherId))
                .build());
    }

    @Test
    @DisplayName("Should successfully get all courses for authenticated teacher")
    void testGetAllCourses_WithAuthenticatedTeacher_ShouldSucceed() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacherUser));
        when(loadCoursePort.findAllByTeacherId(teacherId)).thenReturn(mockCourses);

        // Act
        GetAllCoursesResponse response = getAllCoursesService.getAllCourses(10, "asc");

        // Assert
        assertNotNull(response);
        assertEquals(3, response.getTotalCourses());
        assertEquals(10, response.getPageSize());
        assertEquals("asc", response.getSortDirection());
        assertEquals(3, response.getCourses().size());
        
        // Verify ascending order by first letter of course name
        assertEquals("Advanced Java Programming", response.getCourses().get(0).getCourseName());
        assertEquals("Basic Python", response.getCourses().get(1).getCourseName());
        assertEquals("Cloud Computing", response.getCourses().get(2).getCourseName());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findAllByTeacherId(teacherId);
    }

    @Test
    @DisplayName("Should successfully get all courses for authenticated student")
    void testGetAllCourses_WithAuthenticatedStudent_ShouldSucceed() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockStudentUser));
        when(loadCoursePort.findAllByStudentId(studentId)).thenReturn(mockCourses);

        // Act
        GetAllCoursesResponse response = getAllCoursesService.getAllCourses(10, "asc");

        // Assert
        assertNotNull(response);
        assertEquals(3, response.getTotalCourses());
        assertEquals(10, response.getPageSize());
        assertEquals("asc", response.getSortDirection());
        assertEquals(3, response.getCourses().size());
        
        // Verify ascending order by first letter of course name
        assertEquals("Advanced Java Programming", response.getCourses().get(0).getCourseName());
        assertEquals("Basic Python", response.getCourses().get(1).getCourseName());
        assertEquals("Cloud Computing", response.getCourses().get(2).getCourseName());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findAllByStudentId(studentId);
    }

    @Test
    @DisplayName("Should return courses in descending order when sortDirection is desc")
    void testGetAllCourses_WithDescendingSortOrder_ShouldReturnDescending() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacherUser));
        when(loadCoursePort.findAllByTeacherId(teacherId)).thenReturn(mockCourses);

        // Act
        GetAllCoursesResponse response = getAllCoursesService.getAllCourses(10, "desc");

        // Assert
        assertNotNull(response);
        assertEquals(3, response.getTotalCourses());
        assertEquals("desc", response.getSortDirection());
        assertEquals(3, response.getCourses().size());
        
        // Verify descending order by first letter of course name
        assertEquals("Cloud Computing", response.getCourses().get(0).getCourseName());
        assertEquals("Basic Python", response.getCourses().get(1).getCourseName());
        assertEquals("Advanced Java Programming", response.getCourses().get(2).getCourseName());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findAllByTeacherId(teacherId);
    }

    @Test
    @DisplayName("Should limit courses based on pageSize parameter")
    void testGetAllCourses_WithLimitedPageSize_ShouldReturnLimitedResults() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacherUser));
        when(loadCoursePort.findAllByTeacherId(teacherId)).thenReturn(mockCourses);

        // Act
        GetAllCoursesResponse response = getAllCoursesService.getAllCourses(2, "asc");

        // Assert
        assertNotNull(response);
        assertEquals(3, response.getTotalCourses());
        assertEquals(2, response.getPageSize());
        assertEquals(2, response.getCourses().size());
        
        // Verify only first 2 courses are returned (sorted ascending)
        assertEquals("Advanced Java Programming", response.getCourses().get(0).getCourseName());
        assertEquals("Basic Python", response.getCourses().get(1).getCourseName());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findAllByTeacherId(teacherId);
    }

    @Test
    @DisplayName("Should throw RuntimeException when user is not authenticated")
    void testGetAllCourses_WithNonAuthenticatedUser_ShouldThrowException() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> getAllCoursesService.getAllCourses(10, "asc"));

        assertEquals("Authenticated user not found", exception.getMessage());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, never()).findAllByTeacherId(any());
        verify(loadCoursePort, never()).findAllByStudentId(any());
    }

    @Test
    @DisplayName("Should return empty list when teacher has no courses")
    void testGetAllCourses_WithNoCourses_ShouldReturnEmptyList() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacherUser));
        when(loadCoursePort.findAllByTeacherId(teacherId)).thenReturn(new ArrayList<>());

        // Act
        GetAllCoursesResponse response = getAllCoursesService.getAllCourses(10, "asc");

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getTotalCourses());
        assertEquals(10, response.getPageSize());
        assertEquals("asc", response.getSortDirection());
        assertEquals(0, response.getCourses().size());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findAllByTeacherId(teacherId);
    }

    @Test
    @DisplayName("Should use default sort direction when sortDirection is null")
    void testGetAllCourses_WithNullSortDirection_ShouldUseDefault() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacherUser));
        when(loadCoursePort.findAllByTeacherId(teacherId)).thenReturn(mockCourses);

        // Act
        GetAllCoursesResponse response = getAllCoursesService.getAllCourses(10, null);

        // Assert
        assertNotNull(response);
        assertEquals("asc", response.getSortDirection());
        
        // Should be in ascending order since null defaults to ascending
        assertEquals("Advanced Java Programming", response.getCourses().get(0).getCourseName());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findAllByTeacherId(teacherId);
    }

    @Test
    @DisplayName("Should return all courses when pageSize is larger than total courses")
    void testGetAllCourses_WithLargePageSize_ShouldReturnAllCourses() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacherUser));
        when(loadCoursePort.findAllByTeacherId(teacherId)).thenReturn(mockCourses);

        // Act
        GetAllCoursesResponse response = getAllCoursesService.getAllCourses(100, "asc");

        // Assert
        assertNotNull(response);
        assertEquals(3, response.getTotalCourses());
        assertEquals(100, response.getPageSize());
        assertEquals(3, response.getCourses().size());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findAllByTeacherId(teacherId);
    }

    @Test
    @DisplayName("Should return courses with correct course summary fields")
    void testGetAllCourses_ShouldReturnCorrectCourseSummaryFields() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacherUser));
        when(loadCoursePort.findAllByTeacherId(teacherId)).thenReturn(mockCourses);

        // Act
        GetAllCoursesResponse response = getAllCoursesService.getAllCourses(10, "asc");

        // Assert
        assertNotNull(response);
        GetAllCoursesResponse.CourseSummary firstCourse = response.getCourses().get(0);
        
        assertNotNull(firstCourse.getCourseId());
        assertNotNull(firstCourse.getCourseName());
        assertNotNull(firstCourse.getCourseCode());
        assertNotNull(firstCourse.getDescription());
        assertNotNull(firstCourse.getActive());
        assertNotNull(firstCourse.getStartDate());
        assertNotNull(firstCourse.getEndDate());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findAllByTeacherId(teacherId);
    }

    @Test
    @DisplayName("Should handle case-insensitive sort direction")
    void testGetAllCourses_WithUpperCaseSortDirection_ShouldWork() {
        // Arrange
        when(loadSignedInUserPort.findAuthenticatedUser()).thenReturn(Optional.of(mockTeacherUser));
        when(loadCoursePort.findAllByTeacherId(teacherId)).thenReturn(mockCourses);

        // Act
        GetAllCoursesResponse response = getAllCoursesService.getAllCourses(10, "DESC");

        // Assert
        assertNotNull(response);
        assertEquals(3, response.getCourses().size());
        
        // Verify descending order
        assertEquals("Cloud Computing", response.getCourses().get(0).getCourseName());
        assertEquals("Basic Python", response.getCourses().get(1).getCourseName());
        assertEquals("Advanced Java Programming", response.getCourses().get(2).getCourseName());

        verify(loadSignedInUserPort, times(1)).findAuthenticatedUser();
        verify(loadCoursePort, times(1)).findAllByTeacherId(teacherId);
    }
}
