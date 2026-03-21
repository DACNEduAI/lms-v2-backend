package edu.ai.lms.core.course.app.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.auth.app.domain.UserAuth;
import edu.ai.lms.core.course.adapter.in.response.ModifyChapterResponse;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.exception.ChapterNotFoundException;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.command.ModifyChapterCommand;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherModifyChapterUseCase;
import edu.ai.lms.core.course.app.port.out.LoadChapterPort;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInUserPort;
import edu.ai.lms.core.course.app.port.out.SaveChapterPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModifyChapterService implements TeacherModifyChapterUseCase {

    private final SaveChapterPort saveChapterPort;
    private final LoadChapterPort loadChapterPort;
    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInUserPort loadSignedInUserPort;

    @Override
    @Transactional
    public ModifyChapterResponse modifyChapter(ModifyChapterCommand command) {
        // Verify teacher is authenticated
        UserAuth user = loadSignedInUserPort.findAuthenticatedUser()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));

        // Verify course exists
        Course course = loadCoursePort.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + command.courseId()));

        // Business rule: Validate that the teacher is associated with the course
        if (!course.getTeacherIds().contains(user.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to modify chapters in this course");
        }

        // Verify chapter exists
        Chapter existingChapter = loadChapterPort.findByKey(command.courseId(), command.chapterId())
                .orElseThrow(() -> new ChapterNotFoundException(
                        "Chapter not found with number: " + command.chapterId() + " in course: " + command.courseId()));

        // Update chapter with new data
        Chapter updatedChapter = Chapter.builder()
                .chapterId(existingChapter.getChapterId())
                .courseId(existingChapter.getCourseId())
                .title(command.chapter().getTitle())
                .description(command.chapter().getDescription())
                .createdAt(existingChapter.getCreatedAt())
                .build();

        // Save the updated chapter
        Chapter savedChapter = saveChapterPort.save(updatedChapter);

        return ModifyChapterResponse.builder()
                .chapterId(savedChapter.getChapterId())
                .title(savedChapter.getTitle())
                .description(savedChapter.getDescription())
                .courseId(savedChapter.getCourseId())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
