package com.orion.api_gateway.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndividualLoginRequest {

    /**
     * User's unique handle, identifier, or email address.
     */
    @JsonAlias({"identifier", "user", "login"})
    private String username;

    /**
     * Explicit email field as alternative to username.
     */
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    public IndividualLoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Resolves the login identifier from either username or email.
     */
    public String getLoginIdentifier() {
        if (this.username != null && !this.username.isBlank()) {
            return this.username.trim();
        }
        if (this.email != null && !this.email.isBlank()) {
            return this.email.trim();
        }
        return null;
    }
}
