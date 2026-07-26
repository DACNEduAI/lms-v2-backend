package edu.ai.lms.core.course.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.app.domain.CurrentActor;
import edu.ai.lms.core.course.adapter.in.response.CreateChapterResponse;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.CreateChapterCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherCreateChapterUseCase;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveChapterPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateChapterService implements TeacherCreateChapterUseCase {

    private final SaveChapterPort saveChapterPort;
    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInUserPort loadSignedInUserPort;

    @Override
    @Transactional
    public CreateChapterResponse createChapter(CreateChapterCommand command) {
        // Verify teacher is authenticated
        CurrentActor user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));

        // Verify course exists and belongs to the authenticated teacher
        Course course = loadCoursePort.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + command.courseId()));

        if (!course.getTeacherIds().contains(user.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to add chapters to this course");
        }

        // Set the courseId on the chapter
        Chapter chapter = command.chapter();
        chapter.setCourseId(course.getId());

        // Save chapter
        Chapter savedChapter = saveChapterPort.save(chapter);

        return CreateChapterResponse.builder()
                .chapterId(savedChapter.getChapterId())
                .title(savedChapter.getTitle())
                .description(savedChapter.getDescription())
                .courseId(command.courseId())
                .createdAt(savedChapter.getCreatedAt())
                .build();
    }
}
