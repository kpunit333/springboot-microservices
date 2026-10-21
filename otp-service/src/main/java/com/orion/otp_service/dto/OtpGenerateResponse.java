package com.orion.otp_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpGenerateResponse {

    private String secretKey;

    private String userId;

    private String purposeId;

    private String otp;

    private long expiresInSeconds;
}

