package com.orion.otp_service.template;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Professional English templates for OTP notification titles and descriptions.
 *
 * Guarantees:
 * - Purpose is mentioned in the title.
 * - Purpose name, OTP, and project name are all mentioned in the description.
 * - Proper paragraph spacing, indented OTP code, and clear separation between
 * body content and footer/security advisory messages.
 */
@Component
public class OtpNotificationTemplate {

        public static final String DEFAULT_PROJECT_NAME = "Orion";
        public static final String DEFAULT_PURPOSE_NAME = "Account Verification";
        public static final String DEFAULT_TITLE_TEMPLATE = "%s - Verification Code";
        public static final String FOOTER_DIVIDER = "---";

        private final String defaultProjectName;
        private final String defaultPurposeName;
        private final String titleTemplate;
        private final String customDescriptionTemplate;

        public OtpNotificationTemplate() {
                this(DEFAULT_PROJECT_NAME, DEFAULT_PURPOSE_NAME, DEFAULT_TITLE_TEMPLATE, null);
        }

        public OtpNotificationTemplate(
                        @Value("${otp.template.default-project-name:Orion}") String defaultProjectName,
                        @Value("${otp.template.default-purpose-name:Account Verification}") String defaultPurposeName,
                        @Value("${otp.template.title:%s - Verification Code}") String titleTemplate) {
                this(defaultProjectName, defaultPurposeName, titleTemplate, null);
        }

        public OtpNotificationTemplate(
                        String defaultProjectName,
                        String defaultPurposeName,
                        String titleTemplate,
                        String customDescriptionTemplate) {
                this.defaultProjectName = (defaultProjectName != null && !defaultProjectName.isBlank())
                                ? defaultProjectName.trim()
                                : DEFAULT_PROJECT_NAME;
                this.defaultPurposeName = (defaultPurposeName != null && !defaultPurposeName.isBlank())
                                ? defaultPurposeName.trim()
                                : DEFAULT_PURPOSE_NAME;
                this.titleTemplate = (titleTemplate != null && !titleTemplate.isBlank())
                                ? titleTemplate
                                : DEFAULT_TITLE_TEMPLATE;
                this.customDescriptionTemplate = (customDescriptionTemplate != null
                                && !customDescriptionTemplate.isBlank())
                                                ? customDescriptionTemplate
                                                : null;
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
         * Generates the body message with formal greeting, purpose, project, indented
         * OTP,
         * validity period, and request advisory.
         *
         * @param purposeName name of the purpose
         * @param projectName name of the project
         * @param otp         6-digit OTP code
         * @param ttlSeconds  time-to-live in seconds
         * @return formatted body message
         */
        public String generateBody(String purposeName, String projectName, String otp, long ttlSeconds) {
                long minutes = Math.max(1, ttlSeconds / 60);
                String effectivePurpose = (purposeName != null && !purposeName.isBlank()) ? purposeName.trim()
                                : defaultPurposeName;
                String effectiveProject = (projectName != null && !projectName.isBlank()) ? projectName.trim()
                                : defaultProjectName;
                String pluralSuffix = minutes == 1 ? "" : "s";

                return String.format(
                                "Dear User,\n\n"
                                                + "Your one-time password (OTP) for %s on %s is:\n\n"
                                                + "%s\n\n"
                                                + "This verification code is valid for %d minute%s. Please enter this code to complete your verification.\n"
                                                + "If you did not initiate this request, please disregard this message or contact support immediately.",
                                effectivePurpose, effectiveProject, otp, minutes, pluralSuffix);
        }

        /**
         * Generates the footer message with visual divider, security notice, and
         * sign-off.
         *
         * @param projectName name of the project
         * @return formatted footer message
         */
        public String generateFooter(String projectName) {
                String effectiveProject = (projectName != null && !projectName.isBlank()) ? projectName.trim()
                                : defaultProjectName;

                return String.format(
                                "%s\n" +
                                                "Security Notice: Never share this verification code with anyone. For your protection, %s will never ask for your password or OTP.\n\n"
                                                +
                                                "Best regards,\n" +
                                                "%s Team",
                                FOOTER_DIVIDER, effectiveProject, effectiveProject);
        }

        /**
         * Generates a complete professional description with structured spacing,
         * code indentation, and clean separation between body and footer.
         *
         * @param purposeName name of the purpose (from the purpose table)
         * @param projectName name of the project (from the project table)
         * @param otp         6-digit OTP code
         * @param ttlSeconds  time-to-live in seconds
         * @return professional description in English
         */
        public String generateDescription(String purposeName, String projectName, String otp, long ttlSeconds) {
                if (customDescriptionTemplate != null) {
                        long minutes = Math.max(1, ttlSeconds / 60);
                        String effectivePurpose = (purposeName != null && !purposeName.isBlank()) ? purposeName.trim()
                                        : defaultPurposeName;
                        String effectiveProject = (projectName != null && !projectName.isBlank()) ? projectName.trim()
                                        : defaultProjectName;
                        String pluralSuffix = minutes == 1 ? "" : "s";
                        return String.format(customDescriptionTemplate, effectivePurpose, effectiveProject, otp,
                                        minutes,
                                        pluralSuffix);
                }

                String body = generateBody(purposeName, projectName, otp, ttlSeconds);
                return body;
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
