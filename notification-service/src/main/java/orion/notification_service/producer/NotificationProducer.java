package orion.notification_service.producer;

import orion.notification_service.constant.Channel;
import orion.notification_service.dto.ProducerPayload;

public interface NotificationProducer {

    /**
     * Sends a notification payload to the specified delivery channel (EMAIL, SMS, WHATSAPP).
     *
     * @param channel the delivery channel (e.g. EMAIL, SMS, WHATSAPP)
     * @param payload the producer payload containing receiver, title, description, and timestamp
     */
    void send(String channel, ProducerPayload payload);

    default void sendEmail(ProducerPayload payload) {
        send(Channel.EMAIL, payload);
    }

    default void sendSms(ProducerPayload payload) {
        send(Channel.SMS, payload);
    }

    default void sendWhatsApp(ProducerPayload payload) {
        send(Channel.WHATSAPP, payload);
    }
}

