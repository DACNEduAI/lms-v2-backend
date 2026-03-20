package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import edu.ai.lms.core.course.app.domain.Chapter;

/**
 * Package-private repository interface for chapters.
 * Chapter is a weak entity with composite primary key (chapter_number, course_id).
 * We use Long as the ID type (chapter_number) but handle updates via custom queries
 * because the actual primary key is composite and includes course_id.
 */
@Mapper
interface ChapterRepository {

        List<Chapter> findByCourseId(@Param("courseId") UUID courseId);

        Optional<Chapter> findByChapterNumberAndCourseId(
            @Param("chapterNumber") Long chapterNumber,
            @Param("courseId") UUID courseId);

    void deleteByCourseId(@Param("courseId") UUID courseId);

    void deleteByChapterNumberAndCourseId(
            @Param("chapterNumber") Long chapterNumber,
            @Param("courseId") UUID courseId);

    /**
     * Custom INSERT that returns the auto-generated chapter_number.
     * Used for creating new chapters since chapter_number is GENERATED ALWAYS AS IDENTITY.
     */
    Long insertAndReturnChapterNumber(
            @Param("courseId") UUID courseId,
            @Param("title") String title,
            @Param("description") String description);

    /**
     * Custom UPDATE by composite key (chapter_number, course_id).
     * Standard save() doesn't work properly with composite keys that have auto-generated parts.
     */
    void updateByCompositeKey(
            @Param("chapterNumber") Long chapterNumber,
            @Param("courseId") UUID courseId,
            @Param("title") String title,
            @Param("description") String description);

    List<UUID> findLearningOutcomeIdsByChapterKey(
            @Param("chapterNumber") Long chapterNumber,
            @Param("courseId") UUID courseId);

    void insertChapterLearningOutcome(
            @Param("chapterNumber") Long chapterNumber,
            @Param("courseId") UUID courseId,
            @Param("learningOutcomeId") UUID learningOutcomeId);

    void deleteChapterLearningOutcome(
            @Param("chapterNumber") Long chapterNumber,
            @Param("courseId") UUID courseId,
            @Param("learningOutcomeId") UUID learningOutcomeId);
}
