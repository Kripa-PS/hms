package com.hms.model;

import java.math.BigDecimal;

/**
 * RoomServiceMenuItem
 * 
 * Represents an available dining or beverage item in the hotel's room service menu.
 */
public class RoomServiceMenuItem {

    private final String id;
    private final String name;
    private final String category;
    private final String description;
    private final BigDecimal price;
    private final boolean available;

    public RoomServiceMenuItem(String id, String name, String category, String description, BigDecimal price, boolean available) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.available = available;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isAvailable() {
        return available;
    }

    @Override
    public String toString() {
        return name + " (Rs. " + price.toPlainString() + ")";
    }
}
