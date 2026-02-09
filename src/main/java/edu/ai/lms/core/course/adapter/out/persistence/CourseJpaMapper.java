package edu.ai.lms.core.course.adapter.out.persistence;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.database.ChapterJpaEntity;
import edu.ai.lms.database.CourseJpaEntity;
import edu.ai.lms.database.LearningOutcomeJpaEntity;
import edu.ai.lms.database.TeacherJpaEntity;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class CourseJpaMapper {

    private final TeacherJpaMapper teacherJpaMapper;
    private final ChapterJpaMapper chapterJpaMapper;
    private final LearningOutcomeMapper learningOutcomeMapper;

    public CourseJpaEntity mapToCourseJpaEntity(Course course, TeacherJpaEntity teacherEntity) {
        CourseJpaEntity courseEntity = CourseJpaEntity.builder()
                .id(course.getId())
                .courseName(course.getCourseName())
                .description(course.getDescription())
                .courseCode(course.getCourseCode())
                .teacher(teacherEntity)
                .active(course.getActive())
                .coverImage(course.getCoverImage())
                .startDate(course.getStartDate())
                .endDate(course.getEndDate())
                .chapters(new ArrayList<>())
                .learningOutcomes(new ArrayList<>())
                .build();

        // Map chapters
        if (course.getChapters() != null) {
            List<ChapterJpaEntity> chapterEntities = course.getChapters().stream()
                    .map(chapter -> chapterJpaMapper.mapToChapterJpaEntity(chapter, courseEntity))
                    .collect(Collectors.toList());
            courseEntity.setChapters(chapterEntities);
        }

        // Map learning outcomes
        if (course.getLearningOutcomes() != null) {
            List<LearningOutcomeJpaEntity> learningOutcomeEntities = course.getLearningOutcomes().stream()
                    .map(lo -> learningOutcomeMapper.mapToEntity(lo, courseEntity))
                    .collect(Collectors.toList());
            courseEntity.setLearningOutcomes(learningOutcomeEntities);
        }

        return courseEntity;
    }

    public Course mapToCourse(CourseJpaEntity entity) {
        Course course = Course.builder()
                .id(entity.getId())
                .courseName(entity.getCourseName())
                .description(entity.getDescription())
                .courseCode(entity.getCourseCode())
                .teacher(teacherJpaMapper.mapToTeacher(entity.getTeacher()))
                .active(entity.getActive())
                .coverImage(entity.getCoverImage())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .chapters(new ArrayList<>())
                .learningOutcomes(new ArrayList<>())
                .build();

        // Map chapters
        if (entity.getChapters() != null) {
            List<Chapter> chapters = entity.getChapters().stream()
                    .map(chapterEntity -> chapterJpaMapper.mapToChapter(chapterEntity, course))
                    .collect(Collectors.toList());
            course.setChapters(chapters);
        }

        // Map learning outcomes
        if (entity.getLearningOutcomes() != null) {
            List<LearningOutcome> learningOutcomes = entity.getLearningOutcomes().stream()
                    .map(loEntity -> learningOutcomeMapper.mapToLearningOutcome(loEntity, course))
                    .collect(Collectors.toList());
            course.setLearningOutcomes(learningOutcomes);
        }

        return course;
    }
}
