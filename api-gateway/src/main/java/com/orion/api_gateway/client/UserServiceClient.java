package com.orion.api_gateway.client;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.orion.api_gateway.client.dto.UserInternalDto;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(@Qualifier("loadBalanced") RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("http://user-service")
                .build();
    }

    public Optional<UserInternalDto> getUserByUsername(String username) {
        try {
            UserInternalDto user = restClient.get()
                    .uri("/api/users/internal/v1/user/{username}", username)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        log.warn("User service returned {} for username: {}", response.getStatusCode(), username);
                    })
                    .body(UserInternalDto.class);

            return Optional.ofNullable(user);
        } catch (Exception e) {
            log.error("Error communicating with user-service for username '{}': {}", username, e.getMessage());
            return Optional.empty();
        }
    }
}
