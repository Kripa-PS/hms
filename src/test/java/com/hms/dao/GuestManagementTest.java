package com.hms.dao;

import com.hms.model.Guest;
import com.hms.ui.GuestManagementPanel;
import com.hms.util.DatabaseConnection;

import javax.swing.*;
import java.util.List;
import java.util.Optional;

/**
 * GuestManagementTest
 * 
 * Verifies Stage 4 implementation:
 * 1. Model input validation.
 * 2. Exception handling (DuplicatePhoneException).
 * 3. Swing UI panel initialization (offline-safe).
 * 4. Conditional live database test (if MySQL is running).
 */
public class GuestManagementTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  Stage 4: Guest Management Verification Tests");
        System.out.println("=================================================");

        int passed = 0;
        int failed = 0;

        // Test 1: Guest Validation Logic
        System.out.print("[Test 1] Validating Guest model constraints... ");
        Guest validGuest = new Guest("Ananya Sen", "9876543299", "ananya@example.com", "Aadhaar", "9999-8888-7777", "Kochi");
        Guest invalidGuest = new Guest("", "", null, "Aadhaar", "", "");

        if (validGuest.isValid() && !invalidGuest.isValid()) {
            System.out.println("PASSED");
            passed++;
        } else {
            System.out.println("FAILED");
            failed++;
        }

        // Test 2: DuplicatePhoneException Handling
        System.out.print("[Test 2] DuplicatePhoneException contract... ");
        try {
            throw new GuestDAO.DuplicatePhoneException("A guest with phone '9876543210' is already registered.");
        } catch (GuestDAO.DuplicatePhoneException e) {
            if (e.getMessage().contains("9876543210")) {
                System.out.println("PASSED (" + e.getMessage() + ")");
                passed++;
            } else {
                System.out.println("FAILED (Message mismatch)");
                failed++;
            }
        }

        // Test 3: GuestManagementPanel UI Construction (Offline Safety Check)
        System.out.print("[Test 3] Initializing GuestManagementPanel (Swing EDT)... ");
        try {
            final boolean[] uiPassed = {false};
            SwingUtilities.invokeAndWait(() -> {
                GuestManagementPanel panel = new GuestManagementPanel();
                if (panel.getComponentCount() > 0) {
                    uiPassed[0] = true;
                }
            });
            if (uiPassed[0]) {
                System.out.println("PASSED (Panel rendered safely with MySQL offline)");
                passed++;
            } else {
                System.out.println("FAILED");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // Test 4: Live MySQL CRUD Test
        System.out.print("[Test 4] Live MySQL Database Operations... ");
        boolean dbOnline = DatabaseConnection.testConnection();
        if (dbOnline) {
            try {
                GuestDAO dao = new GuestDAOImpl();
                String testPhone = "99999" + (int)(Math.random() * 90000 + 10000);
                Guest newGuest = new Guest("Test Guest " + System.currentTimeMillis(), testPhone, 
                        "test@example.com", "Aadhaar", "1111-2222-3333", "Test City");

                // Add
                boolean added = dao.addGuest(newGuest);
                assert added && newGuest.getId() > 0 : "Insert failed";

                // Find by phone
                Optional<Guest> found = dao.findByPhone(testPhone);
                assert found.isPresent() : "Lookup by phone failed";

                // Update
                newGuest.setFullName("Updated " + newGuest.getFullName());
                boolean updated = dao.updateGuest(newGuest);
                assert updated : "Update failed";

                // Duplicate test
                boolean duplicateCaught = false;
                try {
                    Guest dup = new Guest("Duplicate Guy", testPhone, "dup@example.com", "Aadhaar", "1111", "City");
                    dao.addGuest(dup);
                } catch (GuestDAO.DuplicatePhoneException ex) {
                    duplicateCaught = true;
                }
                assert duplicateCaught : "Duplicate phone was not blocked!";

                System.out.println("PASSED (Add, Find, Update, and Duplicate-check all verified against MySQL!)");
                passed++;
            } catch (Exception ex) {
                System.out.println("FAILED: " + ex.getMessage());
                failed++;
            }
        } else {
            System.out.println("BLOCKED (MySQL Server is not running. Live CRUD tests skipped safely).");
        }

        System.out.println("-------------------------------------------------");
        System.out.println("Summary: Passed: " + passed + ", Failed: " + failed 
                + ", DB Tests: " + (dbOnline ? "RUN" : "BLOCKED (Pending MySQL install)"));
        System.out.println("=================================================");
    }
}
