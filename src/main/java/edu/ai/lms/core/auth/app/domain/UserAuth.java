package edu.ai.lms.core.auth.app.domain;

import edu.ai.lms.common.api.Roles;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserAuth {
    private UUID id;
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private Roles role;

    /**
     * Converts the current role enum to a String.
     */
    public String getRoleAsString() {
        return this.role != null ? this.role.name() : null;
    }

    public String getPassword() {
        return this.password;
    }
}
