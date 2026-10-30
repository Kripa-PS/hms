package com.hms.util;

import com.hms.util.ValidationUtil.ValidationResult;

/**
 * ValidationTest
 * 
 * Comprehensive automated tests for Email and ID Proof validation rules.
 */
public class ValidationTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  Phase 4: Email & ID Proof Validation Test Suite");
        System.out.println("=================================================");

        int passed = 0;
        int failed = 0;

        // Test 1: Valid Emails
        System.out.print("[Test 1] Valid Email Formats Accepted... ");
        String[] validEmails = {
            "guest@example.com",
            "customer.name@gmail.com",
            "first_last@company.org",
            "user+tag@domain.co.in",
            "   padded@domain.com   " // surrounding whitespace should be trimmed
        };

        boolean allValidAccepted = true;
        for (String email : validEmails) {
            ValidationResult res = ValidationUtil.validateEmail(email, true);
            if (!res.isValid()) {
                allValidAccepted = false;
                System.out.println("\n  FAILED on: " + email + " -> " + res.getMessage());
            }
        }
        if (allValidAccepted) {
            System.out.println("PASSED (All 5 valid formats accepted)");
            passed++;
        } else {
            failed++;
        }

        // Test 2: Invalid Emails Rejected
        System.out.print("[Test 2] Malformed Emails Rejected with Clear Messages... ");
        String[] invalidEmails = {
            "guestexample.com",     // Missing @
            "guest@",              // Missing domain
            "@gmail.com",           // Missing local part
            "guest @example.com",   // Space inside local part
            "guest@ example.com",   // Space inside domain
            "guest@domain",         // Missing TLD
            "",                     // Empty when mandatory
            "   "                   // Blank when mandatory
        };

        boolean allInvalidRejected = true;
        for (String email : invalidEmails) {
            ValidationResult res = ValidationUtil.validateEmail(email, true);
            if (res.isValid()) {
                allInvalidRejected = false;
                System.out.println("\n  FAILED: Accepted invalid email: '" + email + "'");
            }
        }
        if (allInvalidRejected) {
            System.out.println("PASSED (All 8 malformed formats correctly blocked)");
            passed++;
        } else {
            failed++;
        }

        // Test 3: Valid Aadhaar
        System.out.print("[Test 3] Valid 12-Digit Aadhaar Formats Accepted... ");
        boolean aadhaarValid = ValidationUtil.validateIdProof("Aadhaar", "123456789012", true).isValid()
                            && ValidationUtil.validateIdProof("Aadhaar", "1234-5678-9012", true).isValid();
        if (aadhaarValid) {
            System.out.println("PASSED");
            passed++;
        } else {
            System.out.println("FAILED");
            failed++;
        }

        // Test 4: Invalid Aadhaar (Alpha, wrong length)
        System.out.print("[Test 4] Invalid Aadhaar Formats Rejected... ");
        boolean aadhaarInvalid = !ValidationUtil.validateIdProof("Aadhaar", "1234ABCD9012", true).isValid()
                              && !ValidationUtil.validateIdProof("Aadhaar", "12345678901", true).isValid()   // 11 digits
                              && !ValidationUtil.validateIdProof("Aadhaar", "1234567890123", true).isValid(); // 13 digits
        if (aadhaarInvalid) {
            System.out.println("PASSED");
            passed++;
        } else {
            System.out.println("FAILED");
            failed++;
        }

        // Test 5: Valid and Invalid Passport
        System.out.print("[Test 5] Passport Format Validation (Letter + 7 Digits)... ");
        boolean passportCheck = ValidationUtil.validateIdProof("Passport", "A1234567", true).isValid()
                             && ValidationUtil.validateIdProof("Passport", "Z9876543", true).isValid()
                             && !ValidationUtil.validateIdProof("Passport", "12345678", true).isValid()  // missing leading letter
                             && !ValidationUtil.validateIdProof("Passport", "A123456", true).isValid();  // only 6 digits
        if (passportCheck) {
            System.out.println("PASSED");
            passed++;
        } else {
            System.out.println("FAILED");
            failed++;
        }

        // Test 6: Valid and Invalid Driving License
        System.out.print("[Test 6] Driving License Format Validation... ");
        boolean dlCheck = ValidationUtil.validateIdProof("Driving License", "DL-0420110012345", true).isValid()
                       && !ValidationUtil.validateIdProof("Driving License", "DL", true).isValid(); // too short
        if (dlCheck) {
            System.out.println("PASSED");
            passed++;
        } else {
            System.out.println("FAILED");
            failed++;
        }

        // Test 7: Valid and Invalid Voter ID
        System.out.print("[Test 7] Voter ID Format Validation (3 Letters + 7 Digits)... ");
        boolean voterCheck = ValidationUtil.validateIdProof("Voter ID", "ABC1234567", true).isValid()
                          && !ValidationUtil.validateIdProof("Voter ID", "AB1234567", true).isValid(); // 2 letters only
        if (voterCheck) {
            System.out.println("PASSED");
            passed++;
        } else {
            System.out.println("FAILED");
            failed++;
        }

        // Test 8: Phone Validation
        System.out.print("[Test 8] Phone Number 10-Digit Validation... ");
        boolean phoneCheck = ValidationUtil.validatePhone("9876543210").isValid()
                          && ValidationUtil.validatePhone("+91 9876543210").isValid()
                          && !ValidationUtil.validatePhone("98765").isValid()
                          && !ValidationUtil.validatePhone("9876543210ABCD").isValid();
        if (phoneCheck) {
            System.out.println("PASSED");
            passed++;
        } else {
            System.out.println("FAILED");
            failed++;
        }

        System.out.println("-------------------------------------------------");
        System.out.println("Summary: Passed: " + passed + " / 8, Failed: " + failed);
        System.out.println("=================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
