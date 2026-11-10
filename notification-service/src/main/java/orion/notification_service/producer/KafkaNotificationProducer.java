package orion.notification_service.producer;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;
import orion.notification_service.constant.Channel;
import orion.notification_service.dto.ProducerPayload;

@Slf4j
@Component
public class KafkaNotificationProducer implements NotificationProducer {

    private static final String KAFKA_REST_CONTENT_TYPE = "application/vnd.kafka.json.v2+json";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestClient restClient;
    private final String kafkaRestUrl;
    private final String topicEmail;
    private final String topicSms;
    private final String topicWhatsApp;

    @Autowired
    public KafkaNotificationProducer(
            Optional<KafkaTemplate<String, Object>> kafkaTemplateOpt,
            Optional<RestClient.Builder> restClientBuilderOpt,
            @Value("${notification.service.url:}") String kafkaRestUrl,
            @Value("${notification.service.username:}") String username,
            @Value("${notification.service.password:}") String password,
            @Value("${notification.service.topic-email:TOPIC_EMAIL}") String topicEmail,
            @Value("${notification.service.topic-sms:TOPIC_SMS}") String topicSms,
            @Value("${notification.service.topic-whatsapp:TOPIC_WHATSAPP}") String topicWhatsApp) {

        this.kafkaTemplate = kafkaTemplateOpt.orElse(null);
        this.kafkaRestUrl = kafkaRestUrl != null ? kafkaRestUrl.trim() : "";
        this.topicEmail = topicEmail;
        this.topicSms = topicSms;
        this.topicWhatsApp = topicWhatsApp;

        if (!this.kafkaRestUrl.isEmpty()
                && (this.kafkaRestUrl.startsWith("http://") || this.kafkaRestUrl.startsWith("https://"))) {
            RestClient.Builder builder = restClientBuilderOpt.orElseGet(RestClient::builder);
            builder.baseUrl(this.kafkaRestUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, KAFKA_REST_CONTENT_TYPE)
                    .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.kafka.v2+json, application/json");

            if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
                String auth = username.trim() + ":" + password.trim();
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
            }
            this.restClient = builder.build();
            log.info("KafkaNotificationProducer configured for Aiven Kafka REST API at {}", this.kafkaRestUrl);
        } else {
            this.restClient = null;
        }
    }

    // Convenience constructor for unit testing with KafkaTemplate
    public KafkaNotificationProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            String topicEmail,
            String topicSms,
            String topicWhatsApp) {
        this(Optional.ofNullable(kafkaTemplate), Optional.empty(), null, null, null, topicEmail, topicSms,
                topicWhatsApp);
    }

    @Override
    public void send(String channel, ProducerPayload payload) {
        String topic = resolveTopic(channel);
        log.info("Publishing notification to channel '{}' (topic '{}') for receiver: {}",
                channel, topic, payload.getReceiver());

        if (restClient != null) {
            publishViaRest(topic, channel, payload);
        } else if (kafkaTemplate != null) {
            kafkaTemplate.send(topic, payload.getReceiver(), payload);
        } else {
            log.error("No Kafka transport configured for channel '{}' (topic '{}')", channel, topic);
            throw new IllegalStateException(
                    "No Kafka transport configured (neither Kafka REST nor KafkaTemplate available)");
        }
    }

    private void publishViaRest(String topic, String channel, ProducerPayload payload) {
        try {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("key", payload.getReceiver());
            record.put("value", payload);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("records", List.of(record));

            String response = restClient.post()
                    .uri("/topics/{topic}", topic)
                    .contentType(MediaType.parseMediaType(KAFKA_REST_CONTENT_TYPE))
                    .body(body)
                    .retrieve()
                    .body(String.class);

            log.info(
                    "Successfully published notification via Kafka REST to topic '{}' for receiver='{}', channel='{}'. Response: {}",
                    topic, payload.getReceiver(), channel, response);
        } catch (Exception e) {
            log.error("Failed to publish notification via Kafka REST to topic '{}' for channel '{}' (receiver: {}): {}",
                    topic, channel, payload.getReceiver(), e.getMessage());
            throw new RuntimeException(
                    "Failed to dispatch notification to Kafka topic " + topic + ": " + e.getMessage(), e);
        }
    }

    private String resolveTopic(String channel) {
        if (channel == null) {
            throw new IllegalArgumentException("Channel must not be null");
        }
        return switch (channel.toUpperCase().trim()) {
            case Channel.EMAIL, "MAIL" -> topicEmail;
            case Channel.SMS -> topicSms;
            case Channel.WHATSAPP, "WA" -> topicWhatsApp;
            default -> channel;
        };
    }
}
