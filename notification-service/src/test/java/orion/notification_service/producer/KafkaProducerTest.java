package orion.notification_service.producer;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import orion.notification_service.constant.Channel;
import orion.notification_service.dto.ProducerPayload;

@ExtendWith(MockitoExtension.class)
class KafkaProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private KafkaNotificationProducer producer;

    @BeforeEach
    void setUp() {
        producer = new KafkaNotificationProducer(kafkaTemplate, "TOPIC_EMAIL", "TOPIC_SMS", "TOPIC_WHATSAPP");
    }

    @Test
    void testEmailProducer_send() {
        ProducerPayload payload = ProducerPayload.builder()
                .receiver("test@example.com")
                .title("Email Title")
                .description("Email Description")
                .timestamp("2026-11-01T00:00:00Z")
                .build();

        producer.sendEmail(payload);
        verify(kafkaTemplate).send("TOPIC_EMAIL", "test@example.com", payload);
    }

    @Test
    void testSmsProducer_send() {
        ProducerPayload payload = ProducerPayload.builder()
                .receiver("+919876543210")
                .title("SMS Title")
                .description("SMS Description")
                .timestamp("2026-11-01T00:00:00Z")
                .build();

        producer.sendSms(payload);
        verify(kafkaTemplate).send("TOPIC_SMS", "+919876543210", payload);
    }

    @Test
    void testWhatsAppProducer_send() {
        ProducerPayload payload = ProducerPayload.builder()
                .receiver("+919876543210")
                .title("WA Title")
                .description("WA Description")
                .timestamp("2026-11-01T00:00:00Z")
                .build();

        producer.sendWhatsApp(payload);
        verify(kafkaTemplate).send("TOPIC_WHATSAPP", "+919876543210", payload);
    }

    @Test
    void testGenericSend_withExplicitChannel() {
        ProducerPayload payload = ProducerPayload.builder()
                .receiver("receiver@test.com")
                .title("General Title")
                .description("General Description")
                .timestamp("2026-11-01T00:00:00Z")
                .build();

        producer.send(Channel.EMAIL, payload);
        verify(kafkaTemplate).send("TOPIC_EMAIL", "receiver@test.com", payload);
    }
}
