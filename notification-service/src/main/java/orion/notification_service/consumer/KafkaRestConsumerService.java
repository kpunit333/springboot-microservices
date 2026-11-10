package orion.notification_service.consumer;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import orion.notification_service.dto.ProducerPayload;

/**
 * Background consumer service that polls the Aiven Kafka REST Proxy (Karapace)
 * and dispatches incoming messages to EmailConsumer, SmsConsumer, and WhatsAppConsumer.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "notification.consumer.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaRestConsumerService implements SmartLifecycle {

    private static final String KAFKA_V2_JSON = "application/vnd.kafka.v2+json";
    private static final String KAFKA_JSON_V2 = "application/vnd.kafka.json.v2+json";

    private final EmailConsumer emailConsumer;
    private final SmsConsumer smsConsumer;
    private final WhatsAppConsumer whatsAppConsumer;
    private final ObjectMapper objectMapper;

    private final String kafkaRestUrl;
    private final String username;
    private final String password;
    private final String consumerGroup;
    private final String topicEmail;
    private final String topicSms;
    private final String topicWhatsApp;
    private final String autoOffsetReset;
    private final long pollIntervalMs;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread consumerThread;
    private RestClient restClient;
    private String currentBaseUri;

    @Autowired
    public KafkaRestConsumerService(
            EmailConsumer emailConsumer,
            SmsConsumer smsConsumer,
            WhatsAppConsumer whatsAppConsumer,
            ObjectMapper objectMapper,
            @Value("${notification.service.url:}") String kafkaRestUrl,
            @Value("${notification.service.username:}") String username,
            @Value("${notification.service.password:}") String password,
            @Value("${notification.consumer.group-id:notification-service-group}") String consumerGroup,
            @Value("${notification.service.topic-email:TOPIC_EMAIL}") String topicEmail,
            @Value("${notification.service.topic-sms:TOPIC_SMS}") String topicSms,
            @Value("${notification.service.topic-whatsapp:TOPIC_WHATSAPP}") String topicWhatsApp,
            @Value("${notification.consumer.auto-offset-reset:latest}") String autoOffsetReset,
            @Value("${notification.consumer.poll-interval-ms:2000}") long pollIntervalMs) {

        this.emailConsumer = emailConsumer;
        this.smsConsumer = smsConsumer;
        this.whatsAppConsumer = whatsAppConsumer;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.kafkaRestUrl = kafkaRestUrl != null ? kafkaRestUrl.trim() : "";
        this.username = username != null ? username.trim() : "";
        this.password = password != null ? password.trim() : "";
        this.consumerGroup = consumerGroup;
        this.topicEmail = topicEmail;
        this.topicSms = topicSms;
        this.topicWhatsApp = topicWhatsApp;
        this.autoOffsetReset = autoOffsetReset;
        this.pollIntervalMs = Math.max(500, pollIntervalMs);

        if (!this.kafkaRestUrl.isEmpty()
                && (this.kafkaRestUrl.startsWith("http://") || this.kafkaRestUrl.startsWith("https://"))) {
            RestClient.Builder builder = RestClient.builder()
                    .baseUrl(this.kafkaRestUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, KAFKA_V2_JSON)
                    .defaultHeader(HttpHeaders.ACCEPT, KAFKA_JSON_V2 + ", " + KAFKA_V2_JSON + ", application/json");

            if (!this.username.isEmpty() && !this.password.isEmpty()) {
                String auth = this.username + ":" + this.password;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
            }
            this.restClient = builder.build();
        } else {
            this.restClient = null;
        }
    }

    @Override
    public void start() {
        if (restClient == null) {
            log.info("KafkaRestConsumerService disabled (no valid notification.service.url configured)");
            return;
        }

        if (running.compareAndSet(false, true)) {
            log.info("Starting KafkaRestConsumerService for group '{}' on URL '{}'", consumerGroup, kafkaRestUrl);
            consumerThread = new Thread(this::pollLoop, "kafka-rest-consumer-thread");
            consumerThread.setDaemon(true);
            consumerThread.start();
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("Stopping KafkaRestConsumerService...");
            if (consumerThread != null) {
                consumerThread.interrupt();
            }
            deleteConsumerInstance();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private void pollLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                if (currentBaseUri == null) {
                    if (!createAndSubscribeConsumer()) {
                        sleepSafe(5000);
                        continue;
                    }
                }

                List<Map<String, Object>> records = fetchRecords();
                if (records != null && !records.isEmpty()) {
                    for (Map<String, Object> record : records) {
                        processRecord(record);
                    }
                }

                sleepSafe(pollIntervalMs);
            } catch (Exception e) {
                if (!running.get() || Thread.currentThread().isInterrupted()) {
                    break;
                }
                log.warn("Error in Kafka REST consumer loop: {}. Reconnecting in 5 seconds...", e.getMessage());
                currentBaseUri = null;
                sleepSafe(5000);
            }
        }
        deleteConsumerInstance();
    }

    @SuppressWarnings("unchecked")
    private boolean createAndSubscribeConsumer() {
        try {
            String instanceName = "consumer-" + UUID.randomUUID().toString().substring(0, 8);
            Map<String, Object> createReq = new LinkedHashMap<>();
            createReq.put("name", instanceName);
            createReq.put("format", "json");
            createReq.put("auto.offset.reset", autoOffsetReset);

            log.info("Creating Kafka REST consumer instance '{}' in group '{}'", instanceName, consumerGroup);
            String response = restClient.post()
                    .uri("/consumers/{group}", consumerGroup)
                    .contentType(MediaType.parseMediaType(KAFKA_V2_JSON))
                    .body(createReq)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> respMap = objectMapper.readValue(response, Map.class);
            this.currentBaseUri = (String) respMap.get("base_uri");
            log.info("Consumer instance created successfully. base_uri: {}", currentBaseUri);

            // Subscribe to topics
            List<String> topics = List.of(topicEmail, topicSms, topicWhatsApp);
            Map<String, Object> subReq = Collections.singletonMap("topics", topics);

            createInstanceClient().post()
                    .uri("/subscription")
                    .contentType(MediaType.parseMediaType(KAFKA_V2_JSON))
                    .body(subReq)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Consumer subscribed successfully to topics: {}", topics);
            return true;
        } catch (Exception e) {
            log.error("Failed to create/subscribe Kafka REST consumer: {}", e.getMessage());
            this.currentBaseUri = null;
            return false;
        }
    }

    private List<Map<String, Object>> fetchRecords() {
        if (currentBaseUri == null) {
            return Collections.emptyList();
        }

        try {
            RestClient baseClient = createInstanceClient();
            String response = baseClient.get()
                    .uri("/records?timeout=3000")
                    .header(HttpHeaders.ACCEPT, KAFKA_JSON_V2)
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank() || response.equals("[]")) {
                return Collections.emptyList();
            }

            return objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("Failed fetching records from consumer base_uri: {}", e.getMessage());
            currentBaseUri = null;
            return Collections.emptyList();
        }
    }

    private void processRecord(Map<String, Object> record) {
        try {
            String topic = (String) record.get("topic");
            Object rawValue = record.get("value");
            if (topic == null || rawValue == null) {
                return;
            }

            ProducerPayload payload;
            if (rawValue instanceof Map) {
                payload = objectMapper.convertValue(rawValue, ProducerPayload.class);
            } else if (rawValue instanceof String strVal) {
                payload = objectMapper.readValue(strVal, ProducerPayload.class);
            } else {
                log.warn("Unrecognized message value type: {}", rawValue.getClass());
                return;
            }

            log.info("Processing message from topic '{}' for receiver: {}", topic, payload.getReceiver());

            if (topic.equalsIgnoreCase(topicEmail)) {
                emailConsumer.consume(payload);
            } else if (topic.equalsIgnoreCase(topicSms)) {
                smsConsumer.consume(payload);
            } else if (topic.equalsIgnoreCase(topicWhatsApp)) {
                whatsAppConsumer.consume(payload);
            } else {
                log.debug("Message received for unhandled topic: {}", topic);
            }
        } catch (Exception e) {
            log.error("Error processing record: {}", e.getMessage(), e);
        }
    }

    private void deleteConsumerInstance() {
        if (currentBaseUri != null) {
            try {
                log.info("Deleting consumer instance at {}", currentBaseUri);
                createInstanceClient().delete()
                        .retrieve()
                        .toBodilessEntity();
            } catch (Exception e) {
                log.debug("Error while deleting consumer instance (harmless on exit): {}", e.getMessage());
            } finally {
                currentBaseUri = null;
            }
        }
    }

    private RestClient createInstanceClient() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(currentBaseUri)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, KAFKA_V2_JSON)
                .defaultHeader(HttpHeaders.ACCEPT, KAFKA_JSON_V2 + ", " + KAFKA_V2_JSON + ", application/json");
        if (!username.isEmpty() && !password.isEmpty()) {
            String auth = username + ":" + password;
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8)));
        }
        return builder.build();
    }

    private void sleepSafe(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
