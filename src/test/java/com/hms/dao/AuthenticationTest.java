package com.hms.dao;

import com.hms.model.User;
import com.hms.util.DatabaseConnection;
import com.hms.util.UserSession;

import java.util.Optional;

/**
 * AuthenticationTest
 * 
 * Comprehensive automated test suite for Authentication, Customer Registration,
 * Role Separation, and Security Enforcement.
 */
public class AuthenticationTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  Phase 2: Authentication & Security Test Suite  ");
        System.out.println("=================================================");

        if (!DatabaseConnection.testConnection()) {
            System.err.println("[ERROR] Database offline. Aborting test.");
            System.exit(1);
        }

        UserDAO userDAO = new UserDAOImpl();
        int passed = 0;
        int failed = 0;

        // Test 1: Valid Admin Login
        System.out.print("[Test 1] Valid Admin Login ('admin' / 'admin123')... ");
        Optional<User> adminOpt = userDAO.authenticate("admin", "admin123");
        if (adminOpt.isPresent() && adminOpt.get().isAdmin() && "admin".equals(adminOpt.get().getUsername())) {
            System.out.println("PASSED (User: " + adminOpt.get().getFullName() + ", Role: " + adminOpt.get().getRole() + ")");
            passed++;
        } else {
            System.out.println("FAILED (Admin login failed)");
            failed++;
        }

        // Test 2: Invalid Admin Login
        System.out.print("[Test 2] Invalid Admin Login (Wrong Password)... ");
        Optional<User> badAdmin = userDAO.authenticate("admin", "wrongpassword");
        if (badAdmin.isEmpty()) {
            System.out.println("PASSED (Correctly rejected)");
            passed++;
        } else {
            System.out.println("FAILED (Should have rejected wrong password)");
            failed++;
        }

        // Test 3: Customer Registration
        System.out.print("[Test 3] Customer Registration with Profile & Validation... ");
        long timestamp = System.currentTimeMillis();
        String custUsername = "cust_" + timestamp;
        String custPhone = "99" + (int)(Math.random() * 90000000 + 10000000);
        String custEmail = "cust" + timestamp + "@example.com";
        User regUser = null;

        try {
            regUser = userDAO.registerCustomer(
                    custUsername, "custpass123", "Amit Kumar",
                    custPhone, custEmail, "Aadhaar",
                    "1234-5678-9012", "Mumbai, Maharashtra"
            );

            if (regUser.getId() > 0 && regUser.getGuestId() != null && regUser.getGuestId() > 0 && regUser.isCustomer()) {
                System.out.println("PASSED (UserID=" + regUser.getId() + ", GuestID=" + regUser.getGuestId() + ")");
                passed++;
            } else {
                System.out.println("FAILED (Missing UserID or GuestID)");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // Test 4: Customer Login with Registered Credentials
        System.out.print("[Test 4] Valid Customer Login with BCrypt Verification... ");
        Optional<User> custLoginOpt = userDAO.authenticate(custUsername, "custpass123");
        if (custLoginOpt.isPresent() && custLoginOpt.get().isCustomer() && custLoginOpt.get().getGuestId() != null) {
            System.out.println("PASSED (Logged in as: " + custLoginOpt.get().getUsername() + ")");
            passed++;
        } else {
            System.out.println("FAILED (Customer login failed)");
            failed++;
        }

        // Test 5: Customer Login with Wrong Password
        System.out.print("[Test 5] Customer Login with Incorrect Password... ");
        Optional<User> badCust = userDAO.authenticate(custUsername, "incorrectpass");
        if (badCust.isEmpty()) {
            System.out.println("PASSED (Correctly rejected)");
            passed++;
        } else {
            System.out.println("FAILED (Should have rejected wrong password)");
            failed++;
        }

        // Test 6: Duplicate Username Prevention
        System.out.print("[Test 6] Duplicate Username Registration Prevention... ");
        try {
            userDAO.registerCustomer(custUsername, "newpass123", "Another Name", "9812345678", "other@example.com", "Aadhaar", "123456789012", "Delhi");
            System.out.println("FAILED (Should have thrown DuplicateUsernameException)");
            failed++;
        } catch (UserDAO.DuplicateUsernameException e) {
            System.out.println("PASSED (Correctly blocked: " + e.getMessage() + ")");
            passed++;
        } catch (Exception e) {
            System.out.println("FAILED (Unexpected exception: " + e.getMessage() + ")");
            failed++;
        }

        // Test 7: Duplicate Customer Account Prevention (Same Phone)
        System.out.print("[Test 7] Duplicate Customer Registration with Same Phone... ");
        try {
            userDAO.registerCustomer("other_user_" + timestamp, "somepass123", "Different Name", custPhone, "diff@example.com", "Aadhaar", "123456789012", "Delhi");
            System.out.println("FAILED (Should have thrown DuplicateCustomerException)");
            failed++;
        } catch (UserDAO.DuplicateCustomerException e) {
            System.out.println("PASSED (Correctly blocked: " + e.getMessage() + ")");
            passed++;
        } catch (Exception e) {
            System.out.println("FAILED (Unexpected exception: " + e.getMessage() + ")");
            failed++;
        }

        // Test 8: Session Management & Logout
        System.out.print("[Test 8] UserSession Login and Logout... ");
        UserSession.login(adminOpt.get());
        boolean sessionActive = UserSession.isLoggedIn() && UserSession.isAdmin();
        UserSession.logout();
        boolean sessionCleared = !UserSession.isLoggedIn() && UserSession.getCurrentUser() == null;

        if (sessionActive && sessionCleared) {
            System.out.println("PASSED");
            passed++;
        } else {
            System.out.println("FAILED (Session state mismatch)");
            failed++;
        }

        // Test 9: Role-Based Admin Enforcement
        System.out.print("[Test 9] Role-Based Admin Enforcement (Customer blocked from admin ops)... ");
        UserSession.login(regUser);
        try {
            UserSession.requireAdmin();
            System.out.println("FAILED (Should have thrown SecurityException)");
            failed++;
        } catch (SecurityException e) {
            System.out.println("PASSED (SecurityException thrown: " + e.getMessage() + ")");
            passed++;
        }

        // Test 10: Cross-Customer Data Access Restriction
        System.out.print("[Test 10] Cross-Customer Data Access Prevention... ");
        int otherGuestId = 999999;
        try {
            UserSession.enforceGuestAccess(otherGuestId);
            System.out.println("FAILED (Should have blocked cross-customer access)");
            failed++;
        } catch (SecurityException e) {
            System.out.println("PASSED (Blocked: " + e.getMessage() + ")");
            passed++;
        }

        // Clean up session
        UserSession.logout();

        System.out.println("-------------------------------------------------");
        System.out.println("Summary: Passed: " + passed + " / 10, Failed: " + failed);
        System.out.println("=================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
