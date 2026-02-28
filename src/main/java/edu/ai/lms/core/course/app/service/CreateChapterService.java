package edu.ai.lms.core.course.app.service;

import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.adapter.in.response.CreateChapterResponse;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.Teacher;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.CreateChapterCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherCreateChapterUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInTeacherPort;
import edu.ai.lms.core.course.app.port.out.SaveChapterPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateChapterService implements TeacherCreateChapterUseCase {

    private final SaveChapterPort saveChapterPort;
    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInTeacherPort loadSignedInTeacherPort;

    @Override
    @Transactional
    public CreateChapterResponse createChapter(CreateChapterCommand command) {
        // Verify teacher is authenticated
        Teacher teacher = loadSignedInTeacherPort.findAuthenticatedTeacher()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));

        // Verify course exists and belongs to the authenticated teacher
        Course course = loadCoursePort.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + command.courseId()));

        if (!course.getTeacherIds().contains(teacher.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to add chapters to this course");
        }

        // Set the courseId on the chapter
        Chapter chapter = command.chapter();
        chapter.setCourseId(course.getId());

        // Save chapter (including materials)
        Chapter savedChapter = saveChapterPort.save(chapter);

        return CreateChapterResponse.builder()
                .chapterNumber(savedChapter.getChapterNumber())
                .title(savedChapter.getTitle())
                .description(savedChapter.getDescription())
                .courseId(command.courseId())
                .materials(savedChapter.getMaterials() != null
                        ? savedChapter.getMaterials().stream()
                                .map(m -> CreateChapterResponse.MaterialDto.builder()
                                        .id(m.getId())
                                        .title(m.getTitle())
                                        .type(m.getType() != null ? m.getType().name() : null)
                                        .sourceUrl(m.getSourceUrl())
                                        .downloadable(m.isDownloadable())
                                        .build())
                                .collect(Collectors.toList())
                        : null)
                .createdAt(savedChapter.getCreatedAt())
                .build();
    }
}
