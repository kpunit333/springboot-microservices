package orion.notification_service.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import orion.notification_service.service.EmailService;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromEmail;

    @Autowired
    public EmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:orion.services.dev@gmail.com}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.warn("Cannot send email: recipient 'to' address is blank");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to.trim());
            message.setSubject(subject != null ? subject : "Notification");
            message.setText(body != null ? body : "");

            log.info("Sending email via Google SMTP from '{}' to '{}' with subject: {}", fromEmail, to, subject);
            mailSender.send(message);
            log.info("Email successfully sent to '{}'", to);
        } catch (Exception e) {
            log.error("Failed to send email to '{}' via SMTP: {}", to, e.getMessage(), e);
        }
    }
}
