package com.hms.ui;

import com.formdev.flatlaf.FlatLightLaf;

public class MainAppFrameTest {
    public static void main(String[] args) {
        System.out.println("[Test] Starting MainAppFrame verification...");
        try {
            FlatLightLaf.setup();
            MainAppFrame frame = new MainAppFrame();
            System.out.println("[Test] SUCCESS: Frame created with title -> " + frame.getTitle());
            System.out.println("[Test] Width: " + frame.getWidth() + ", Height: " + frame.getHeight());
            frame.dispose();
            System.out.println("[Test] Frame disposed cleanly.");
            System.exit(0);
        } catch (Throwable t) {
            System.err.println("[Test] FAILED: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }
}
