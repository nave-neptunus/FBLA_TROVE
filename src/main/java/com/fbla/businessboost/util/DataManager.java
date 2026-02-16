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
 * <p>
 * <strong>Data Structure Choice:</strong> Returns an
 * {@code ObservableList<Business>} rather
 * than a standard {@code List} because JavaFX TableView requires an
 * ObservableList for automatic
 * UI updates. When businesses are added, removed, or modified, the TableView
 * automatically
 * refreshes without manual intervention.
 * </p>
 * 
 * <p>
 * <strong>Design Pattern:</strong> Uses static factory method pattern to
 * provide centralized
 * data initialization, making it easy to swap out the data source in future
 * iterations.
 * </p>
 * 
 * @author FBLA Development Team
 * @version 1.0
 */
public class DataManager {

    /**
     * Creates and returns an ObservableList containing 10 mock businesses.
     * Each business is initialized with realistic data including name, category,
     * rating,
     * and promotional deals.
     * 
     * <p>
     * <strong>Category Distribution:</strong>
     * </p>
     * <ul>
     * <li>Food: 4 businesses</li>
     * <li>Retail: 3 businesses</li>
     * <li>Services: 3 businesses</li>
     * </ul>
     * 
     * @return ObservableList of 10 Business objects ready for TableView binding
     */
    public static ObservableList<Business> initializeMockData() {
        ObservableList<Business> businesses = FXCollections.observableArrayList();

        // ==================== Food Category ====================

        Business b1 = new Business("Joe's Pizza Palace", "Food", 4.5);
        b1.addDeal("20% off large pizzas on Tuesdays");
        b1.addDeal("Free garlic bread with any order over $25");
        businesses.add(b1);

        Business b2 = new Business("Sweet Treats Bakery", "Food", 4.8);
        b2.addDeal("Buy 6 cupcakes, get 2 free");
        b2.addDeal("10% student discount with valid ID");
        businesses.add(b2);

        Business b3 = new Business("Dragon Wok Chinese", "Food", 4.2);
        b3.addDeal("Free spring rolls with lunch combo");
        b3.addDeal("$5 off orders over $30");
        businesses.add(b3);

        Business b4 = new Business("Sunrise Cafe", "Food", 4.6);
        b4.addDeal("Free coffee with breakfast sandwich");
        b4.addDeal("Happy hour: 50% off pastries after 3 PM");
        businesses.add(b4);

        // ==================== Retail Category ====================

        Business b5 = new Business("TechHub Electronics", "Retail", 4.3);
        b5.addDeal("15% off all accessories");
        b5.addDeal("Price match guarantee");
        b5.addDeal("Free tech support for 30 days");
        businesses.add(b5);

        Business b6 = new Business("Fashion Forward Boutique", "Retail", 4.7);
        b6.addDeal("Buy one, get one 50% off on all clothing");
        b6.addDeal("Free alterations on purchases over $100");
        businesses.add(b6);

        Business b7 = new Business("BookNook Bookstore", "Retail", 4.4);
        b7.addDeal("20% off bestsellers");
        b7.addDeal("Free bookmark with every purchase");
        b7.addDeal("Student discount: 10% off textbooks");
        businesses.add(b7);

        // ==================== Services Category ====================

        Business b8 = new Business("QuickCut Hair Salon", "Services", 4.1);
        b8.addDeal("$10 off first visit");
        b8.addDeal("Refer a friend, get 20% off next haircut");
        businesses.add(b8);

        Business b9 = new Business("FitLife Gym", "Services", 4.9);
        b9.addDeal("First month free for new members");
        b9.addDeal("Personal training session: $25 (regular $50)");
        b9.addDeal("Free fitness assessment");
        businesses.add(b9);

        Business b10 = new Business("AutoCare Repair Shop", "Services", 4.0);
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
