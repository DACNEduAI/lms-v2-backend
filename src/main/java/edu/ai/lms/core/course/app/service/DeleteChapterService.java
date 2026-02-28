package edu.ai.lms.core.course.app.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ai.lms.core.course.adapter.in.response.DeleteChapterResponse;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.Teacher;
import edu.ai.lms.core.course.app.exception.ChapterNotFoundException;
import edu.ai.lms.core.course.app.exception.CourseNotFoundException;
import edu.ai.lms.core.course.app.exception.TeacherNotFoundException;
import edu.ai.lms.core.course.app.port.in.usecase.TeacherDeleteChapterUseCase;
import edu.ai.lms.core.course.app.port.out.DeleteChapterPort;
import edu.ai.lms.core.course.app.port.out.LoadChapterPort;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadSignedInTeacherPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteChapterService implements TeacherDeleteChapterUseCase {

    private final DeleteChapterPort deleteChapterPort;
    private final LoadChapterPort loadChapterPort;
    private final LoadCoursePort loadCoursePort;
    private final LoadSignedInTeacherPort loadSignedInTeacherPort;

    @Override
    @Transactional
    public DeleteChapterResponse deleteChapter(UUID courseId, Long chapterNumber) {
        // Verify teacher is authenticated
        Teacher teacher = loadSignedInTeacherPort.findAuthenticatedTeacher()
                .orElseThrow(() -> new TeacherNotFoundException("Teacher not authenticated"));

        // Verify course exists
        Course course = loadCoursePort.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + courseId));

        // Business rule: Validate that the teacher is associated with the course
        if (!course.getTeacherIds().contains(teacher.getId())) {
            throw new IllegalArgumentException("Teacher is not authorized to delete chapters in this course");
        }

        // Verify chapter exists
        Chapter existingChapter = loadChapterPort.findByKeyWithoutMaterials(courseId, chapterNumber)
                .orElseThrow(() -> new ChapterNotFoundException(
                        "Chapter not found with number: " + chapterNumber + " in course: " + courseId));

        // Delete the chapter
        deleteChapterPort.deleteByKey(courseId, chapterNumber);

        return DeleteChapterResponse.builder()
                .courseId(courseId)
                .chapterNumber(chapterNumber)
                .message("Chapter deleted successfully")
                .success(true)
                .build();
    }
}
