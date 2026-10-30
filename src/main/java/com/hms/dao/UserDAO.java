package com.hms.dao;

import com.hms.model.User;
import java.util.Optional;

/**
 * UserDAO Interface
 * 
 * Defines operations for user authentication, customer registration,
 * role verification, and profile management.
 */
public interface UserDAO {

    /**
     * Authenticates a user against the database using secure BCrypt password verification.
     * 
     * @param username user login identifier
     * @param plaintextPassword plaintext password entered by the user
     * @return Optional containing the authenticated User, or empty if invalid
     */
    Optional<User> authenticate(String username, String plaintextPassword);

    /**
     * Registers a new customer user account and links or creates the corresponding guest record.
     * Enforces username uniqueness, password strength, phone/email validation, and ID proof validation.
     * 
     * @param username desired unique username
     * @param plaintextPassword account password
     * @param fullName full name of the customer
     * @param phone unique contact phone number
     * @param email valid email address
     * @param idProofType document type (e.g. Aadhaar, Passport)
     * @param idProofNumber document identification number
     * @param address residential address
     * @return Created User object
     * @throws DuplicateUsernameException if username is already taken
     * @throws DuplicateCustomerException if an online account already exists for this phone
     * @throws IllegalArgumentException on validation failure
     */
    User registerCustomer(String username, String plaintextPassword, String fullName, 
                         String phone, String email, String idProofType, 
                         String idProofNumber, String address);

    /**
     * Finds a user by their primary key ID.
     */
    Optional<User> findById(int id);

    /**
     * Finds a user by their unique username.
     */
    Optional<User> findByUsername(String username);

    /**
     * Updates profile details for a customer user and their linked guest profile.
     */
    boolean updateCustomerProfile(int userId, String fullName, String phone, String email, 
                                 String address, String idProofType, String idProofNumber);

    class DuplicateUsernameException extends RuntimeException {
        public DuplicateUsernameException(String message) {
            super(message);
        }
    }

    class DuplicateCustomerException extends RuntimeException {
        public DuplicateCustomerException(String message) {
            super(message);
        }
    }
}
