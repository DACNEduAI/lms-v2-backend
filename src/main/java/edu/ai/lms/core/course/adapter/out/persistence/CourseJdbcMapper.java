package edu.ai.lms.core.course.adapter.out.persistence;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.core.course.app.domain.Teacher;
import edu.ai.lms.database.ChapterJdbcEntity;
import edu.ai.lms.database.CourseJdbcEntity;
import edu.ai.lms.database.LearningOutcomeJdbcEntity;
import edu.ai.lms.database.MaterialJdbcEntity;
import edu.ai.lms.database.TeacherJdbcEntity;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class CourseJdbcMapper {

    private final TeacherJdbcMapper teacherJdbcMapper;
    private final ChapterJdbcMapper chapterJdbcMapper;
    private final LearningOutcomeJdbcMapper learningOutcomeJdbcMapper;
    private final MaterialJdbcMapper materialJdbcMapper;

    public CourseJdbcEntity mapToCourseJdbcEntity(Course course, UUID teacherId) {
        return CourseJdbcEntity.builder()
                .id(course.getId())
                .courseName(course.getCourseName())
                .description(course.getDescription())
                .courseCode(course.getCourseCode())
                .teacherId(teacherId)
                .active(course.getActive())
                .coverImage(course.getCoverImage())
                .startDate(course.getStartDate())
                .endDate(course.getEndDate())
                .build();
    }

    public Course mapToCourse(
            CourseJdbcEntity entity,
            TeacherJdbcEntity teacherEntity,
            List<ChapterJdbcEntity> chapterEntities,
            List<LearningOutcomeJdbcEntity> learningOutcomeEntities,
            List<MaterialJdbcEntity> allMaterials
    ) {
        Teacher teacher = teacherJdbcMapper.mapToTeacher(teacherEntity);

        List<Chapter> chapters = new ArrayList<>();
        if (chapterEntities != null) {
            chapters = chapterEntities.stream()
                    .map(chapterEntity -> {
                        List<MaterialJdbcEntity> chapterMaterials = allMaterials != null
                                ? allMaterials.stream()
                                    .filter(m -> m.getChapterId().equals(chapterEntity.getId()))
                                    .collect(Collectors.toList())
                                : new ArrayList<>();
                        return chapterJdbcMapper.mapToChapter(chapterEntity, chapterMaterials);
                    })
                    .collect(Collectors.toList());
        }

        List<LearningOutcome> learningOutcomes = new ArrayList<>();
        if (learningOutcomeEntities != null) {
            learningOutcomes = learningOutcomeEntities.stream()
                    .map(learningOutcomeJdbcMapper::mapToLearningOutcome)
                    .collect(Collectors.toList());
        }

        return Course.builder()
                .id(entity.getId())
                .courseName(entity.getCourseName())
                .description(entity.getDescription())
                .courseCode(entity.getCourseCode())
                .teacher(teacher)
                .active(entity.getActive())
                .coverImage(entity.getCoverImage())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .chapters(chapters)
                .learningOutcomes(learningOutcomes)
                .build();
    }
}
