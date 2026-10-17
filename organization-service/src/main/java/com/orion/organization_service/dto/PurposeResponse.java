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
public class PurposeResponse {

    private String code;

    private String name;

    private String description;

    private OffsetDateTime createdAt;

    private OffsetDateTime expireAt;
}
