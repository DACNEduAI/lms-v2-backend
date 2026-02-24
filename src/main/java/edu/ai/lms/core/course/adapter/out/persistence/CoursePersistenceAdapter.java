package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.adapter.out.persistence.mapper.ChapterJdbcMapper;
import edu.ai.lms.core.course.adapter.out.persistence.mapper.CourseJdbcMapper;
import edu.ai.lms.core.course.adapter.out.persistence.mapper.LearningOutcomeJdbcMapper;
import edu.ai.lms.core.course.adapter.out.persistence.mapper.MaterialJdbcMapper;
import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.port.out.DeleteCoursePort;
import edu.ai.lms.core.course.app.port.out.LoadCoursePort;
import edu.ai.lms.core.course.app.port.out.SaveCoursePort;
import edu.ai.lms.database.ChapterJdbcTable;
import edu.ai.lms.database.CourseJdbcTable;
import edu.ai.lms.database.LearningOutcomeJdbcTable;
import edu.ai.lms.database.MaterialJdbcTable;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CoursePersistenceAdapter implements SaveCoursePort, LoadCoursePort, DeleteCoursePort {

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LearningOutcomeRepository learningOutcomeRepository;
    private final MaterialRepository materialRepository;
    private final CourseJdbcMapper courseJdbcMapper;
    private final ChapterJdbcMapper chapterJdbcMapper;
    private final LearningOutcomeJdbcMapper learningOutcomeJdbcMapper;
    private final MaterialJdbcMapper materialJdbcMapper;

    @Override
    public Course save(Course course) {
        // Save the course entity (junction table refs are managed via @MappedCollection)
        CourseJdbcTable courseEntity = courseJdbcMapper.mapToCourseJdbcEntity(course);
        CourseJdbcTable savedCourse = courseRepository.save(courseEntity);

        // Save chapters
        if (course.getChapters() != null) {
            for (Chapter chapter : course.getChapters()) {
                ChapterJdbcTable chapterEntity = chapterJdbcMapper.mapToChapterJdbcEntity(chapter, savedCourse.getId());
                ChapterJdbcTable savedChapter = chapterRepository.save(chapterEntity);

                // Save materials for each chapter
                if (chapter.getMaterials() != null) {
                    chapter.getMaterials().forEach(material -> {
                        MaterialJdbcTable materialEntity = materialJdbcMapper.mapToMaterialJdbcEntity(material);
                        materialEntity.setChapterId(savedChapter.getId());
                        materialRepository.save(materialEntity);
                    });
                }
            }
        }

        // Save learning outcomes
        if (course.getLearningOutcomes() != null) {
            for (LearningOutcome lo : course.getLearningOutcomes()) {
                LearningOutcomeJdbcTable loEntity = learningOutcomeJdbcMapper.mapToEntity(lo, savedCourse.getId());
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
                .flatMap(entity -> courseRepository.findById(entity.getId()))
                .map(this::loadFullCourse);
    }

    @Override
    public List<Course> findAllByTeacherId(UUID teacherId) {
        return courseRepository.findAllByTeacherId(teacherId).stream()
                .map(entity -> courseRepository.findById(entity.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::loadFullCourse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void deleteById(UUID courseId) {
        // Delete related entities first due to foreign key constraints
        List<ChapterJdbcTable> chapters = chapterRepository.findByCourseId(courseId);
        
        // Delete materials for each chapter
        for (ChapterJdbcTable chapter : chapters) {
            materialRepository.deleteByChapterId(chapter.getId());
        }
        
        // Delete chapters
        chapterRepository.deleteByCourseId(courseId);
        
        // Delete learning outcomes
        learningOutcomeRepository.deleteByCourseId(courseId);
        
        // Finally delete the course (junction table entries are cascade-deleted
        // via @MappedCollection and ON DELETE CASCADE)
        courseRepository.deleteById(courseId);
    }

    /**
     * Loads a full Course aggregate by explicitly fetching related entities.
     * Teacher/student IDs are extracted from the entity's @MappedCollection sets
     * (populated automatically by Spring Data JDBC when loading via findById).
     * Chapters, materials, and learning outcomes are loaded manually.
     */
    private Course loadFullCourse(CourseJdbcTable courseEntity) {
        List<ChapterJdbcTable> chapters = chapterRepository.findByCourseId(courseEntity.getId());

        // Load all materials for all chapters in one batch
        List<MaterialJdbcTable> allMaterials = new ArrayList<>();
        for (ChapterJdbcTable chapter : chapters) {
            allMaterials.addAll(materialRepository.findByChapterId(chapter.getId()));
        }

        List<LearningOutcomeJdbcTable> learningOutcomes = learningOutcomeRepository.findByCourseId(courseEntity.getId());

        return courseJdbcMapper.mapToCourse(courseEntity, chapters, learningOutcomes, allMaterials);
    }
}
