package com.hms.util;

import com.hms.model.RoomServiceMenuItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RoomServiceMenu
 * 
 * Provides the curated catalog of room service items available for hotel guests to order.
 */
public class RoomServiceMenu {

    private static final List<RoomServiceMenuItem> MENU_ITEMS = new ArrayList<>();

    static {
        // Breakfast
        MENU_ITEMS.add(new RoomServiceMenuItem("BK01", "Continental Breakfast", "Breakfast",
                "Warm butter croissant, jam, fresh sliced fruits, and chilled orange juice",
                new BigDecimal("250.00"), true));
        MENU_ITEMS.add(new RoomServiceMenuItem("BK02", "South Indian Platter", "Breakfast",
                "3 Steamed idlis, 1 crispy medu vada, spiced lentil sambar and two chutneys",
                new BigDecimal("180.00"), true));
        MENU_ITEMS.add(new RoomServiceMenuItem("BK03", "Masala Omelette & Toast", "Breakfast",
                "Double egg omelette with diced onions, green chillies, coriander & 2 buttered toasts",
                new BigDecimal("160.00"), true));

        // Mains & All-Day Dining
        MENU_ITEMS.add(new RoomServiceMenuItem("MN01", "Club Sandwich", "Mains",
                "Toasted triple-decker sandwich with cheese, tomatoes, crisp lettuce, fries & dip",
                new BigDecimal("220.00"), true));
        MENU_ITEMS.add(new RoomServiceMenuItem("MN02", "Hyderabadi Chicken Biryani", "Mains",
                "Fragrant basmati rice slow-cooked with tender spiced chicken, served with onion raita",
                new BigDecimal("350.00"), true));
        MENU_ITEMS.add(new RoomServiceMenuItem("MN03", "Paneer Butter Masala & Rotis", "Mains",
                "Fresh cottage cheese simmered in rich creamy tomato gravy, served with 3 tawa rotis",
                new BigDecimal("280.00"), true));
        MENU_ITEMS.add(new RoomServiceMenuItem("MN04", "Dal Khichdi & Curd", "Mains",
                "Comforting mild moong dal and basmati rice preparation served with fresh curd and pickle",
                new BigDecimal("190.00"), true));

        // Snacks & Quick Bites
        MENU_ITEMS.add(new RoomServiceMenuItem("SN01", "French Fries with Dip", "Snacks",
                "Crispy golden salted potato fries served with tomato ketchup and garlic mayo",
                new BigDecimal("130.00"), true));
        MENU_ITEMS.add(new RoomServiceMenuItem("SN02", "Veg Spring Rolls (4 Pcs)", "Snacks",
                "Crisp golden wrappers filled with sauteed vegetables, served with sweet chilli sauce",
                new BigDecimal("170.00"), true));

        // Beverages & Desserts
        MENU_ITEMS.add(new RoomServiceMenuItem("BV01", "Hot Masala Tea", "Beverages",
                "Freshly brewed whole leaf tea infused with crushed cardamom, ginger and milk",
                new BigDecimal("60.00"), true));
        MENU_ITEMS.add(new RoomServiceMenuItem("BV02", "Cold Brew Coffee", "Beverages",
                "Chilled blended espresso coffee topped with chocolate drizzle",
                new BigDecimal("120.00"), true));
        MENU_ITEMS.add(new RoomServiceMenuItem("BV03", "Mineral Water Bottle (1L)", "Beverages",
                "Chilled sealed packaged natural mineral water bottle",
                new BigDecimal("40.00"), true));
        MENU_ITEMS.add(new RoomServiceMenuItem("DS01", "Warm Gulab Jamun (2 Pcs)", "Desserts",
                "Soft khoya milk dumplings soaked in aromatic cardamom and saffron sugar syrup",
                new BigDecimal("110.00"), true));
    }

    private RoomServiceMenu() {}

    /**
     * Returns an unmodifiable list of all room service menu items.
     */
    public static List<RoomServiceMenuItem> getAllItems() {
        return Collections.unmodifiableList(MENU_ITEMS);
    }

    /**
     * Returns all unique menu categories.
     */
    public static List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        categories.add("All Categories");
        for (RoomServiceMenuItem item : MENU_ITEMS) {
            if (!categories.contains(item.getCategory())) {
                categories.add(item.getCategory());
            }
        }
        return categories;
    }

    /**
     * Finds a menu item by its code or name.
     */
    public static RoomServiceMenuItem findByName(String name) {
        if (name == null) return null;
        for (RoomServiceMenuItem item : MENU_ITEMS) {
            if (item.getName().equalsIgnoreCase(name.trim())) {
                return item;
            }
        }
        return null;
    }
}
