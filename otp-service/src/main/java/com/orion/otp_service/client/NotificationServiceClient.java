package com.orion.otp_service.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.orion.otp_service.dto.ApiResponse;
import com.orion.otp_service.dto.NotificationSendRequest;
import com.orion.otp_service.dto.NotificationSendResponse;
import com.orion.otp_service.dto.OtpUserDto;

import lombok.extern.slf4j.Slf4j;

/**
 * Way 1: HTTP Client that calls notification-service via
 * POST /api/notifications/internal/v1/send
 */
@Slf4j
@Component
public class NotificationServiceClient {

    private final RestClient restClient;

    public NotificationServiceClient(
            @Qualifier("loadBalanced") RestClient.Builder loadBalancedBuilder) {
        this.restClient = loadBalancedBuilder
                .baseUrl("http://notification-service")
                .build();
    }

    /**
     * Dispatches notification to notification-service internal endpoint.
     *
     * @param secretKey project secret key
     * @param user      recipient user details
     * @param title     notification title
     * @param message   notification description/message body
     * @return List of dispatched channels reported by notification-service
     */
    public List<String> sendNotification(String secretKey, OtpUserDto user, String title, String message) {
        return sendNotification(secretKey, user, title, message, null);
    }

    /**
     * Dispatches notification to notification-service internal endpoint.
     *
     * @param secretKey project secret key
     * @param user      recipient user details
     * @param title     notification title
     * @param message   notification description/message body
     * @param channels  target delivery channels
     * @return List of dispatched channels reported by notification-service
     */
    public List<String> sendNotification(String secretKey, OtpUserDto user, String title, String message,
            List<String> channels) {
        NotificationSendRequest request = NotificationSendRequest.builder()
                .secretKey(secretKey)
                .user(user)
                .title(title)
                .description(message)
                .channels(channels)
                .build();

        try {
            log.info("Dispatching notification via internal API (/api/notifications/internal/v1/send) for user: {}",
                    user != null ? user.getId() : "null");

            ApiResponse<NotificationSendResponse> response = restClient.post()
                    .uri("/api/notifications/internal/v1/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        log.warn("Notification-service returned client error: {}", res.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("Notification-service returned server error: {}", res.getStatusCode());
                    })
                    .body(new ParameterizedTypeReference<ApiResponse<NotificationSendResponse>>() {});

            if (response != null && response.getData() != null
                    && response.getData().getDispatchedChannels() != null) {
                List<String> dispatched = response.getData().getDispatchedChannels();
                log.info("Notification dispatched successfully via internal API to channels: {}", dispatched);
                return dispatched;
            }

            log.warn("Notification-service responded without dispatched channels list");
            return List.of();
        } catch (Exception e) {
            log.warn("Failed to dispatch notification via internal API (/api/notifications/internal/v1/send): {}",
                    e.getMessage());
            return List.of();
        }
    }
}

