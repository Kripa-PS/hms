package com.hms.dao;

import com.hms.model.Guest;
import java.util.List;
import java.util.Optional;

/**
 * GuestDAO Interface
 * 
 * Defines data access operations for Guest Management.
 * Follows the DAO (Data Access Object) pattern for separation of concerns and Abstraction.
 */
public interface GuestDAO {

    /**
     * Inserts a new guest into the database.
     * Generates and sets the new database ID in the guest object.
     * 
     * @param guest Guest object containing full name, phone, etc.
     * @return true if successfully inserted, false otherwise.
     * @throws DuplicatePhoneException if a guest with this phone number already exists.
     * @throws RuntimeException for general database errors.
     */
    boolean addGuest(Guest guest);

    /**
     * Finds a guest by their unique primary key ID.
     */
    Optional<Guest> findById(int id);

    /**
     * Finds a guest by their unique phone number.
     */
    Optional<Guest> findByPhone(String phone);

    /**
     * Searches guests matching the given query string against full name OR phone number.
     */
    List<Guest> searchByNameOrPhone(String query);

    /**
     * Retrieves all guests ordered by created timestamp descending.
     */
    List<Guest> findAll();

    /**
     * Updates an existing guest's details.
     * 
     * @param guest Guest object with updated information and a valid ID.
     * @return true if successfully updated, false otherwise.
     * @throws DuplicatePhoneException if the new phone number belongs to another guest.
     */
    boolean updateGuest(Guest guest);

    /**
     * Checked exception thrown when attempting to register or update a guest
     * with a phone number that is already in use by another guest.
     */
    class DuplicatePhoneException extends RuntimeException {
        public DuplicatePhoneException(String message) {
            super(message);
        }
    }
}
