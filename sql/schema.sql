-- ============================================================================
-- HOTEL MANAGEMENT SYSTEM - SHARED DATABASE SCHEMA
-- Database: hotel_management
-- Architecture: 4-Member BTech CSE Mini-Project (Java Swing + JDBC + MySQL)
-- ============================================================================

CREATE DATABASE IF NOT EXISTS hotel_management;
USE hotel_management;

-- 1. USERS (Authentication for Admin, Receptionist, and Customers)
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'RECEPTIONIST', 'CUSTOMER') NOT NULL DEFAULT 'CUSTOMER',
    guest_id INT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_guest (guest_id)
) ENGINE=InnoDB;

-- 2. ROOMS (Maintained by Teammate: Room Management)
CREATE TABLE IF NOT EXISTS rooms (
    id INT AUTO_INCREMENT PRIMARY KEY,
    room_number VARCHAR(10) NOT NULL UNIQUE,
    room_type ENUM('SINGLE', 'DOUBLE', 'DELUXE', 'SUITE') NOT NULL,
    capacity INT NOT NULL DEFAULT 2,
    price_per_night DECIMAL(10, 2) NOT NULL,
    amenities VARCHAR(255) DEFAULT 'WiFi, TV, AC',
    operational_status ENUM('AVAILABLE', 'OCCUPIED', 'MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_room_type (room_type),
    INDEX idx_room_status (operational_status)
) ENGINE=InnoDB;

-- 3. GUESTS (Your Module: Guest Management)
CREATE TABLE IF NOT EXISTS guests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(100),
    id_proof_type VARCHAR(50) DEFAULT 'Aadhaar',
    id_proof_number VARCHAR(50),
    address VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_guest_phone (phone),
    INDEX idx_guest_name (full_name)
) ENGINE=InnoDB;

-- 4. BOOKINGS (Your Module: Booking Management)
CREATE TABLE IF NOT EXISTS bookings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    booking_reference VARCHAR(20) NOT NULL UNIQUE,
    guest_id INT NOT NULL,
    room_id INT NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    guest_count INT NOT NULL DEFAULT 1,
    status ENUM('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
    total_amount DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP NULL,
    
    FOREIGN KEY (guest_id) REFERENCES guests(id) ON DELETE RESTRICT,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE RESTRICT,
    
    INDEX idx_booking_ref (booking_reference),
    INDEX idx_booking_dates (check_in_date, check_out_date),
    INDEX idx_booking_status (status)
) ENGINE=InnoDB;

-- 5. ROOM HOLDS (Your Module: 5-Minute Ghost-Inventory Prevention)
CREATE TABLE IF NOT EXISTS room_holds (
    id INT AUTO_INCREMENT PRIMARY KEY,
    hold_token VARCHAR(64) NOT NULL UNIQUE,
    room_id INT NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    status ENUM('ACTIVE', 'CONSUMED', 'CANCELLED', 'EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    INDEX idx_hold_token (hold_token),
    INDEX idx_hold_expiry (expires_at, status)
) ENGINE=InnoDB;

-- 6. PAYMENTS (Maintained by Teammate: Billing and Reports)
CREATE TABLE IF NOT EXISTS payments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method ENUM('CASH', 'CARD', 'UPI') NOT NULL DEFAULT 'CASH',
    payment_status ENUM('PAID', 'PARTIAL', 'PENDING', 'REFUNDED', 'FAILED') NOT NULL DEFAULT 'PAID',
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    transaction_reference VARCHAR(50),
    notes VARCHAR(255),
    
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE RESTRICT,
    INDEX idx_payment_booking (booking_id),
    INDEX idx_payment_txn_ref (transaction_reference)
) ENGINE=InnoDB;

-- 6b. INVOICES (Maintained by Teammate: Billing and Reports)
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

-- 7. HOUSEKEEPING (Maintained by Teammate: Services)
CREATE TABLE IF NOT EXISTS housekeeping (
    id INT AUTO_INCREMENT PRIMARY KEY,
    room_id INT NOT NULL,
    task_description VARCHAR(255) NOT NULL,
    assigned_staff VARCHAR(100),
    status ENUM('PENDING', 'IN_PROGRESS', 'COMPLETED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    INDEX idx_housekeeping_room (room_id),
    INDEX idx_housekeeping_status (status)
) ENGINE=InnoDB;

-- 8. ROOM SERVICE (Maintained by Teammate: Services)
CREATE TABLE IF NOT EXISTS room_service (
    id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL,
    room_id INT NOT NULL,
    service_item VARCHAR(100) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(10, 2) NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    status ENUM('REQUESTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'REQUESTED',
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    INDEX idx_service_booking (booking_id)
) ENGINE=InnoDB;
