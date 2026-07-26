package edu.ai.lms.core.user.api;

import edu.ai.lms.common.api.Roles;

import java.util.Optional;

public interface LoginAccountApi {

    Optional<LoginAccountView> findByEmailAndRole(String email, Roles role);
}
