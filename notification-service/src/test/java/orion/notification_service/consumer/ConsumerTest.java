package orion.notification_service.consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.anyString;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import orion.notification_service.dto.ProducerPayload;
import orion.notification_service.service.EmailService;

class ConsumerTest {

    private EmailService emailService;
    private EmailConsumer emailConsumer;
    private SmsConsumer smsConsumer;
    private WhatsAppConsumer whatsAppConsumer;

    @BeforeEach
    void setUp() {
        emailService = mock(EmailService.class);
        emailConsumer = new EmailConsumer(emailService);
        smsConsumer = new SmsConsumer();
        whatsAppConsumer = new WhatsAppConsumer();
    }

    @Test
    void testEmailConsumer_dispatchesToEmailService() {
        ProducerPayload payload = ProducerPayload.builder()
                .receiver("test@example.com")
                .title("Your OTP")
                .description("OTP is 123456")
                .build();

        emailConsumer.consume(payload);

        verify(emailService).sendEmail("test@example.com", "Your OTP", "OTP is 123456");
    }

    @Test
    void testEmailConsumer_nullPayload_doesNotThrow() {
        emailConsumer.consume(null);
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testSmsConsumer_consume_doesNotThrow() {
        ProducerPayload payload = ProducerPayload.builder()
                .receiver("+919876543210")
                .title("OTP")
                .description("Your OTP is 123456")
                .build();

        smsConsumer.consume(payload);
        smsConsumer.consume(null);
    }

    @Test
    void testWhatsAppConsumer_consume_doesNotThrow() {
        ProducerPayload payload = ProducerPayload.builder()
                .receiver("+919876543210")
                .title("OTP")
                .description("Your OTP is 123456")
                .build();

        whatsAppConsumer.consume(payload);
        whatsAppConsumer.consume(null);
    }
}

