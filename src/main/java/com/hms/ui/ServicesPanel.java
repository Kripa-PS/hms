package com.hms.ui;

import javax.swing.*;
import java.awt.BorderLayout;

/**
 * ServicesPanel
 *
 * The "Services" screen: one tab for Housekeeping and one for Room Service (Member 4).
 */
public class ServicesPanel extends JPanel {

    private final HousekeepingPanel housekeepingPanel = new HousekeepingPanel();
    private final RoomServicePanel roomServicePanel = new RoomServicePanel();

    public ServicesPanel() {
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Housekeeping", housekeepingPanel);
        tabs.addTab("Room Service", roomServicePanel);
        add(tabs, BorderLayout.CENTER);
    }

    /** Reloads both tabs from the database. Called each time the Services screen is opened. */
    public void refresh() {
        housekeepingPanel.refresh();
        roomServicePanel.refresh();
    }
}
