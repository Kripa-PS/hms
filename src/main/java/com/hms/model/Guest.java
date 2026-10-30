package com.hms.model;

import java.time.LocalDateTime;

/**
 * Guest Model
 * 
 * Represents a customer or hotel guest record.
 * Corresponds to the 'guests' table in the MySQL database.
 * Demonstrates OOP Encapsulation (private fields, public getters and setters).
 */
public class Guest {

    private int id;
    private String fullName;
    private String phone;
    private String email;
    private String idProofType;
    private String idProofNumber;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Default constructor
    public Guest() {}

    // Constructor for creating a new guest (before database ID assignment)
    public Guest(String fullName, String phone, String email, String idProofType, String idProofNumber, String address) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.idProofType = (idProofType != null && !idProofType.isBlank()) ? idProofType : "Aadhaar";
        this.idProofNumber = idProofNumber;
        this.address = address;
    }

    // Full constructor (for reading existing records from MySQL)
    public Guest(int id, String fullName, String phone, String email, String idProofType, 
                 String idProofNumber, String address, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.idProofType = idProofType;
        this.idProofNumber = idProofNumber;
        this.address = address;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Validation helper
    public boolean isValid() {
        if (fullName == null || fullName.trim().isEmpty() || phone == null || phone.trim().isEmpty()) {
            return false;
        }
        if (!com.hms.util.ValidationUtil.validatePhone(phone).isValid()) {
            return false;
        }
        if (email != null && !email.trim().isEmpty() && !com.hms.util.ValidationUtil.validateEmail(email, false).isValid()) {
            return false;
        }
        if (idProofNumber != null && !idProofNumber.trim().isEmpty() && !com.hms.util.ValidationUtil.validateIdProof(idProofType, idProofNumber, false).isValid()) {
            return false;
        }
        return true;
    }

    /**
     * Validates guest model throwing IllegalArgumentException on failure.
     */
    public void validate() {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full Name is required.");
        }
        com.hms.util.ValidationUtil.ValidationResult phoneRes = com.hms.util.ValidationUtil.validatePhone(phone);
        if (!phoneRes.isValid()) {
            throw new IllegalArgumentException(phoneRes.getMessage());
        }
        if (email != null && !email.trim().isEmpty()) {
            com.hms.util.ValidationUtil.ValidationResult emailRes = com.hms.util.ValidationUtil.validateEmail(email, false);
            if (!emailRes.isValid()) {
                throw new IllegalArgumentException(emailRes.getMessage());
            }
        }
        if (idProofNumber != null && !idProofNumber.trim().isEmpty()) {
            com.hms.util.ValidationUtil.ValidationResult idRes = com.hms.util.ValidationUtil.validateIdProof(idProofType, idProofNumber, false);
            if (!idRes.isValid()) {
                throw new IllegalArgumentException(idRes.getMessage());
            }
        }
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIdProofType() {
        return idProofType;
    }

    public void setIdProofType(String idProofType) {
        this.idProofType = idProofType;
    }

    public String getIdProofNumber() {
        return idProofNumber;
    }

    public void setIdProofNumber(String idProofNumber) {
        this.idProofNumber = idProofNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return fullName + " (" + phone + ")";
    }
}
