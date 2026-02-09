package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import edu.ai.lms.database.MaterialJdbcEntity;
import org.springframework.stereotype.Repository;

/**
 * Package-private repository interface for materials.
 */
@Repository
interface MaterialRepository extends CrudRepository<MaterialJdbcEntity, UUID> {

    @Query("SELECT * FROM materials WHERE chapter_id = :chapterId")
    List<MaterialJdbcEntity> findByChapterId(@Param("chapterId") UUID chapterId);
}
