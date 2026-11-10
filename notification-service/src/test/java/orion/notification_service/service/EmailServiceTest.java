package orion.notification_service.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import orion.notification_service.service.impl.EmailServiceImpl;

class EmailServiceTest {

    private JavaMailSender mailSender;
    private EmailService emailService;
    private final String fromEmail = "orion.services.dev@gmail.com";

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        emailService = new EmailServiceImpl(mailSender, fromEmail);
    }

    @Test
    void testSendEmail_success() {
        emailService.sendEmail("user@example.com", "Test Subject", "Test Body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertEquals(fromEmail, sent.getFrom());
        assertEquals("user@example.com", sent.getTo()[0]);
        assertEquals("Test Subject", sent.getSubject());
        assertEquals("Test Body", sent.getText());
    }

    @Test
    void testSendEmail_nullOrBlankRecipient_doesNotSend() {
        emailService.sendEmail(null, "Subject", "Body");
        emailService.sendEmail("   ", "Subject", "Body");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}

