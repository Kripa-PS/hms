-- ============================================================================
-- HOTEL MANAGEMENT SYSTEM - SAFE SAMPLE SEED DATA
-- Database: hotel_management
-- Idempotent inserts for demonstration and viva testing
-- ============================================================================

USE hotel_management;

-- 1. SEED ROOMS (Single, Double, Deluxe, Suite)
INSERT INTO rooms (room_number, room_type, capacity, price_per_night, amenities, operational_status)
VALUES
    ('101', 'SINGLE', 1, 1500.00, 'WiFi, TV, Single Bed', 'AVAILABLE'),
    ('102', 'SINGLE', 1, 1500.00, 'WiFi, TV, Single Bed', 'AVAILABLE'),
    ('201', 'DOUBLE', 2, 2500.00, 'WiFi, TV, AC, Queen Bed', 'AVAILABLE'),
    ('202', 'DOUBLE', 2, 2500.00, 'WiFi, TV, AC, Queen Bed', 'AVAILABLE'),
    ('203', 'DOUBLE', 2, 2500.00, 'WiFi, TV, AC, Queen Bed', 'MAINTENANCE'),
    ('301', 'DELUXE', 3, 4000.00, 'WiFi, Smart TV, AC, King Bed, Mini Bar', 'AVAILABLE'),
    ('302', 'DELUXE', 3, 4000.00, 'WiFi, Smart TV, AC, King Bed, Balcony', 'AVAILABLE'),
    ('401', 'SUITE', 4, 7500.00, 'WiFi, Smart TV, AC, Jacuzzi, Living Room, Kitchenette', 'AVAILABLE')
ON DUPLICATE KEY UPDATE operational_status = VALUES(operational_status);

-- 2. SEED STAFF USERS (Passwords hashed using standard BCrypt for 'admin123' and 'recep123')
INSERT INTO users (username, password_hash, full_name, role, active)
VALUES
    ('admin', '$2a$10$KBeUrIki0OCkSksLfwsS7uaOS5VmY3gSboCjjLRFFXjNpr3aewvLe', 'System Administrator', 'ADMIN', TRUE),
    ('receptionist', '$2a$10$5SiTf6u0O7VtZcLQOMFOYO86lZ4vCvuSrMr0UfOVM25KarEPClfWC', 'Front Desk Receptionist', 'RECEPTIONIST', TRUE)
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), full_name = VALUES(full_name);

-- 3. SEED SAMPLE GUESTS
INSERT INTO guests (full_name, phone, email, id_proof_type, id_proof_number, address)
VALUES
    ('Rahul Sharma', '9876543210', 'rahul.sharma@example.com', 'Aadhaar', '1234-5678-9012', 'Kochi, Kerala'),
    ('Priya Nair', '9876543211', 'priya.nair@example.com', 'Passport', 'P1234567', 'Ernakulam, Kerala'),
    ('Anand Verma', '9876543212', 'anand.v@example.com', 'Driving License', 'DL-0420110012345', 'Bangalore, Karnataka')
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- 4. SEED SAMPLE CONFIRMED BOOKING
-- Sample booking for room 201 (Double)
INSERT INTO bookings (booking_reference, guest_id, room_id, check_in_date, check_out_date, guest_count, status, total_amount)
VALUES
    ('HMS-2026-1001', 1, 3, '2026-10-20', '2026-10-22', 2, 'CONFIRMED', 5000.00)
ON DUPLICATE KEY UPDATE status = VALUES(status);

-- 5. SEED HOUSEKEEPING TASK FOR ROOM UNDER MAINTENANCE
INSERT INTO housekeeping (room_id, task_description, assigned_staff, status)
VALUES
    (5, 'AC repair and deep sanitation', 'Ramesh (Maintenance)', 'IN_PROGRESS')
ON DUPLICATE KEY UPDATE status = VALUES(status);
