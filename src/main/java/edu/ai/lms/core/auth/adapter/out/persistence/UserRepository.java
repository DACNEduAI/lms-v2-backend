package edu.ai.lms.core.auth.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import edu.ai.lms.core.auth.app.domain.UserAuth;

/**
 * Package-private repository interface for users.
 * Only classes within the persistence package can access this repository.
 */
@Mapper
interface UserRepository {

    Optional<UserAuth> findById(@Param("id") UUID id);

    Optional<UserAuth> findByEmail(@Param("email") String email);
    
    java.util.List<UserAuth> findByIds(@Param("ids") java.util.List<UUID> ids);

    int insert(UserAuth user);

    int update(UserAuth user);

    default UserAuth save(UserAuth user) {
        if (user.getId() == null) {
            user.setId(java.util.UUID.randomUUID());
            insert(user);
        } else {
            int updated = update(user);
            if (updated == 0) {
                insert(user);
            }
        }
        return findById(user.getId()).orElseThrow();
    }

    void deleteById(@Param("id") UUID id);
}
