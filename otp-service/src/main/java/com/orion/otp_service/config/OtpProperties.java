package com.orion.otp_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Getter
@Configuration
public class OtpProperties {

    @Value("${upstash.redis.rest.url:https://fast-monitor-176817.upstash.io}")
    private String upstashRedisRestUrl;

    @Value("${upstash.redis.rest.token:gQAAAAAAArKxAAIgcDFmN2YxMTUyNTUyZjQ0YmI5ODdlZjQwMjEyOTk5NDExMw}")
    private String upstashRedisRestToken;

    @Value("${otp.ttl.seconds:300}")
    private long ttlSeconds;

    @Value("${otp.validation.project-purpose.enabled:true}")
    private boolean validationEnabled;

    @Value("${otp.validation.strict:false}")
    private boolean validationStrict;
}
