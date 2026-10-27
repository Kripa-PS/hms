-- ============================================================================
-- HOTEL MANAGEMENT SYSTEM - MEMBER 3: BILLING, PAYMENTS AND REPORTS MIGRATION
-- Database: hotel_management
-- Safe, non-destructive migration script
-- ============================================================================

USE hotel_management;

-- 1. Safely alter payments table to support 'FAILED' status for demo payments
ALTER TABLE payments 
MODIFY COLUMN payment_status ENUM('PAID', 'PARTIAL', 'PENDING', 'REFUNDED', 'FAILED') 
NOT NULL DEFAULT 'PAID';

-- 2. Add unique index on transaction_reference if not exists (prevents duplicate payments)
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics 
               WHERE table_schema = DATABASE() 
                 AND table_name = 'payments' 
                 AND index_name = 'idx_payment_txn_ref_uq');
SET @sqlstmt := IF(@exist = 0, 'CREATE UNIQUE INDEX idx_payment_txn_ref_uq ON payments (transaction_reference);', 'SELECT ''Index already exists''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. Create INVOICES table if not exists (stores generated invoice records)
CREATE TABLE IF NOT EXISTS invoices (
    id INT AUTO_INCREMENT PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    booking_id INT NOT NULL,
    issue_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10, 2) NOT NULL,
    paid_amount DECIMAL(10, 2) NOT NULL,
    balance_due DECIMAL(10, 2) NOT NULL,
    notes VARCHAR(255),
    
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE RESTRICT,
    INDEX idx_invoice_booking (booking_id),
    INDEX idx_invoice_number (invoice_number)
) ENGINE=InnoDB;
