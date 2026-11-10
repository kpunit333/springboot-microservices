package com.orion.otp_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "notification.service")
public class NotificationProperties {

    /**
     * Notification service (Aiven Kafka REST Proxy) URL.
     */
    private String url = "https://localhost:21340";

    private String host = "localhost";

    private int port = 21340;

    private String username = "username";

    private String password;

    private String topicEmail = "TOPIC_EMAIL";

    private String topicSms = "TOPIC_SMS";

    private String topicWhatsapp = "TOPIC_WHATSAPP";

    /**
     * Notification dispatch mode:
     * - "WAY_1" or "INTERNAL_API": Calls /api/notifications/internal/v1/send on notification-service
     * - "WAY_2" or "PRODUCER": Produces directly to Kafka topics (TOPIC_EMAIL, TOPIC_SMS, TOPIC_WHATSAPP)
     * - "BOTH": Dispatches using both Way 1 and Way 2
     */
    private String dispatchMode = "WAY_1";
}
