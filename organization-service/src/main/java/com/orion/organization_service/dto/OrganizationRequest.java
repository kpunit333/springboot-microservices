package com.orion.organization_service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationRequest {

    /** Required for CREATE — the organization name. Optional for UPDATE. */
    private String name;

    /**
     * Required for CREATE (raw password).
     * Required for UPDATE (used to verify existing credentials before making updates).
     */
    private String password;

    /**
     * Optional for UPDATE — new raw password if updating the organization password.
     */
    @JsonAlias({"new_password", "newPassword"})
    private String newPassword;

    /**
     * Required for UPDATE, login, and lookups — identifies which organization to operate on.
     * NOT accepted/used during CREATE (code is auto-generated).
     */
    @JsonAlias({"organizationCode", "organization_code"})
    private String code;
}
