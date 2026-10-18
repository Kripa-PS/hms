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
        return fullName != null && !fullName.trim().isEmpty() 
            && phone != null && !phone.trim().isEmpty();
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
