package com.hms;

import com.formdev.flatlaf.FlatLightLaf;
import com.hms.ui.MainAppFrame;

import javax.swing.SwingUtilities;

/**
 * Main
 * 
 * Application Entry Point for Hotel Management System.
 * BTech CSE Mini-Project.
 */
public class Main {
    public static void main(String[] args) {
        // Setup modern FlatLaf theme
        try {
            FlatLightLaf.setup();
        } catch (Exception e) {
            System.err.println("[Main] Notice: Using default Swing Look and Feel (" + e.getMessage() + ")");
        }

        // Launch GUI on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            MainAppFrame app = new MainAppFrame();
            app.setVisible(true);
        });
    }
}
