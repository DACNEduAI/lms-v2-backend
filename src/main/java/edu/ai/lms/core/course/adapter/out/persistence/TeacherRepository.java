package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import edu.ai.lms.database.TeacherJdbcEntity;
import org.springframework.stereotype.Repository;

/**
 * Package-private repository interface for teachers.
 * Only classes within the persistence package can access this repository.
 */
@Repository
interface TeacherRepository extends CrudRepository<TeacherJdbcEntity, UUID> {
}
