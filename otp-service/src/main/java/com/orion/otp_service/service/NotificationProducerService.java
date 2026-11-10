package com.orion.otp_service.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.orion.otp_service.config.NotificationProperties;
import com.orion.otp_service.dto.OtpUserDto;
import com.orion.otp_service.util.PhoneUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Way 2: Produces notifications directly to Kafka topics for all subscribed channels,
 * exactly matching the payload format and channel routing of notification-service.
 * - TOPIC_EMAIL for EMAIL
 * - TOPIC_SMS for SMS
 * - TOPIC_WHATSAPP for WHATSAPP
 *
 * Producer payload:
 * {
 *   "receiver": "",
 *   "title": "",
 *   "description": "",
 *   "timestamp": ""
 * }
 */
@Slf4j
@Service
public class NotificationProducerService {

    private static final String KAFKA_REST_CONTENT_TYPE = "application/vnd.kafka.json.v2+json";

    private final RestClient restClient;
    private final NotificationProperties properties;

    public NotificationProducerService(RestClient.Builder restClientBuilder, NotificationProperties properties) {
        this.properties = properties;

        String auth = properties.getUsername() + ":" + properties.getPassword();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        this.restClient = restClientBuilder
                .baseUrl(properties.getUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, KAFKA_REST_CONTENT_TYPE)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.kafka.v2+json, application/json")
                .build();
    }

    /**
     * Way 2: Produces notifications for all subscribed channels with formatted receiver and payload.
     */
    public List<String> dispatchToChannels(
            List<String> channels,
            String secretKey,
            OtpUserDto user,
            String purposeId,
            String otp,
            long expiresInSeconds) {

        String title = "OTP Verification";
        long minutes = Math.max(1, expiresInSeconds / 60);
        String description = "Your OTP is " + otp + ". Valid for " + minutes + " minutes.";

        return dispatchToChannelsWithContent(channels, user, title, description);
    }

    /**
     * Produces notification payload with custom title and description to all subscribed channels.
     */
    public List<String> dispatchToChannelsWithContent(
            List<String> channels,
            OtpUserDto user,
            String title,
            String description) {

        String userId = user != null && user.getId() != null ? user.getId() : "";

        if (channels == null || channels.isEmpty()) {
            log.debug("No delivery channels provided. Skipping Kafka producer dispatch.");
            return List.of();
        }

        List<String> dispatched = new ArrayList<>();

        for (String chan : channels) {
            if (chan == null || chan.trim().isEmpty()) {
                continue;
            }

            String normalized = chan.trim().toUpperCase();
            String topic = resolveTopicForChannel(normalized);

            if (topic == null) {
                log.warn("Unrecognized delivery channel '{}' for userId='{}'. Skipping.", normalized, userId);
                continue;
            }

            String receiver = resolveReceiverForChannel(normalized, user);
            if (receiver == null || receiver.trim().isEmpty()) {
                log.warn("Missing receiver information for channel '{}', userId='{}'. Skipping.", normalized, userId);
                continue;
            }

            boolean success = publishToTopic(topic, normalized, receiver, title, description);
            if (success) {
                dispatched.add(normalized);
            }
        }

        return dispatched;
    }

    /**
     * Resolves receiver identifier based on channel type matching notification-service rules:
     * - EMAIL: user email
     * - SMS: formatted phone with country code
     * - WHATSAPP: formatted whatsapp (or phone fallback) with country code
     */
    public String resolveReceiverForChannel(String channel, OtpUserDto user) {
        if (user == null) {
            return null;
        }

        return switch (channel.trim().toUpperCase()) {
            case "EMAIL", "MAIL" -> (user.getEmail() != null && !user.getEmail().trim().isEmpty())
                    ? user.getEmail().trim()
                    : user.getId();

            case "SMS" -> (user.getPhone() != null && !user.getPhone().trim().isEmpty())
                    ? PhoneUtils.formatReceiverPhone(user.getCountryCode(), user.getPhone().trim())
                    : user.getId();

            case "WHATSAPP", "WA" -> {
                String waTarget = (user.getWhatsapp() != null && !user.getWhatsapp().trim().isEmpty())
                        ? user.getWhatsapp().trim()
                        : ((user.getPhone() != null && !user.getPhone().trim().isEmpty())
                                ? user.getPhone().trim()
                                : user.getId());
                yield PhoneUtils.formatReceiverPhone(user.getCountryCode(), waTarget);
            }

            default -> user.getId();
        };
    }

    /**
     * Resolves the Kafka topic name for a given channel name.
     */
    public String resolveTopicForChannel(String channel) {
        if (channel == null) {
            return null;
        }
        return switch (channel.trim().toUpperCase()) {
            case "SMS" -> properties.getTopicSms();
            case "EMAIL", "MAIL" -> properties.getTopicEmail();
            case "WHATSAPP", "WA" -> properties.getTopicWhatsapp();
            default -> null;
        };
    }

    private boolean publishToTopic(
            String topic,
            String channel,
            String receiver,
            String title,
            String description) {

        try {
            // Producer payload strictly: receiver, title, description, timestamp
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("receiver", receiver);
            payload.put("title", title);
            payload.put("description", description);
            payload.put("timestamp", Instant.now().toString());

            Map<String, Object> record = new LinkedHashMap<>();
            record.put("key", receiver);
            record.put("value", payload);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("records", List.of(record));

            String response = restClient.post()
                    .uri("/topics/{topic}", topic)
                    .contentType(MediaType.parseMediaType(KAFKA_REST_CONTENT_TYPE))
                    .body(body)
                    .retrieve()
                    .body(String.class);

            log.info("Pushed notification payload to Kafka topic '{}' for receiver='{}', channel='{}'. Response: {}",
                    topic, receiver, channel, response);
            return true;
        } catch (Exception e) {
            log.error("Failed to push notification payload to Kafka topic '{}' for channel '{}' (receiver='{}'): {}",
                    topic, channel, receiver, e.getMessage());
            return false;
        }
    }
}
