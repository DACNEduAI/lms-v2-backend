package edu.ai.lms.core.auth.adapter.out.repository;

import edu.ai.lms.database.StudentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<StudentJpaEntity, UUID> {
    Optional<StudentJpaEntity> findByEmail(String email);
}
