package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ai.lms.database.CourseJpaEntity;

/**
 * Package-private repository interface.
 * Only classes within the persistence package can access this repository.
 */
interface CourseRepository extends JpaRepository<CourseJpaEntity, UUID> {
    Optional<CourseJpaEntity> findByCourseCode(String courseCode);
}
