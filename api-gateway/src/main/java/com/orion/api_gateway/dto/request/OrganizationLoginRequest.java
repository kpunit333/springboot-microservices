package com.orion.api_gateway.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationLoginRequest {

    /**
     * Organization unique code (e.g. ORG04A07EAT0F2482).
     * Optional if organization name is provided.
     */
    @JsonAlias({"organizationCode", "organization_code"})
    private String code;

    /**
     * Organization name (e.g. pwc, Acme Corp).
     * Optional if organization code is provided.
     */
    @JsonAlias({"organizationName", "organization_name"})
    private String name;

    @NotBlank(message = "Password is required")
    private String password;

    public OrganizationLoginRequest(String code, String password) {
        this.code = code;
        this.password = password;
    }

    @AssertTrue(message = "Either organization code or organization name must be provided")
    public boolean isCodeOrNamePresent() {
        return (code != null && !code.isBlank()) || (name != null && !name.isBlank());
    }
}
