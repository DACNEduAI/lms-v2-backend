package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.SaveCoursePort;
import edu.ai.lms.database.CourseJpaEntity;
import edu.ai.lms.database.TeacherJpaEntity;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CoursePersistenceAdapter implements SaveCoursePort, LoadCoursePort {

    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final CourseJpaMapper courseJpaMapper;

    @Override
    public Course save(Course course) {
        // Load the teacher entity
        TeacherJpaEntity teacherEntity = teacherRepository.findById(course.getTeacher().getId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found with id: " + course.getTeacher().getId()));

        CourseJpaEntity courseEntity = courseJpaMapper.mapToCourseJpaEntity(course, teacherEntity);
        CourseJpaEntity savedEntity = courseRepository.save(courseEntity);
        return courseJpaMapper.mapToCourse(savedEntity);
    }

    @Override
    public Optional<Course> findById(UUID courseId) {
        return courseRepository.findById(courseId)
                .map(courseJpaMapper::mapToCourse);
    }

    @Override
    public Optional<Course> findByCourseCode(String courseCode) {
        return courseRepository.findByCourseCode(courseCode)
                .map(courseJpaMapper::mapToCourse);
    }
}
