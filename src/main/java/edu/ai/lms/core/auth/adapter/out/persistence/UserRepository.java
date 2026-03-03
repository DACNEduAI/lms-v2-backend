package edu.ai.lms.core.auth.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.ai.lms.database.UserJdbcTable;

/**
 * Package-private repository interface for users.
 * Only classes within the persistence package can access this repository.
 */
@Repository
interface UserRepository extends CrudRepository<UserJdbcTable, UUID> {

    @Query("SELECT * FROM users WHERE email = :email")
    Optional<UserJdbcTable> findByEmail(@Param("email") String email);
}
