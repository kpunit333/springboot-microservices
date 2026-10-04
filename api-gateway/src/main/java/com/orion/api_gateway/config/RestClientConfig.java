package com.orion.api_gateway.config;

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
     * that Spring Cloud Netflix Eureka's auto-configuration picks this one up
     * for its own internal HTTP calls (e.g., registering with the Eureka server).
     *
     * Eureka resolves real hostnames (localhost:8000), so it MUST NOT go through
     * the load-balancer interceptor.
     */
    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    /**
     * Load-balanced RestClient.Builder — injected explicitly into service clients
     * (OrganizationServiceClient, UserServiceClient) via @Qualifier so they can
     * resolve logical service names like "http://user-service" via Eureka + LB.
     */
    @Bean
    @LoadBalanced
    @Qualifier("loadBalanced")
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}
