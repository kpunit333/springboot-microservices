package com.orion.otp_service.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * Plain (non-load-balanced) RestClient.Builder — registered as PRIMARY so
     * that Eureka's auto-configuration and external REST calls (e.g. Upstash Redis HTTPS)
     * do not go through the Ribbon/Spring Cloud load-balancer interceptor.
     */
    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    /**
     * Load-balanced RestClient.Builder — injected into service clients
     * (e.g., OrganizationServiceClient) via @Qualifier so they can resolve
     * logical Eureka service names like "http://organization-service".
     */
    @Bean
    @LoadBalanced
    @Qualifier("loadBalanced")
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}
