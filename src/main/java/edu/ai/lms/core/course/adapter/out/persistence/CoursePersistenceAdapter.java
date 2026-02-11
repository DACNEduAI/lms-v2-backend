package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.port.out.DeleteCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.SaveCoursePort;
import edu.ai.lms.database.ChapterJdbcEntity;
import edu.ai.lms.database.CourseJdbcEntity;
import edu.ai.lms.database.LearningOutcomeJdbcEntity;
import edu.ai.lms.database.MaterialJdbcEntity;
import edu.ai.lms.database.TeacherJdbcEntity;
import lombok.RequiredArgsConstructor;
@Component
@RequiredArgsConstructor
class CoursePersistenceAdapter implements SaveCoursePort, LoadCoursePort, DeleteCoursePort {

    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final ChapterRepository chapterRepository;
    private final LearningOutcomeRepository learningOutcomeRepository;
    private final MaterialRepository materialRepository;
    private final CourseJdbcMapper courseJdbcMapper;
    private final ChapterJdbcMapper chapterJdbcMapper;
    private final LearningOutcomeJdbcMapper learningOutcomeJdbcMapper;
    private final MaterialJdbcMapper materialJdbcMapper;

    @Override
    public Course save(Course course) {
        UUID teacherId = course.getTeacher().getId();

        // Verify teacher exists
        TeacherJdbcEntity teacherEntity = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found with id: " + teacherId));

        // Save the course entity
        CourseJdbcEntity courseEntity = courseJdbcMapper.mapToCourseJdbcEntity(course, teacherId);
        CourseJdbcEntity savedCourse = courseRepository.save(courseEntity);

        // Save chapters
        if (course.getChapters() != null) {
            for (Chapter chapter : course.getChapters()) {
                ChapterJdbcEntity chapterEntity = chapterJdbcMapper.mapToChapterJdbcEntity(chapter, savedCourse.getId());
                ChapterJdbcEntity savedChapter = chapterRepository.save(chapterEntity);

                // Save materials for each chapter
                if (chapter.getMaterials() != null) {
                    chapter.getMaterials().forEach(material -> {
                        MaterialJdbcEntity materialEntity = materialJdbcMapper.mapToMaterialJdbcEntity(material);
                        materialEntity.setChapterId(savedChapter.getId());
                        materialRepository.save(materialEntity);
                    });
                }
            }
        }

        // Save learning outcomes
        if (course.getLearningOutcomes() != null) {
            for (LearningOutcome lo : course.getLearningOutcomes()) {
                LearningOutcomeJdbcEntity loEntity = learningOutcomeJdbcMapper.mapToEntity(lo, savedCourse.getId());
                learningOutcomeRepository.save(loEntity);
            }
        }

        // Re-load the full course to return
        return loadFullCourse(savedCourse);
    }

    @Override
    public Optional<Course> findById(UUID courseId) {
        return courseRepository.findById(courseId)
                .map(this::loadFullCourse);
    }

    @Override
    public Optional<Course> findByCourseCode(String courseCode) {
        return courseRepository.findByCourseCode(courseCode)
                .map(this::loadFullCourse);
    }

    @Override
    public List<Course> findAllByTeacherId(UUID teacherId) {
        return courseRepository.findAllByTeacherId(teacherId).stream()
                .map(this::loadFullCourse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void deleteById(UUID courseId) {
        // Delete related entities first due to foreign key constraints
        List<ChapterJdbcEntity> chapters = chapterRepository.findByCourseId(courseId);
        
        // Delete materials for each chapter
        for (ChapterJdbcEntity chapter : chapters) {
            materialRepository.deleteByChapterId(chapter.getId());
        }
        
        // Delete chapters
        chapterRepository.deleteByCourseId(courseId);
        
        // Delete learning outcomes
        learningOutcomeRepository.deleteByCourseId(courseId);
        
        // Finally delete the course
        courseRepository.deleteById(courseId);
    }

    /**
     * Loads a full Course aggregate by explicitly fetching related entities.
     * With Spring Data JDBC there are no lazy-loading surprises —
     * we control exactly what gets loaded.
     */
    private Course loadFullCourse(CourseJdbcEntity courseEntity) {
        TeacherJdbcEntity teacherEntity = teacherRepository.findById(courseEntity.getTeacherId())
                .orElse(null);

        List<ChapterJdbcEntity> chapters = chapterRepository.findByCourseId(courseEntity.getId());

        // Load all materials for all chapters in one batch
        List<MaterialJdbcEntity> allMaterials = new ArrayList<>();
        for (ChapterJdbcEntity chapter : chapters) {
            allMaterials.addAll(materialRepository.findByChapterId(chapter.getId()));
        }

        List<LearningOutcomeJdbcEntity> learningOutcomes = learningOutcomeRepository.findByCourseId(courseEntity.getId());

        return courseJdbcMapper.mapToCourse(courseEntity, teacherEntity, chapters, learningOutcomes, allMaterials);
    }
}
