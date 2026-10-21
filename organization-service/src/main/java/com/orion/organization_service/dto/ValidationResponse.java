package com.orion.organization_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResponse {

    private boolean valid;

    private String message;

    private Long organizationId;

    private String secretKey;
}

