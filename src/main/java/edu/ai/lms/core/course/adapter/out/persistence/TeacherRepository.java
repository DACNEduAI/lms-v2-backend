package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ai.lms.database.TeacherJpaEntity;

/**
 * Package-private repository interface.
 * Only classes within the persistence package can access this repository.
 */
interface TeacherRepository extends JpaRepository<TeacherJpaEntity, UUID> {
}
