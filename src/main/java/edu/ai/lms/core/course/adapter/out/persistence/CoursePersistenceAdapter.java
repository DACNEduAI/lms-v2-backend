package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.port.out.DeleteCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadUsersPort;
import edu.ai.lms.core.course.app.port.out.SaveCoursePort;
import edu.ai.lms.core.course.app.port.out.SaveCourseEnrollmentPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CoursePersistenceAdapter implements SaveCoursePort, SaveCourseEnrollmentPort, LoadCoursePort, DeleteCoursePort {

    private final CourseRepository courseRepository;
    private final LoadUsersPort loadUsersPort;
    private final LearningOutcomePersistenceAdapter learningOutcomePersistenceAdapter;
    private final GroupLearningOutcomePersistenceAdapter groupLearningOutcomePersistenceAdapter;
    private final ChapterPersistenceAdapter chapterPersistenceAdapter;

    @Override
    public Course save(Course course) {

        boolean isExistingCourse = course.getId() != null
                && courseRepository.findById(course.getId()).isPresent();
        Course savedCourse = Objects.requireNonNull(courseRepository.save(course));

        // Relationship persistence is currently create-only.
        // For updates (e.g. modify-course), only course table fields are updated.
        UUID courseId = savedCourse.getId();
        if (!isExistingCourse) {
            if (course.getTeacherIds() != null) {
                for (UUID teacherId : course.getTeacherIds()) {
                    courseRepository.insertCourseTeacher(courseId, teacherId);
                }
            }

            if (course.getStudentIds() != null) {
                for (UUID studentId : course.getStudentIds()) {
                    courseRepository.insertCourseStudent(courseId, studentId);
                }
            }

            if (course.getLearningOutcomes() != null) {
                for (LearningOutcome lo : course.getLearningOutcomes()) {
                    lo.setCourseId(courseId);
                    learningOutcomePersistenceAdapter.save(lo);
                }
            }
        }

        // Re-load the full course to return
        return loadFullCourse(savedCourse);
    }

    @Override
    public void addStudentsToCourse(UUID courseId, List<UUID> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return;
        }

        for (UUID studentId : studentIds) {
            courseRepository.insertCourseStudent(courseId, studentId);
        }
    }

    @Override
    public void removeStudentsFromCourse(UUID courseId, List<UUID> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return;
        }

        courseRepository.deleteCourseStudentsByCourseIdAndStudentIds(courseId, studentIds);
    }

    @Override
    public Optional<Course> findById(UUID courseId) {
        return courseRepository.findById(Objects.requireNonNull(courseId))
                .map(this::loadFullCourse);
    }

    @Override
    public Optional<Course> findByCourseCode(String courseCode) {
        return courseRepository.findByCourseCode(courseCode)
                .flatMap(entity -> courseRepository.findById(Objects.requireNonNull(entity.getId())))
                .map(this::loadFullCourse);
    }

    @Override
    public List<Course> findAllByTeacherId(UUID teacherId) {
        return courseRepository.findAllByTeacherId(teacherId).stream()
                .map(entity -> courseRepository.findById(Objects.requireNonNull(entity.getId())))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::loadFullCourse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<Course> findAllByStudentId(UUID studentId) {
        return courseRepository.findAllByStudentId(studentId).stream()
                .map(entity -> courseRepository.findById(Objects.requireNonNull(entity.getId())))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::loadFullCourse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void deleteById(UUID courseId) {
        // All child entities (chapters, learning outcomes, materials, junction tables)
        // are cascade-deleted by the database via ON DELETE CASCADE.
        courseRepository.deleteById(Objects.requireNonNull(courseId));
    }

    /**
     * Loads a full Course aggregate by explicitly fetching related entities.
     */
    private Course loadFullCourse(Course course) {
        List<UUID> enrollmentIds = courseRepository.findEnrollmentIdsByCourseId(course.getId());
        var enrolledUsers = loadUsersPort.findAllByIds(enrollmentIds);
        List<UUID> teacherIds = enrolledUsers.stream()
                .filter(user -> user.role() == edu.ai.lms.common.api.Roles.TEACHER)
                .map(edu.ai.lms.core.course.app.port.out.UserSummaryView::id)
                .toList();
        List<UUID> studentIds = enrolledUsers.stream()
                .filter(user -> user.role() == edu.ai.lms.common.api.Roles.STUDENT)
                .map(edu.ai.lms.core.course.app.port.out.UserSummaryView::id)
                .toList();

        course.setTeacherIds(teacherIds);
        course.setStudentIds(studentIds);
        course.setChapters(chapterPersistenceAdapter.findAllByCourseId(course.getId()));
        course.setLearningOutcomes(learningOutcomePersistenceAdapter.findByCourseId(course.getId()));
        course.setGroupLearningOutcomes(groupLearningOutcomePersistenceAdapter.findByCourseId(course.getId()));
        return course;
    }
}
