package com.hms.model;

import java.time.LocalDateTime;

/**
 * User Model
 * 
 * Represents an authenticated staff or customer user in the Hotel Management System.
 * Corresponds to the 'users' table in MySQL.
 */
public class User {

    private int id;
    private String username;
    private String passwordHash;
    private String fullName;
    private String role; // 'ADMIN', 'RECEPTIONIST', 'CUSTOMER'
    private Integer guestId; // Nullable; links to guests.id for CUSTOMER role
    private boolean active;
    private LocalDateTime createdAt;

    public User() {
        this.active = true;
        this.role = "CUSTOMER";
    }

    public User(int id, String username, String passwordHash, String fullName, String role, Integer guestId, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = (role != null) ? role.toUpperCase() : "CUSTOMER";
        this.guestId = guestId;
        this.active = active;
        this.createdAt = createdAt;
    }

    public User(String username, String passwordHash, String fullName, String role, Integer guestId) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = (role != null) ? role.toUpperCase() : "CUSTOMER";
        this.guestId = guestId;
        this.active = true;
    }

    // Role helpers
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isReceptionist() {
        return "RECEPTIONIST".equalsIgnoreCase(role);
    }

    public boolean isCustomer() {
        return "CUSTOMER".equalsIgnoreCase(role);
    }

    public boolean isStaff() {
        return isAdmin() || isReceptionist();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = (role != null) ? role.toUpperCase() : "CUSTOMER";
    }

    public Integer getGuestId() {
        return guestId;
    }

    public void setGuestId(Integer guestId) {
        this.guestId = guestId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User[" + username + " (" + role + "), GuestID=" + guestId + ", Active=" + active + "]";
    }
}
