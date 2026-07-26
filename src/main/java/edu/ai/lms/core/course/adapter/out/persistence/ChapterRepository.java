package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import edu.ai.lms.core.course.app.domain.Chapter;

/**
 * Package-private repository interface for chapters.
 * chapter_id is a UUID primary key, while course_id is used for scoped queries.
 */
@Mapper
interface ChapterRepository {

        List<Chapter> findByCourseId(@Param("courseId") UUID courseId);

        Optional<Chapter> findByChapterIdAndCourseId(
            @Param("chapterId") UUID chapterId,
            @Param("courseId") UUID courseId);

    void deleteByCourseId(@Param("courseId") UUID courseId);

    void deleteByChapterIdAndCourseId(
            @Param("chapterId") UUID chapterId,
            @Param("courseId") UUID courseId);

    void deleteQuizChapterLinks(@Param("chapterId") UUID chapterId);

    UUID insertAndReturnChapterId(
            @Param("courseId") UUID courseId,
            @Param("title") String title,
            @Param("description") String description);

    void updateByCompositeKey(
            @Param("chapterId") UUID chapterId,
            @Param("courseId") UUID courseId,
            @Param("title") String title,
            @Param("description") String description);

    List<UUID> findLearningOutcomeIdsByChapterKey(
            @Param("chapterId") UUID chapterId,
            @Param("courseId") UUID courseId);

    void insertChapterLearningOutcome(
            @Param("chapterId") UUID chapterId,
            @Param("courseId") UUID courseId,
            @Param("learningOutcomeId") UUID learningOutcomeId);

    void deleteChapterLearningOutcome(
            @Param("chapterId") UUID chapterId,
            @Param("courseId") UUID courseId,
            @Param("learningOutcomeId") UUID learningOutcomeId);
}
