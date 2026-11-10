package com.orion.otp_service.template;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Professional English templates for OTP notification titles and descriptions.
 *
 * Guarantees that:
 * - Purpose is mentioned in the title.
 * - Purpose name, OTP, and project name are all mentioned in the description.
 */
@Component
public class OtpNotificationTemplate {

    public static final String DEFAULT_PROJECT_NAME = "Orion";
    public static final String DEFAULT_PURPOSE_NAME = "Account Verification";

    private final String defaultProjectName;
    private final String defaultPurposeName;
    private final String titleTemplate;
    private final String descriptionTemplate;

    public OtpNotificationTemplate() {
        this(DEFAULT_PROJECT_NAME,
                DEFAULT_PURPOSE_NAME,
                "%s - Verification Code",
                "Your one-time password (OTP) for %s on %s is %s. This verification code is valid for %d minute%s. For security reasons, please do not share this code with anyone.");
    }

    public OtpNotificationTemplate(
            @Value("${otp.template.default-project-name:Orion}") String defaultProjectName,
            @Value("${otp.template.default-purpose-name:Account Verification}") String defaultPurposeName,
            @Value("${otp.template.title:%s - Verification Code}") String titleTemplate,
            @Value("${otp.template.description:Your one-time password (OTP) for %s on %s is %s. This verification code is valid for %d minute%s. For security reasons, please do not share this code with anyone.}") String descriptionTemplate) {
        this.defaultProjectName = (defaultProjectName != null && !defaultProjectName.isBlank())
                ? defaultProjectName.trim()
                : DEFAULT_PROJECT_NAME;
        this.defaultPurposeName = (defaultPurposeName != null && !defaultPurposeName.isBlank())
                ? defaultPurposeName.trim()
                : DEFAULT_PURPOSE_NAME;
        this.titleTemplate = (titleTemplate != null && !titleTemplate.isBlank())
                ? titleTemplate
                : "%s - Verification Code";
        this.descriptionTemplate = (descriptionTemplate != null && !descriptionTemplate.isBlank())
                ? descriptionTemplate
                : "Your one-time password (OTP) for %s on %s is %s. This verification code is valid for %d minute%s. For security reasons, please do not share this code with anyone.";
    }

    /**
     * Generates a professional notification title mentioning the purpose name.
     *
     * Example: "User Login - Verification Code"
     *
     * @param purposeName name of the purpose (e.g. from the purpose table)
     * @return professional title in English
     */
    public String generateTitle(String purposeName) {
        String effectivePurpose = (purposeName != null && !purposeName.isBlank())
                ? purposeName.trim()
                : defaultPurposeName;
        return String.format(titleTemplate, effectivePurpose);
    }

    /**
     * Generates a professional notification description mentioning purpose name,
     * project name, and OTP code with validity period and security advisory.
     *
     * Example:
     * "Your one-time password (OTP) for User Login on Acme Portal is 123456. This verification code is valid for 5 minutes. For security reasons, please do not share this code with anyone."
     *
     * @param purposeName name of the purpose (from the purpose table)
     * @param projectName name of the project (from the project table)
     * @param otp 6-digit OTP code
     * @param ttlSeconds time-to-live in seconds
     * @return professional description in English
     */
    public String generateDescription(String purposeName, String projectName, String otp, long ttlSeconds) {
        long minutes = Math.max(1, ttlSeconds / 60);
        String effectivePurpose = (purposeName != null && !purposeName.isBlank()) ? purposeName.trim() : defaultPurposeName;
        String effectiveProject = (projectName != null && !projectName.isBlank()) ? projectName.trim() : defaultProjectName;
        String pluralSuffix = minutes == 1 ? "" : "s";
        return String.format(descriptionTemplate, effectivePurpose, effectiveProject, otp, minutes, pluralSuffix);
    }

    /**
     * Overload for fallback when purpose name is not available.
     */
    public String generateDescription(String projectName, String otp, long ttlSeconds) {
        return generateDescription(defaultPurposeName, projectName, otp, ttlSeconds);
    }

    /**
     * Overload for fallback when neither purpose nor project name is specified.
     */
    public String generateDescription(String otp, long ttlSeconds) {
        return generateDescription(defaultPurposeName, defaultProjectName, otp, ttlSeconds);
    }
}

