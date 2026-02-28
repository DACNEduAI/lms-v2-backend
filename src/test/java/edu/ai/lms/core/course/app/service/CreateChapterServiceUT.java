package edu.ai.lms.core.course.app.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import edu.ai.lms.core.course.adapter.in.response.CreateChapterResponse;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.Material;
import edu.ai.lms.core.course.app.domain.Teacher;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.CreateChapterCommand;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInTeacherPort;
import edu.ai.lms.core.course.app.port.out.SaveChapterPort;

@ExtendWith(MockitoExtension.class)
class CreateChapterServiceUT {

    @Mock
    private SaveChapterPort saveChapterPort;

    @Mock
    private LoadCoursePort loadCoursePort;

    @Mock
    private LoadSignedInTeacherPort loadSignedInTeacherPort;

    @InjectMocks
    private CreateChapterService createChapterService;

    private Teacher mockTeacher;
    private UUID teacherId;
    private UUID courseId;
    private Long chapterNumber;
    private Course mockCourse;
    private CreateChapterCommand validCommand;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        chapterNumber = 1L;

        mockTeacher = Teacher.builder()
                .id(teacherId)
                .employeeId("EMP001")
                .department("Computer Science")
                .specialization("Software Engineering")
                .build();

        mockCourse = Course.builder()
                .id(courseId)
                .courseName("Introduction to Java")
                .description("Learn Java programming basics")
                .courseCode("CS101")
                .active(true)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .teacherIds(List.of(teacherId))
                .build();

        Chapter chapter = Chapter.builder()
                .title("Chapter 1: Introduction")
                .description("Introduction to Java programming")
                .materials(new ArrayList<>())
                .build();

        validCommand = CreateChapterCommand.builder()
                .courseId(courseId)
                .chapter(chapter)
                .build();
    }

    @Test
    @DisplayName("Should successfully create a chapter with valid details")
    void testCreateChapter_WithValidDetails_ShouldSucceed() {
        // Arrange
        when(loadSignedInTeacherPort.findAuthenticatedTeacher()).thenReturn(Optional.of(mockTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        Chapter savedChapter = Chapter.builder()
                .chapterNumber(chapterNumber)
                .title("Chapter 1: Introduction")
                .description("Introduction to Java programming")
                .courseId(courseId)
                .materials(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();
        when(saveChapterPort.save(any(Chapter.class))).thenReturn(savedChapter);

        // Act
        CreateChapterResponse response = createChapterService.createChapter(validCommand);

        // Assert
        assertNotNull(response);
        assertEquals(chapterNumber, response.getChapterNumber());
        assertEquals("Chapter 1: Introduction", response.getTitle());
        assertEquals("Introduction to Java programming", response.getDescription());
        assertEquals(courseId, response.getCourseId());
        assertNotNull(response.getCreatedAt());

        verify(loadSignedInTeacherPort, times(1)).findAuthenticatedTeacher();
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(saveChapterPort, times(1)).save(any(Chapter.class));
    }

    @Test
    @DisplayName("Should throw TeacherNotFoundException when teacher is not authenticated")
    void testCreateChapter_WithNonAuthenticatedTeacher_ShouldThrowException() {
        // Arrange
        when(loadSignedInTeacherPort.findAuthenticatedTeacher()).thenReturn(Optional.empty());

        // Act & Assert
        TeacherNotFoundException exception = assertThrows(TeacherNotFoundException.class,
                () -> createChapterService.createChapter(validCommand));

        assertEquals("Teacher not authenticated", exception.getMessage());

        verify(loadSignedInTeacherPort, times(1)).findAuthenticatedTeacher();
        verify(loadCoursePort, never()).findById(any());
        verify(saveChapterPort, never()).save(any(Chapter.class));
    }

    @Test
    @DisplayName("Should throw CourseNotFoundException when course does not exist")
    void testCreateChapter_WithNonExistentCourse_ShouldThrowException() {
        // Arrange
        when(loadSignedInTeacherPort.findAuthenticatedTeacher()).thenReturn(Optional.of(mockTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.empty());

        // Act & Assert
        CourseNotFoundException exception = assertThrows(CourseNotFoundException.class,
                () -> createChapterService.createChapter(validCommand));

        assertTrue(exception.getMessage().contains("Course not found with id:"));

        verify(loadSignedInTeacherPort, times(1)).findAuthenticatedTeacher();
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(saveChapterPort, never()).save(any(Chapter.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when teacher is not authorized for the course")
    void testCreateChapter_WithUnauthorizedTeacher_ShouldThrowException() {
        // Arrange
        Teacher anotherTeacher = Teacher.builder()
                .id(UUID.randomUUID())
                .employeeId("EMP002")
                .department("Physics")
                .specialization("Quantum Mechanics")
                .build();

        when(loadSignedInTeacherPort.findAuthenticatedTeacher()).thenReturn(Optional.of(anotherTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> createChapterService.createChapter(validCommand));

        assertEquals("Teacher is not authorized to add chapters to this course", exception.getMessage());

        verify(loadSignedInTeacherPort, times(1)).findAuthenticatedTeacher();
        verify(loadCoursePort, times(1)).findById(courseId);
        verify(saveChapterPort, never()).save(any(Chapter.class));
    }

    @Test
    @DisplayName("Should set courseId on chapter before saving")
    void testCreateChapter_ShouldSetCourseIdBeforeSaving() {
        // Arrange
        ArgumentCaptor<Chapter> chapterCaptor = ArgumentCaptor.forClass(Chapter.class);
        when(loadSignedInTeacherPort.findAuthenticatedTeacher()).thenReturn(Optional.of(mockTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        Chapter savedChapter = Chapter.builder()
                .chapterNumber(chapterNumber)
                .title("Chapter 1: Introduction")
                .description("Introduction to Java programming")
                .courseId(courseId)
                .materials(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();
        when(saveChapterPort.save(any(Chapter.class))).thenReturn(savedChapter);

        // Act
        createChapterService.createChapter(validCommand);

        // Assert
        verify(saveChapterPort, times(1)).save(chapterCaptor.capture());
        Chapter capturedChapter = chapterCaptor.getValue();

        assertEquals(courseId, capturedChapter.getCourseId());
        assertEquals("Chapter 1: Introduction", capturedChapter.getTitle());
        assertEquals("Introduction to Java programming", capturedChapter.getDescription());
    }

    @Test
    @DisplayName("Should successfully create a chapter with materials")
    void testCreateChapter_WithMaterials_ShouldSucceed() {
        // Arrange
        List<Material> materials = new ArrayList<>();
        materials.add(Material.builder()
                .title("Tutorial PDF")
                .type(Material.MaterialType.PDF)
                .sourceUrl("https://example.com/tutorial.pdf")
                .downloadable(true)
                .build());
        materials.add(Material.builder()
                .title("Video Lecture")
                .type(Material.MaterialType.VIDEO)
                .sourceUrl("https://example.com/video.mp4")
                .downloadable(false)
                .build());

        Chapter chapterWithMaterials = Chapter.builder()
                .title("Chapter with Materials")
                .description("Has multiple materials")
                .materials(materials)
                .build();

        CreateChapterCommand commandWithMaterials = CreateChapterCommand.builder()
                .courseId(courseId)
                .chapter(chapterWithMaterials)
                .build();

        when(loadSignedInTeacherPort.findAuthenticatedTeacher()).thenReturn(Optional.of(mockTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        UUID materialId1 = UUID.randomUUID();
        UUID materialId2 = UUID.randomUUID();
        List<Material> savedMaterials = new ArrayList<>();
        savedMaterials.add(Material.builder()
                .id(materialId1)
                .title("Tutorial PDF")
                .type(Material.MaterialType.PDF)
                .sourceUrl("https://example.com/tutorial.pdf")
                .downloadable(true)
                .build());
        savedMaterials.add(Material.builder()
                .id(materialId2)
                .title("Video Lecture")
                .type(Material.MaterialType.VIDEO)
                .sourceUrl("https://example.com/video.mp4")
                .downloadable(false)
                .build());

        Chapter savedChapter = Chapter.builder()
                .chapterNumber(2L)
                .title("Chapter with Materials")
                .description("Has multiple materials")
                .courseId(courseId)
                .materials(savedMaterials)
                .createdAt(LocalDateTime.now())
                .build();
        when(saveChapterPort.save(any(Chapter.class))).thenReturn(savedChapter);

        // Act
        CreateChapterResponse response = createChapterService.createChapter(commandWithMaterials);

        // Assert
        assertNotNull(response);
        assertEquals(2L, response.getChapterNumber());
        assertEquals("Chapter with Materials", response.getTitle());
        assertEquals(courseId, response.getCourseId());
        assertNotNull(response.getMaterials());
        assertEquals(2, response.getMaterials().size());

        assertEquals(materialId1, response.getMaterials().get(0).getId());
        assertEquals("Tutorial PDF", response.getMaterials().get(0).getTitle());
        assertEquals("PDF", response.getMaterials().get(0).getType());
        assertEquals("https://example.com/tutorial.pdf", response.getMaterials().get(0).getSourceUrl());
        assertTrue(response.getMaterials().get(0).isDownloadable());

        assertEquals(materialId2, response.getMaterials().get(1).getId());
        assertEquals("Video Lecture", response.getMaterials().get(1).getTitle());
        assertEquals("VIDEO", response.getMaterials().get(1).getType());

        verify(saveChapterPort, times(1)).save(any(Chapter.class));
    }

    @Test
    @DisplayName("Should handle chapter creation with empty materials list")
    void testCreateChapter_WithEmptyMaterials_ShouldSucceed() {
        // Arrange
        when(loadSignedInTeacherPort.findAuthenticatedTeacher()).thenReturn(Optional.of(mockTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        Chapter savedChapter = Chapter.builder()
                .chapterNumber(chapterNumber)
                .title("Chapter 1: Introduction")
                .description("Introduction to Java programming")
                .courseId(courseId)
                .materials(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();
        when(saveChapterPort.save(any(Chapter.class))).thenReturn(savedChapter);

        // Act
        CreateChapterResponse response = createChapterService.createChapter(validCommand);

        // Assert
        assertNotNull(response);
        assertEquals(chapterNumber, response.getChapterNumber());
        assertNotNull(response.getMaterials());
        assertEquals(0, response.getMaterials().size());

        verify(saveChapterPort, times(1)).save(any(Chapter.class));
    }

    @Test
    @DisplayName("Should handle chapter creation with null materials")
    void testCreateChapter_WithNullMaterials_ShouldSucceed() {
        // Arrange
        Chapter chapterNullMaterials = Chapter.builder()
                .title("Chapter with Null Materials")
                .description("Materials list is null")
                .materials(null)
                .build();

        CreateChapterCommand commandNullMaterials = CreateChapterCommand.builder()
                .courseId(courseId)
                .chapter(chapterNullMaterials)
                .build();

        when(loadSignedInTeacherPort.findAuthenticatedTeacher()).thenReturn(Optional.of(mockTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        Chapter savedChapter = Chapter.builder()
                .chapterNumber(chapterNumber)
                .title("Chapter with Null Materials")
                .description("Materials list is null")
                .courseId(courseId)
                .materials(null)
                .createdAt(LocalDateTime.now())
                .build();
        when(saveChapterPort.save(any(Chapter.class))).thenReturn(savedChapter);

        // Act
        CreateChapterResponse response = createChapterService.createChapter(commandNullMaterials);

        // Assert
        assertNotNull(response);
        assertEquals(chapterNumber, response.getChapterNumber());
        assertNull(response.getMaterials());

        verify(saveChapterPort, times(1)).save(any(Chapter.class));
    }

    @Test
    @DisplayName("Should return correct response with all fields populated")
    void testCreateChapter_ShouldReturnCorrectResponse() {
        // Arrange
        when(loadSignedInTeacherPort.findAuthenticatedTeacher()).thenReturn(Optional.of(mockTeacher));
        when(loadCoursePort.findById(courseId)).thenReturn(Optional.of(mockCourse));

        LocalDateTime createdAt = LocalDateTime.now();
        Chapter savedChapter = Chapter.builder()
                .chapterNumber(chapterNumber)
                .title("Chapter 1: Introduction")
                .description("Introduction to Java programming")
                .courseId(courseId)
                .materials(new ArrayList<>())
                .createdAt(createdAt)
                .build();
        when(saveChapterPort.save(any(Chapter.class))).thenReturn(savedChapter);

        // Act
        CreateChapterResponse response = createChapterService.createChapter(validCommand);

        // Assert
        assertEquals(chapterNumber, response.getChapterNumber());
        assertEquals("Chapter 1: Introduction", response.getTitle());
        assertEquals("Introduction to Java programming", response.getDescription());
        assertEquals(courseId, response.getCourseId());
        assertEquals(createdAt, response.getCreatedAt());
    }
}

