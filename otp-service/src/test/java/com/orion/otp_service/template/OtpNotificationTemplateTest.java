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
    void testGenerateDescription_withPurposeProjectAndOtp() {
        String desc = template.generateDescription("Password Reset", "Acme Portal", "654321", 300);

        assertTrue(desc.contains("Password Reset"), "Description must contain purpose name");
        assertTrue(desc.contains("Acme Portal"), "Description must contain project name");
        assertTrue(desc.contains("654321"), "Description must contain OTP code");
        assertTrue(desc.contains("5 minutes"), "Description must contain valid expiration minutes");
        assertEquals(
                "Your one-time password (OTP) for Password Reset on Acme Portal is 654321. This verification code is valid for 5 minutes. For security reasons, please do not share this code with anyone.",
                desc);
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
    }
}

