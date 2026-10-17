package com.orion.organization_service.dto;

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
public class PurposeRequest {

    /** Purpose code — optional identifier for lookup/update. */
    @JsonAlias({"purposeCode", "purpose_code"})
    private String code;

    /** Purpose name — REQUIRED for CREATE, optional for UPDATE. Max 100 chars. */
    private String name;

    /** Optional short description (max 100 chars). Empty string treated as null. */
    private String description;

    /** Optional expiration timestamp. */
    @JsonAlias({"expire_at", "expireAt"})
    private OffsetDateTime expireAt;
}
