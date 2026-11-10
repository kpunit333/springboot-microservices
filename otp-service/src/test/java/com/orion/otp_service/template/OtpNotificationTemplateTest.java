package com.orion.otp_service.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OtpNotificationTemplateTest {

    private OtpNotificationTemplate template;

    @BeforeEach
    void setUp() {
        template = new OtpNotificationTemplate();
    }

    @Test
    void testGenerateTitle_withPurposeName() {
        String title = template.generateTitle("User Authentication");
        assertEquals("User Authentication - Verification Code", title);
        assertTrue(title.contains("User Authentication"));
    }

    @Test
    void testGenerateTitle_withNullOrBlank_usesDefault() {
        assertEquals("Account Verification - Verification Code", template.generateTitle(null));
        assertEquals("Account Verification - Verification Code", template.generateTitle("   "));
    }

    @Test
    void testGenerateDescription_withStructuredSpacingAndSeparation() {
        String desc = template.generateDescription("Password Reset", "Acme Portal", "654321", 300);

        // Verify purpose, project, and OTP code
        assertTrue(desc.contains("Password Reset"), "Description must contain purpose name");
        assertTrue(desc.contains("Acme Portal"), "Description must contain project name");
        assertTrue(desc.contains("654321"), "Description must contain OTP code");
        assertTrue(desc.contains("5 minutes"), "Description must contain valid expiration minutes");

        // Verify proper indentation for the OTP code (indented with 4 spaces)
        assertTrue(desc.contains("    654321"), "OTP code should be prominently indented with 4 spaces");

        // Verify body and footer separation
        assertTrue(desc.contains("---\n"), "Must contain divider separating body and footer");

        // Verify body messaging
        assertTrue(desc.startsWith("Dear User,\n\n"), "Must start with a professional greeting");
        assertTrue(desc.contains("If you did not initiate this request"),
                "Body should contain advisory for unsolicited requests");

        // Verify footer security notice and sign-off
        assertTrue(desc.contains("Security Notice: Never share this verification code with anyone."),
                "Footer must have security advisory");
        assertTrue(desc.contains("Acme Portal will never ask for your password or OTP."),
                "Footer advisory must mention project name");
        assertTrue(desc.endsWith("Best regards,\nAcme Portal Team"),
                "Footer must conclude with professional team sign-off");
    }

    @Test
    void testGenerateBodyAndFooterSeparately() {
        String body = template.generateBody("Two-Factor Auth", "Orion Gateway", "888999", 120);
        String footer = template.generateFooter("Orion Gateway");

        assertTrue(body.contains("Two-Factor Auth"));
        assertTrue(body.contains("Orion Gateway"));
        assertTrue(body.contains("888999"));
        assertTrue(body.contains("2 minutes"));

        assertTrue(footer.startsWith("---\n"));
        assertTrue(footer.contains("Orion Gateway Team"));
    }

    @Test
    void testGenerateDescription_singularMinute() {
        String desc = template.generateDescription("Login", "MyProject", "123456", 60);

        assertTrue(desc.contains("1 minute"));
        assertTrue(!desc.contains("1 minutes"));
    }

    @Test
    void testGenerateDescription_withNulls_usesDefaults() {
        String desc = template.generateDescription(null, null, "999888", 300);

        assertTrue(desc.contains("Account Verification"));
        assertTrue(desc.contains("Orion"));
        assertTrue(desc.contains("999888"));
        assertTrue(desc.contains("Orion Team"));
    }
}
