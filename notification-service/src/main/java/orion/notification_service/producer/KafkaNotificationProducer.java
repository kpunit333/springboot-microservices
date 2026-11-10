package orion.notification_service.producer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import orion.notification_service.constant.Channel;
import orion.notification_service.dto.ProducerPayload;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KafkaNotificationProducer implements NotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topicEmail;
    private final String topicSms;
    private final String topicWhatsApp;

    public KafkaNotificationProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${notification.service.topic-email:TOPIC_EMAIL}") String topicEmail,
            @Value("${notification.service.topic-sms:TOPIC_SMS}") String topicSms,
            @Value("${notification.service.topic-whatsapp:TOPIC_WHATSAPP}") String topicWhatsApp) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicEmail = topicEmail;
        this.topicSms = topicSms;
        this.topicWhatsApp = topicWhatsApp;
    }

    @Override
    public void send(String channel, ProducerPayload payload) {
        String topic = resolveTopic(channel);
        log.info("Publishing notification to channel '{}' (topic '{}') for receiver: {}",
                channel, topic, payload.getReceiver());
        kafkaTemplate.send(topic, payload.getReceiver(), payload);
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

