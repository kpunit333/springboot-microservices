package orion.notification_service.service;

public interface EmailService {

    /**
     * Send an email with the specified recipient, subject, and body content.
     *
     * @param to recipient email address
     * @param subject email subject
     * @param body email body text
     */
    void sendEmail(String to, String subject, String body);
}

