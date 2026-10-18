package com.hms.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DatabaseConnection
 * 
 * Manages JDBC connectivity for the Hotel Management System.
 * Designed for BTech CSE mini-project (Stage 1).
 * 
 * Security & Best Practices:
 * 1. Reads credentials from an untracked local 'db.properties' file or environment variables.
 * 2. Never hardcodes or logs passwords.
 * 3. Gracefully handles errors so the application does not crash if MySQL is offline.
 */
public class DatabaseConnection {

    private static final String DEFAULT_URL = 
            "jdbc:mysql://localhost:3306/hotel_management?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_USER = "root";
    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static boolean initialized = false;

    // Static initialization block
    static {
        loadConfiguration();
    }

    /**
     * Loads database configuration from db.properties or environment variables.
     */
    public static synchronized void loadConfiguration() {
        Properties props = new Properties();
        boolean loaded = false;

        // 1. Try to load from project root 'db.properties'
        File localFile = new File("db.properties");
        if (localFile.exists()) {
            try (FileInputStream fis = new FileInputStream(localFile)) {
                props.load(fis);
                loaded = true;
                System.out.println("[DatabaseConnection] Loaded settings from local file: db.properties");
            } catch (IOException e) {
                System.err.println("[DatabaseConnection] Warning: Could not read local db.properties: " + e.getMessage());
            }
        }

        // 2. If not found in root, try classpath
        if (!loaded) {
            try (InputStream is = DatabaseConnection.class.getResourceAsStream("/db.properties")) {
                if (is != null) {
                    props.load(is);
                    loaded = true;
                    System.out.println("[DatabaseConnection] Loaded settings from classpath /db.properties");
                }
            } catch (IOException e) {
                // Classpath properties not present, fallback
            }
        }

        // 3. Resolve properties with environment variable fallback
        String envUrl = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        String envPass = System.getenv("DB_PASSWORD");

        dbUrl = (envUrl != null && !envUrl.isBlank()) ? envUrl : props.getProperty("db.url", DEFAULT_URL);
        dbUser = (envUser != null && !envUser.isBlank()) ? envUser : props.getProperty("db.user", DEFAULT_USER);
        dbPassword = (envPass != null) ? envPass : props.getProperty("db.password", "");

        // 4. Verify driver is available
        try {
            Class.forName(DRIVER_CLASS);
            initialized = true;
        } catch (ClassNotFoundException e) {
            System.err.println("[DatabaseConnection] ERROR: MySQL JDBC Driver class not found: " + DRIVER_CLASS);
            System.err.println("Make sure mysql-connector-j jar is present in the classpath/lib directory.");
            initialized = false;
        }
    }

    /**
     * Obtains a new JDBC connection to the MySQL database.
     * 
     * @return active java.sql.Connection
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            loadConfiguration();
            if (!initialized) {
                throw new SQLException("Database driver (" + DRIVER_CLASS + ") could not be initialized.");
            }
        }
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    /**
     * Tests whether MySQL is reachable and credentials are valid.
     * 
     * @return true if connection succeeds, false otherwise (without throwing an uncaught exception).
     */
    public static boolean testConnection() {
        System.out.println("[DatabaseConnection] Testing connection to: " + dbUrl + " as user: " + dbUser);
        try (Connection conn = getConnection()) {
            boolean valid = conn != null && !conn.isClosed();
            if (valid) {
                System.out.println("[DatabaseConnection] SUCCESS: Connected to MySQL database '" 
                        + conn.getCatalog() + "' successfully!");
            }
            return valid;
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] Connection failed: " + e.getMessage());
            explainError(e);
            return false;
        }
    }

    /**
     * Helper to explain common MySQL errors to a student/developer in friendly terms.
     */
    private static void explainError(SQLException e) {
        String msg = e.getMessage().toLowerCase();
        if (msg.contains("communications link failure") || msg.contains("connection refused")) {
            System.err.println("-> CAUSE: MySQL Server is not running on localhost:3306.");
            System.err.println("-> ACTION: Start MySQL Service in Windows Services (services.msc) or run MySQL installer.");
        } else if (msg.contains("access denied for user")) {
            System.err.println("-> CAUSE: Wrong username or password for MySQL.");
            System.err.println("-> ACTION: Check 'db.user' and 'db.password' in your 'db.properties' file.");
        } else if (msg.contains("unknown database")) {
            System.err.println("-> CAUSE: Database 'hotel_management' does not exist yet.");
            System.err.println("-> ACTION: Execute 'sql/schema.sql' in MySQL to create the database.");
        }
    }

    /**
     * Gets the configured database URL (safe to display).
     */
    public static String getDbUrl() {
        return dbUrl;
    }

    /**
     * Gets the configured database user (safe to display).
     */
    public static String getDbUser() {
        return dbUser;
    }

    /**
     * Standalone main method to test JDBC connectivity from command line.
     */
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  Hotel Management System - JDBC Connection Test");
        System.out.println("=================================================");
        boolean success = testConnection();
        if (success) {
            System.out.println("Result: READY for database operations.");
        } else {
            System.out.println("Result: BLOCKED (MySQL is unavailable). Please follow the setup instructions.");
        }
    }
}
