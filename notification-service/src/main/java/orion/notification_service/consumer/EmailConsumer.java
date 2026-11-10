package orion.notification_service.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import orion.notification_service.dto.ProducerPayload;
import orion.notification_service.service.EmailService;

/**
 * Consumer for TOPIC_EMAIL messages.
 * Sends an email via Google SMTP with title as subject and description as body.
 */
@Slf4j
@Component
public class EmailConsumer {

    private final EmailService emailService;

    @Autowired
    public EmailConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    public void consume(ProducerPayload payload) {
        if (payload == null) {
            log.warn("[Email Consumer] Received null payload, skipping.");
            return;
        }

        String receiver = payload.getReceiver();
        String subject = payload.getTitle();
        String body = payload.getDescription();

        log.info("[Email Consumer] Received message for receiver='{}', subject='{}'", receiver, subject);
        emailService.sendEmail(receiver, subject, body);
    }
}

