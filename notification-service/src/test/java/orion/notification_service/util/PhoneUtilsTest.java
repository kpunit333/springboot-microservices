package orion.notification_service.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PhoneUtilsTest {

    @Test
    void testFormatReceiverPhone_withCountryCodeAndLocalPhone() {
        String result = PhoneUtils.formatReceiverPhone("+91", "9876543210");
        assertEquals("+919876543210", result);
    }

    @Test
    void testFormatReceiverPhone_withCountryCodeMissingPlus() {
        String result = PhoneUtils.formatReceiverPhone("91", "9876543210");
        assertEquals("+919876543210", result);
    }

    @Test
    void testFormatReceiverPhone_whenPhoneAlreadyStartsWithPlus() {
        String result = PhoneUtils.formatReceiverPhone("+91", "+919876543210");
        assertEquals("+919876543210", result);
    }

    @Test
    void testFormatReceiverPhone_whenPhoneStartsWithCountryDigits() {
        String result = PhoneUtils.formatReceiverPhone("+91", "919876543210");
        assertEquals("+919876543210", result);
    }

    @Test
    void testFormatReceiverPhone_withNullOrBlankCountryCode() {
        assertEquals("9876543210", PhoneUtils.formatReceiverPhone(null, "9876543210"));
        assertEquals("9876543210", PhoneUtils.formatReceiverPhone("  ", "9876543210"));
    }

    @Test
    void testFormatReceiverPhone_withNullOrBlankPhone() {
        assertNull(PhoneUtils.formatReceiverPhone("+91", null));
        assertNull(PhoneUtils.formatReceiverPhone("+91", "  "));
    }
}

