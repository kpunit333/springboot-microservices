package com.orion.otp_service.util;

public final class PhoneUtils {

    private PhoneUtils() {
    }

    /**
     * Formats phone number by combining countryCode and phone number cleanly.
     * Prevents duplication of country code prefix.
     */
    public static String formatReceiverPhone(String countryCode, String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        String cleanedPhone = phone.trim();
        if (countryCode == null || countryCode.trim().isEmpty()) {
            return cleanedPhone;
        }

        String cleanedCode = countryCode.trim();
        if (!cleanedCode.startsWith("+")) {
            cleanedCode = "+" + cleanedCode;
        }

        // If phone already starts with '+', keep as is
        if (cleanedPhone.startsWith("+")) {
            return cleanedPhone;
        }

        // If phone already starts with country code without '+', prepend '+'
        String codeWithoutPlus = cleanedCode.substring(1);
        if (cleanedPhone.startsWith(codeWithoutPlus)) {
            return "+" + cleanedPhone;
        }

        return cleanedCode + cleanedPhone;
    }
}
