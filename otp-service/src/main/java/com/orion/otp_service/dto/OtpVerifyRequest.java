package com.orion.otp_service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyRequest {

    /**
     * Secret key from the project table.
     */
    @JsonProperty("secretKey")
    @JsonAlias({ "secretKey", "secret_key" })
    private String secretKey;

    @JsonProperty("userId")
    @JsonAlias({ "user_id", "userId", "userid" })
    private String userId;

    @JsonProperty("otp")
    @JsonAlias({ "otp", "code" })
    private String otp;
}

