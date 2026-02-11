package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.ai.lms.database.MaterialJdbcEntity;

/**
 * Package-private repository interface for materials.
 */
@Repository
interface MaterialRepository extends CrudRepository<MaterialJdbcEntity, UUID> {

    @Query("SELECT * FROM materials WHERE chapter_id = :chapterId")
    List<MaterialJdbcEntity> findByChapterId(@Param("chapterId") UUID chapterId);

    @Modifying
    @Query("DELETE FROM materials WHERE chapter_id = :chapterId")
    void deleteByChapterId(@Param("chapterId") UUID chapterId);
}
