package com.orion.api_gateway.client.dto;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgInternalDto {

    private Long id;

    private String name;

    private String code;

    private String email;

    private String password;

    private String status;

    private OffsetDateTime createdAt;

    private OffsetDateTime expireAt;
}
