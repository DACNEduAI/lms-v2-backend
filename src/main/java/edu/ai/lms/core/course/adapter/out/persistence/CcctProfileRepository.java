package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import edu.ai.lms.core.course.app.domain.CcctChapter;
import edu.ai.lms.core.course.app.domain.CcctItem;
import edu.ai.lms.core.course.app.domain.CcctProfile;

@Mapper
interface CcctProfileRepository {
    Optional<CcctProfile> findByCourseIdAndId(
            @Param("courseId") UUID courseId,
            @Param("profileId") UUID profileId);

    Optional<CcctProfile> findPublishedByCourseId(@Param("courseId") UUID courseId);

    List<CcctProfile> findAllByCourseId(@Param("courseId") UUID courseId);

    boolean existsByCourseIdAndNameExcludingId(
            @Param("courseId") UUID courseId,
            @Param("name") String name,
            @Param("excludedProfileId") UUID excludedProfileId);

    List<CcctChapter> findChaptersByProfileId(@Param("profileId") UUID profileId);

    List<CcctItem> findItemsByChapterIds(@Param("chapterIds") Collection<UUID> chapterIds);

    int insertProfile(CcctProfile profile);

    int updateDraft(
            @Param("profile") CcctProfile profile,
            @Param("expectedVersion") long expectedVersion);

    int insertChapters(@Param("chapters") List<CcctChapter> chapters);

    int insertItems(@Param("items") List<CcctItem> items);

    int deleteChaptersByProfileId(@Param("profileId") UUID profileId);

    int deleteDraft(
            @Param("courseId") UUID courseId,
            @Param("profileId") UUID profileId);

    int archivePublished(
            @Param("courseId") UUID courseId,
            @Param("publishedProfileId") UUID publishedProfileId);

    int publishDraft(
            @Param("courseId") UUID courseId,
            @Param("profileId") UUID profileId,
            @Param("expectedVersion") long expectedVersion,
            @Param("publishedAt") java.time.LocalDateTime publishedAt);
}
