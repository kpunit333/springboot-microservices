package orion.notification_service.consumer;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import orion.notification_service.dto.ProducerPayload;

/**
 * Consumer for TOPIC_WHATSAPP messages.
 */
@Slf4j
@Component
public class WhatsAppConsumer {

    public void consume(ProducerPayload payload) {
        if (payload == null) {
            log.warn("[WhatsApp Consumer] Received null payload, skipping.");
            return;
        }

        String receiver = payload.getReceiver();
        String title = payload.getTitle();
        String description = payload.getDescription();

        log.info("[WhatsApp Consumer] Received WhatsApp message for receiver='{}', title='{}', body='{}'",
                receiver, title, description);
    }
}

