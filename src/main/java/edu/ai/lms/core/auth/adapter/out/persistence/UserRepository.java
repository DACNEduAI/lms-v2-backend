package edu.ai.lms.core.auth.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.ai.lms.database.StudentJpaEntity;
import edu.ai.lms.database.TeacherJpaEntity;
import edu.ai.lms.database.UserJpaEntity;

/**
 * Package-private repository interface.
 * Only classes within the persistence package can access this repository.
 */
interface UserRepository extends JpaRepository<UserJpaEntity, UUID> {
    Optional<StudentJpaEntity> findStudentByEmail(String email);
    Optional<TeacherJpaEntity> findTeacherByEmail(String email);
}
