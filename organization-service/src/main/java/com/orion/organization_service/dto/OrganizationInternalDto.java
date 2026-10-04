package com.orion.organization_service.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationInternalDto {

    private Long id;

    private String name;

    private String code;

    private String password;

    private String status;

    private LocalDateTime expiryAt;
}
