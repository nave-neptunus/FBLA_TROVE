package com.fbla.businessboost.util;

import com.fbla.businessboost.model.Business;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;

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

    public static ObservableList<Business> initializeMockData() {
        List<Business> data = new MockDataAdapter().fetchBusinesses();
        return FXCollections.observableArrayList(data);
    }

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private DataManager() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}
