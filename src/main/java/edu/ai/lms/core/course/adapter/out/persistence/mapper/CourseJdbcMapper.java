package edu.ai.lms.core.course.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.ai.lms.core.course.app.domain.Chapter;
import edu.ai.lms.core.course.app.domain.Course;
import edu.ai.lms.core.course.app.domain.LearningOutcome;
import edu.ai.lms.database.ChapterJdbcTable;
import edu.ai.lms.database.CourseJdbcTable;
import edu.ai.lms.database.CourseTeacherJdbcTable;
import edu.ai.lms.database.CourseStudentJdbcTable;
import edu.ai.lms.database.LearningOutcomeJdbcTable;
import edu.ai.lms.database.MaterialJdbcTable;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CourseJdbcMapper {

    private final ChapterJdbcMapper chapterJdbcMapper;
    private final LearningOutcomeJdbcMapper learningOutcomeJdbcMapper;

    public CourseJdbcTable mapToCourseJdbcEntity(Course course) {
        Set<CourseTeacherJdbcTable> courseTeachers = new HashSet<>();
        if (course.getTeacherIds() != null) {
            courseTeachers = course.getTeacherIds().stream()
                    .map(id -> CourseTeacherJdbcTable.builder().teacherId(id).build())
                    .collect(Collectors.toSet());
        }

        Set<CourseStudentJdbcTable> courseStudents = new HashSet<>();
        if (course.getStudentIds() != null) {
            courseStudents = course.getStudentIds().stream()
                    .map(id -> CourseStudentJdbcTable.builder().studentId(id).build())
                    .collect(Collectors.toSet());
        }

        return CourseJdbcTable.builder()
                .id(course.getId())
                .courseName(course.getCourseName())
                .description(course.getDescription())
                .courseCode(course.getCourseCode())
                .active(course.getActive())
                .coverImage(course.getCoverImage())
                .startDate(course.getStartDate())
                .endDate(course.getEndDate())
                .courseTeachers(courseTeachers)
                .courseStudents(courseStudents)
                .build();
    }

    public Course mapToCourse(
            CourseJdbcTable entity,
            List<ChapterJdbcTable> chapterEntities,
            List<LearningOutcomeJdbcTable> learningOutcomeEntities,
            List<MaterialJdbcTable> allMaterials
    ) {
        List<UUID> teacherIds = entity.getCourseTeachers() != null
                ? entity.getCourseTeachers().stream()
                    .map(CourseTeacherJdbcTable::getTeacherId)
                    .collect(Collectors.toList())
                : new ArrayList<>();

        List<UUID> studentIds = entity.getCourseStudents() != null
                ? entity.getCourseStudents().stream()
                    .map(CourseStudentJdbcTable::getStudentId)
                    .collect(Collectors.toList())
                : new ArrayList<>();

        List<Chapter> chapters = new ArrayList<>();
        if (chapterEntities != null) {
            chapters = chapterEntities.stream()
                    .map(chapterEntity -> {
                        List<MaterialJdbcTable> chapterMaterials = allMaterials != null
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
                .teacherIds(teacherIds)
                .studentIds(studentIds)
                .active(entity.getActive())
                .coverImage(entity.getCoverImage())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .chapters(chapters)
                .learningOutcomes(learningOutcomes)
                .build();
    }
}
