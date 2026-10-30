package com.hms.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.hms.model.User;
import com.hms.util.UserSession;

import javax.swing.*;

/**
 * CustomerAndLoginFrameTest
 * 
 * Verifies that LoginFrame and CustomerAppFrame can be safely constructed,
 * laid out, and disposed on the Swing Event Dispatch Thread (EDT).
 */
public class CustomerAndLoginFrameTest {

    public static void main(String[] args) throws Exception {
        System.out.println("[Test] Starting Customer & Login Frame UI verification...");

        try {
            FlatLightLaf.setup();
        } catch (Exception e) {
            // Ignore
        }

        // Test 1: LoginFrame instantiation
        SwingUtilities.invokeAndWait(() -> {
            LoginFrame loginFrame = new LoginFrame();
            assert loginFrame.getTitle().contains("Grand Horizon Hotel") : "LoginFrame title mismatch";
            assert loginFrame.getWidth() > 400 : "LoginFrame width unexpected";
            loginFrame.dispose();
            System.out.println("  [PASS] LoginFrame initialized and disposed cleanly.");
        });

        // Test 2: CustomerAppFrame instantiation with logged in customer
        SwingUtilities.invokeAndWait(() -> {
            User mockCust = new User("test_cust", "hash", "Test Customer", "CUSTOMER", 1);
            mockCust.setId(100);
            UserSession.login(mockCust);

            CustomerAppFrame customerFrame = new CustomerAppFrame();
            assert customerFrame.getTitle().contains("Guest Portal") : "CustomerAppFrame title mismatch";
            assert customerFrame.getWidth() >= 950 : "CustomerAppFrame width unexpected";
            customerFrame.dispose();

            UserSession.logout();
            System.out.println("  [PASS] CustomerAppFrame initialized and disposed cleanly.");
        });

        System.out.println("[Test] ALL UI FRAME TESTS PASSED SUCCESSFULLY!");
    }
}
