package com.hms;

import com.formdev.flatlaf.FlatLightLaf;
import com.hms.ui.LoginFrame;

import javax.swing.SwingUtilities;

/**
 * Main
 * 
 * Application Entry Point for Hotel Management System.
 * Launches the Authentication & Registration window (LoginFrame).
 */
public class Main {
    public static void main(String[] args) {
        // Setup modern FlatLaf theme
        try {
            FlatLightLaf.setup();
        } catch (Exception e) {
            System.err.println("[Main] Notice: Using default Swing Look and Feel (" + e.getMessage() + ")");
        }

        // Launch Login GUI on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
