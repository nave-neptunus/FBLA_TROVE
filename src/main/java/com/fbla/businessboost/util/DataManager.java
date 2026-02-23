package com.fbla.businessboost.util;

import com.fbla.businessboost.model.Business;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Manages the initialization and provisioning of mock business data for the
 * application.
 *
 * <p>
 * This utility class creates a hardcoded dataset of 10 sample businesses across
 * three
 * categories (Food, Retail, Services) for demonstration purposes. In a
 * production environment,
 * this would be replaced with database or API integration.
 * </p>
 *
 * @author FBLA Development Team
 * @version 1.0
 */
public class DataManager {

    /**
     * Creates and returns an ObservableList containing 10 mock businesses.
     * Each business is initialized with realistic data including name, category,
     * rating, image URLs, addresses, and promotional deals.
     *
     * @return ObservableList of 10 Business objects ready for card grid display
     */
    public static ObservableList<Business> initializeMockData() {
        ObservableList<Business> businesses = FXCollections.observableArrayList();

        // ==================== Food Category ====================

        Business b1 = new Business("Joe's Pizza Palace", "Food", 4.5);
        b1.setImageUrl("https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=400&h=300&fit=crop");
        b1.setAddress("123 Main St, Springfield, IL");
        b1.setPrice("$$");
        b1.setDescription("Pizza · $$ · A beloved local pizzeria serving NY-style pies since 1985.");
        b1.addDeal("20% off large pizzas on Tuesdays");
        b1.addDeal("Free garlic bread with any order over $25");
        businesses.add(b1);

        Business b2 = new Business("Sweet Treats Bakery", "Food", 4.8);
        b2.setImageUrl("https://images.unsplash.com/photo-1558326567-98ae2405596b?w=400&h=300&fit=crop");
        b2.setAddress("456 Oak Ave, Springfield, IL");
        b2.setPrice("$");
        b2.setDescription("Bakeries · $ · Artisan pastries and custom cakes baked fresh daily.");
        b2.addDeal("Buy 6 cupcakes, get 2 free");
        b2.addDeal("10% student discount with valid ID");
        businesses.add(b2);

        Business b3 = new Business("Dragon Wok Chinese", "Food", 4.2);
        b3.setImageUrl("https://images.unsplash.com/photo-1525755662778-989d0524087e?w=400&h=300&fit=crop");
        b3.setAddress("789 Elm St, Springfield, IL");
        b3.setPrice("$$");
        b3.setDescription("Chinese · $$ · Authentic Sichuan and Cantonese cuisine.");
        b3.addDeal("Free spring rolls with lunch combo");
        b3.addDeal("$5 off orders over $30");
        businesses.add(b3);

        Business b4 = new Business("Sunrise Cafe", "Food", 4.6);
        b4.setImageUrl("https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=400&h=300&fit=crop");
        b4.setAddress("321 Coffee Ln, Springfield, IL");
        b4.setPrice("$");
        b4.setDescription("Coffee & Tea, Breakfast · $ · Cozy neighborhood café with specialty lattes.");
        b4.addDeal("Free coffee with breakfast sandwich");
        b4.addDeal("Happy hour: 50% off pastries after 3 PM");
        businesses.add(b4);

        // ==================== Retail Category ====================

        Business b5 = new Business("TechHub Electronics", "Retail", 4.3);
        b5.setImageUrl("https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=400&h=300&fit=crop");
        b5.setAddress("555 Tech Blvd, Springfield, IL");
        b5.setPrice("$$$");
        b5.setDescription("Electronics · $$$ · Your local source for the latest gadgets and repairs.");
        b5.addDeal("15% off all accessories");
        b5.addDeal("Price match guarantee");
        b5.addDeal("Free tech support for 30 days");
        businesses.add(b5);

        Business b6 = new Business("Fashion Forward Boutique", "Retail", 4.7);
        b6.setImageUrl("https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=400&h=300&fit=crop");
        b6.setAddress("222 Style Way, Springfield, IL");
        b6.setPrice("$$$");
        b6.setDescription("Fashion · $$$ · Curated designer fashion and trending streetwear.");
        b6.addDeal("Buy one, get one 50% off on all clothing");
        b6.addDeal("Free alterations on purchases over $100");
        businesses.add(b6);

        Business b7 = new Business("BookNook Bookstore", "Retail", 4.4);
        b7.setImageUrl("https://images.unsplash.com/photo-1526243741027-444d633d7365?w=400&h=300&fit=crop");
        b7.setAddress("888 Reader Rd, Springfield, IL");
        b7.setPrice("$$");
        b7.setDescription("Bookstores · $$ · Independent bookstore with rare finds and author events.");
        b7.addDeal("20% off bestsellers");
        b7.addDeal("Free bookmark with every purchase");
        b7.addDeal("Student discount: 10% off textbooks");
        businesses.add(b7);

        // ==================== Services Category ====================

        Business b8 = new Business("QuickCut Hair Salon", "Services", 4.1);
        b8.setImageUrl("https://images.unsplash.com/photo-1560066984-138dadb4c035?w=400&h=300&fit=crop");
        b8.setAddress("444 Beauty St, Springfield, IL");
        b8.setPrice("$$");
        b8.setDescription("Hair Salons · $$ · Expert stylists for cuts, color, and treatments.");
        b8.addDeal("$10 off first visit");
        b8.addDeal("Refer a friend, get 20% off next haircut");
        businesses.add(b8);

        Business b9 = new Business("FitLife Gym", "Services", 4.9);
        b9.setImageUrl("https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=400&h=300&fit=crop");
        b9.setAddress("999 Fitness Dr, Springfield, IL");
        b9.setPrice("$$");
        b9.setDescription("Gyms · $$ · State-of-the-art equipment with personal training.");
        b9.addDeal("First month free for new members");
        b9.addDeal("Personal training session: $25 (regular $50)");
        b9.addDeal("Free fitness assessment");
        businesses.add(b9);

        Business b10 = new Business("AutoCare Repair Shop", "Services", 4.0);
        b10.setImageUrl("https://images.unsplash.com/photo-1487754180451-c456f719a1fc?w=400&h=300&fit=crop");
        b10.setAddress("777 Motor Ln, Springfield, IL");
        b10.setPrice("$$");
        b10.setDescription("Auto Repair · $$ · Honest, certified mechanics you can trust.");
        b10.addDeal("Free oil change with major service");
        b10.addDeal("10% off for seniors and military");
        businesses.add(b10);

        return businesses;
    }

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private DataManager() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}
