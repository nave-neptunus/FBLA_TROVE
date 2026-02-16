package com.fbla.businessboost.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Represents a local business entity in the FBLA Business Boost application.
 * 
 * <p>This class uses JavaFX properties to enable automatic UI updates when data changes.
 * The choice of JavaFX properties (StringProperty, DoubleProperty, BooleanProperty) over
 * primitive types allows for bidirectional binding with UI components, ensuring that any
 * changes to the model are immediately reflected in the view without manual refresh logic.</p>
 * 
 * <p><strong>Data Type Rationale:</strong></p>
 * <ul>
 *   <li><strong>StringProperty:</strong> Used for name and category to enable automatic
 *       TableView cell updates when values change.</li>
 *   <li><strong>DoubleProperty:</strong> Used for rating to support decimal precision (0.0-5.0)
 *       and automatic UI binding for rating displays.</li>
 *   <li><strong>BooleanProperty:</strong> Used for favorite status to enable checkbox binding
 *       and automatic favorites list updates.</li>
 *   <li><strong>ObservableList&lt;String&gt;:</strong> Used for deals to allow dynamic
 *       addition/removal with automatic UI refresh in the deals pane.</li>
 * </ul>
 * 
 * <p><strong>Variable Scope:</strong> All fields are private with public getter/setter methods
 * following JavaBean conventions, ensuring encapsulation and controlled access to business data.</p>
 * 
 * @author FBLA Development Team
 * @version 1.0
 */
public class Business {
    
    /** Business name - StringProperty for UI binding */
    private final StringProperty name;
    
    /** Business category (Food, Retail, or Services) - StringProperty for filtering */
    private final StringProperty category;
    
    /** Average customer rating (0.0 to 5.0) - DoubleProperty for precise decimal values */
    private final DoubleProperty rating;
    
    /** List of current deals and coupons - ObservableList for dynamic UI updates */
    private final ObservableList<String> deals;
    
    /** Favorite/bookmark status - BooleanProperty for checkbox binding */
    private final BooleanProperty favorite;
    
    /**
     * Constructs a new Business with the specified attributes.
     * 
     * @param name The business name (must not be null)
     * @param category The business category: "Food", "Retail", or "Services"
     * @param rating The average rating (0.0 to 5.0)
     */
    public Business(String name, String category, double rating) {
        this.name = new SimpleStringProperty(name);
        this.category = new SimpleStringProperty(category);
        this.rating = new SimpleDoubleProperty(rating);
        this.deals = FXCollections.observableArrayList();
        this.favorite = new SimpleBooleanProperty(false);
    }
    
    // ==================== Name Property ====================
    
    public String getName() {
        return name.get();
    }
    
    public void setName(String name) {
        this.name.set(name);
    }
    
    public StringProperty nameProperty() {
        return name;
    }
    
    // ==================== Category Property ====================
    
    public String getCategory() {
        return category.get();
    }
    
    public void setCategory(String category) {
        this.category.set(category);
    }
    
    public StringProperty categoryProperty() {
        return category;
    }
    
    // ==================== Rating Property ====================
    
    public double getRating() {
        return rating.get();
    }
    
    public void setRating(double rating) {
        this.rating.set(rating);
    }
    
    public DoubleProperty ratingProperty() {
        return rating;
    }
    
    // ==================== Deals List ====================
    
    /**
     * Returns the observable list of deals.
     * This list can be modified directly and changes will automatically update the UI.
     * 
     * @return ObservableList of deal strings
     */
    public ObservableList<String> getDeals() {
        return deals;
    }
    
    /**
     * Adds a new deal to this business.
     * 
     * @param deal The deal description to add
     */
    public void addDeal(String deal) {
        this.deals.add(deal);
    }
    
    // ==================== Favorite Property ====================
    
    public boolean isFavorite() {
        return favorite.get();
    }
    
    public void setFavorite(boolean favorite) {
        this.favorite.set(favorite);
    }
    
    public BooleanProperty favoriteProperty() {
        return favorite;
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s) - Rating: %.1f", getName(), getCategory(), getRating());
    }
}
