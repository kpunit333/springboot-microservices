package com.orion.organization_service.dto;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationResponse {

    private String name;

    private String code;

    private String email;

    private String status;

    private OffsetDateTime createdAt;

    private OffsetDateTime expireAt;
}
