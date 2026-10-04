package com.orion.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequest {

    /**
     * Login handle — required on CREATE; used as identifier on UPDATE/DELETE.
     * Must be unique across the system.
     */
    private String username;

    /** Display name of the user. */
    private String name;

    /** Raw (plain-text) password; will be BCrypt-encoded before persisting. */
    private String password;

    /** Role assigned to the user, e.g. "USER", "ROLE_ADMIN". */
    private String role;
}
