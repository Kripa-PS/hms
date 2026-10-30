package com.hms.util;

/**
 * BCrypt Utility
 * 
 * Thin, safe wrapper around org.mindrot.jbcrypt.BCrypt.
 * Handles null checks and provides clean static methods for password hashing and verification.
 */
public class BCrypt {

    public static String hashpw(String password, String salt) {
        return org.mindrot.jbcrypt.BCrypt.hashpw(password, salt);
    }

    public static String gensalt(int logRounds) {
        return org.mindrot.jbcrypt.BCrypt.gensalt(logRounds);
    }

    public static String gensalt() {
        return org.mindrot.jbcrypt.BCrypt.gensalt();
    }

    public static boolean checkpw(String plaintext, String hashed) {
        if (plaintext == null || hashed == null || !hashed.startsWith("$2a$")) {
            return false;
        }
        try {
            return org.mindrot.jbcrypt.BCrypt.checkpw(plaintext, hashed);
        } catch (Exception e) {
            return false;
        }
    }
}
