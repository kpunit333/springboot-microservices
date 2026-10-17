package com.orion.user_service.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    /** User email address — required on CREATE. */
    private String email;

    /** Raw (plain-text) password; will be BCrypt-encoded before persisting. */
    private String password;

    /** Status of user account, e.g. "ACTIVE", "INACTIVE", "LOCKED". */
    private String status;

    /** Role assigned to the user, e.g. "USER", "ROLE_ADMIN". */
    private String role;

    /** Optional expiration timestamp. */
    @JsonAlias({"expire_at", "expireAt"})
    private OffsetDateTime expireAt;

    /** Optional unlock timestamp. */
    @JsonAlias({"unlock_at", "unlockAt"})
    private OffsetDateTime unlockAt;
}
