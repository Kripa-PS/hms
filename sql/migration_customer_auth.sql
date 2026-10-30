-- ============================================================================
-- HOTEL MANAGEMENT SYSTEM - CUSTOMER AUTHENTICATION & GUEST LINK MIGRATION
-- Database: hotel_management
-- Safe, non-destructive, idempotent migration script
-- ============================================================================

USE hotel_management;

-- 1. Safely extend user roles to support 'CUSTOMER'
ALTER TABLE users 
MODIFY COLUMN role ENUM('ADMIN', 'RECEPTIONIST', 'CUSTOMER') 
NOT NULL DEFAULT 'CUSTOMER';

-- 2. Safely add guest_id column and foreign key to link customer accounts to their guest profile
SET @exist_col := (SELECT COUNT(*) FROM information_schema.COLUMNS 
                   WHERE table_schema = DATABASE() 
                     AND table_name = 'users' 
                     AND column_name = 'guest_id');

SET @sqlstmt := IF(@exist_col = 0, 
    'ALTER TABLE users ADD COLUMN guest_id INT NULL, ADD CONSTRAINT fk_user_guest FOREIGN KEY (guest_id) REFERENCES guests(id) ON DELETE SET NULL, ADD INDEX idx_user_guest (guest_id);', 
    'SELECT ''Column guest_id already exists in users table''');

PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Migration completed safely. Existing users, passwords, and guest records are preserved.
