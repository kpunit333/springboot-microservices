package com.orion.otp_service.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "otp.allowed-ips")
public class OtpIpProperties {

    /**
     * Whether IP restriction is enabled for sensitive OTP endpoints.
     */
    private boolean enabled = true;

    /**
     * List of allowed IP addresses, host:port combinations, or CIDR blocks.
     */
    private List<String> ips = new ArrayList<>(List.of(
            "127.0.0.1",
            "::1",
            "0:0:0:0:0:0:0:1",
            "localhost"
    ));
}
