package com.hms.util;

import java.util.regex.Pattern;

/**
 * ValidationUtil
 * 
 * Centralized, robust input validation for Email addresses, Phone numbers,
 * and Identity Proof documents across the Hotel Management System.
 */
public class ValidationUtil {

    // RFC 5322 compatible email pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    // Document validation patterns
    private static final Pattern AADHAAR_CLEAN_PATTERN = Pattern.compile("^\\d{12}$");
    private static final Pattern PASSPORT_PATTERN = Pattern.compile("^[A-Za-z][0-9]{7}$");
    private static final Pattern DRIVING_LICENSE_PATTERN = Pattern.compile("^[A-Z0-9-]{10,20}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern VOTER_ID_PATTERN = Pattern.compile("^[A-Z]{3}\\d{7}$", Pattern.CASE_INSENSITIVE);

    private ValidationUtil() {}

    /**
     * Validates email address format and rules.
     * 
     * @param email Email address string
     * @param mandatory Whether email is mandatory (cannot be empty)
     * @return ValidationResult containing status and descriptive message
     */
    public static ValidationResult validateEmail(String email, boolean mandatory) {
        if (email == null || email.trim().isEmpty()) {
            if (mandatory) {
                return ValidationResult.error("Email address is required.");
            }
            return ValidationResult.success();
        }

        String trimmed = email.trim();

        // Check for interior whitespace
        if (trimmed.contains(" ") || trimmed.contains("\t") || trimmed.contains("\n")) {
            return ValidationResult.error("Email address cannot contain whitespace.");
        }

        // Check basic components
        if (!trimmed.contains("@")) {
            return ValidationResult.error("Email must contain an '@' symbol.");
        }

        String[] parts = trimmed.split("@", -1);
        if (parts.length != 2) {
            return ValidationResult.error("Email must contain exactly one '@' symbol.");
        }

        String localPart = parts[0];
        String domainPart = parts[1];

        if (localPart.isEmpty()) {
            return ValidationResult.error("Email is missing the username before '@'.");
        }

        if (domainPart.isEmpty() || !domainPart.contains(".")) {
            return ValidationResult.error("Email must contain a valid domain (e.g., example.com).");
        }

        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            return ValidationResult.error("Invalid email address format (expected format: name@domain.com).");
        }

        return ValidationResult.success();
    }

    /**
     * Quick boolean check for email validity.
     */
    public static boolean isValidEmail(String email) {
        return validateEmail(email, true).isValid();
    }

    /**
     * Validates identity document numbers based on document type.
     * 
     * @param docType "Aadhaar", "Passport", "Driving License", "Voter ID"
     * @param docNumber Document number as String (to preserve leading zeros)
     * @param mandatory Whether document is mandatory
     * @return ValidationResult
     */
    public static ValidationResult validateIdProof(String docType, String docNumber, boolean mandatory) {
        if (docNumber == null || docNumber.trim().isEmpty()) {
            if (mandatory) {
                return ValidationResult.error("ID proof number is required.");
            }
            return ValidationResult.success();
        }

        String trimmed = docNumber.trim();
        String type = (docType != null) ? docType.trim() : "Aadhaar";

        if ("Aadhaar".equalsIgnoreCase(type)) {
            // Strip harmless hyphens/spaces for 12-digit check
            String cleanAadhaar = trimmed.replace("-", "").replace(" ", "");
            if (!cleanAadhaar.matches("\\d+")) {
                return ValidationResult.error("Aadhaar number must contain only numeric digits (no alphabetic characters).");
            }
            if (!AADHAAR_CLEAN_PATTERN.matcher(cleanAadhaar).matches()) {
                return ValidationResult.error("Aadhaar number must be exactly 12 digits (e.g. 1234-5678-9012).");
            }
            return ValidationResult.success();
        } else if ("Passport".equalsIgnoreCase(type)) {
            String clean = trimmed.toUpperCase().replace(" ", "");
            if (clean.length() != 8) {
                return ValidationResult.error("Passport number must be exactly 8 characters.");
            }
            if (!PASSPORT_PATTERN.matcher(clean).matches()) {
                return ValidationResult.error("Passport must start with an uppercase letter followed by 7 digits (e.g. P1234567).");
            }
            return ValidationResult.success();
        } else if ("Driving License".equalsIgnoreCase(type) || "Driving Licence".equalsIgnoreCase(type)) {
            String clean = trimmed.toUpperCase().replace(" ", "");
            if (clean.length() < 10 || clean.length() > 20) {
                return ValidationResult.error("Driving License number must be between 10 and 20 alphanumeric characters.");
            }
            if (!DRIVING_LICENSE_PATTERN.matcher(clean).matches()) {
                return ValidationResult.error("Driving License contains invalid characters (e.g. DL-0420110012345).");
            }
            return ValidationResult.success();
        } else if ("Voter ID".equalsIgnoreCase(type)) {
            String clean = trimmed.toUpperCase().replace(" ", "");
            if (clean.length() != 10) {
                return ValidationResult.error("Voter ID must be exactly 10 alphanumeric characters (3 letters + 7 digits).");
            }
            if (!VOTER_ID_PATTERN.matcher(clean).matches()) {
                return ValidationResult.error("Voter ID must consist of 3 letters followed by 7 digits (e.g. ABC1234567).");
            }
            return ValidationResult.success();
        }

        // Generic fallback for any other document type
        if (trimmed.length() < 4) {
            return ValidationResult.error("ID proof number must be at least 4 characters.");
        }
        return ValidationResult.success();
    }

    /**
     * Validates phone number (10 digits standard).
     */
    public static ValidationResult validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return ValidationResult.error("Phone number is required.");
        }
        String clean = phone.trim().replace("-", "").replace(" ", "").replace("+91", "");
        if (!clean.matches("\\d{10}")) {
            return ValidationResult.error("Phone number must be exactly 10 digits.");
        }
        return ValidationResult.success();
    }

    /**
     * Validation result record
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
