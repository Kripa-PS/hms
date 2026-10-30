package com.hms.util;

import com.hms.model.User;

/**
 * UserSession
 * 
 * Thread-safe global session manager for the active user session.
 * Tracks logged-in user state and enforces backend role permissions.
 */
public class UserSession {

    private static volatile User currentUser = null;

    private UserSession() {}

    /**
     * Sets the active authenticated user session.
     */
    public static synchronized void login(User user) {
        currentUser = user;
    }

    /**
     * Clears the active session upon logout.
     */
    public static synchronized void logout() {
        currentUser = null;
    }

    /**
     * Retrieves the active user.
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * Checks if a user is currently logged in.
     */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Checks if the active user has ADMIN role.
     */
    public static boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    /**
     * Checks if the active user has CUSTOMER role.
     */
    public static boolean isCustomer() {
        return currentUser != null && currentUser.isCustomer();
    }

    /**
     * Gets the associated Guest ID for the active customer.
     * Returns null for staff users or unauthenticated state.
     */
    public static Integer getCurrentGuestId() {
        return (currentUser != null) ? currentUser.getGuestId() : null;
    }

    /**
     * Enforces that an operation requires Administrator privileges.
     * Throws SecurityException if user is not an Admin.
     */
    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new SecurityException("Access Denied: Administrative privileges required.");
        }
    }

    /**
     * Enforces that a customer can only access their own data.
     * If user is customer, guestId must match their session's guestId.
     * Admins and staff are permitted to access any guest record.
     */
    public static void enforceGuestAccess(int targetGuestId) {
        if (currentUser != null && currentUser.isCustomer()) {
            Integer myGuestId = currentUser.getGuestId();
            if (myGuestId == null || myGuestId != targetGuestId) {
                throw new SecurityException("Access Denied: You cannot view or modify another customer's records.");
            }
        }
    }
}
