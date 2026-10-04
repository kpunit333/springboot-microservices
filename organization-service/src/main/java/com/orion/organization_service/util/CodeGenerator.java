package com.orion.organization_service.util;

import java.time.LocalDateTime;

public final class CodeGenerator {

    private CodeGenerator() {
        // Utility class
    }

    public static String generateOrganizationCode() {
        return generateCode("ORG", LocalDateTime.now());
    }

    public static String generateProjectCode() {
        return generateCode("PRO", LocalDateTime.now());
    }

    public static String generateCode(String prefix, LocalDateTime dateTime) {
        if (dateTime == null) {
            dateTime = LocalDateTime.now();
        }
        int date = dateTime.getDayOfMonth();
        int month = dateTime.getMonthValue();
        int year = dateTime.getYear();
        int hour = dateTime.getHour();
        char hourChar = (char) ('A' + (hour % 24));
        int minute = dateTime.getMinute();
        int second = dateTime.getSecond();
        int twoDigitMillis = (dateTime.getNano() / 1_000_000) / 10;

        return String.format("%s%02X%X%04X%c%02X%02X%02d",
                prefix, date, month, year, hourChar, minute, second, twoDigitMillis);
    }
}
