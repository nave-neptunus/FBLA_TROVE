package com.fbla.businessboost.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Arrays;

/**
 * Represents a local business entity in the FBLA Business Boost application.
 *
 * <p>
 * This class uses JavaFX properties to enable automatic UI updates when data
 * changes.
 * The choice of JavaFX properties (StringProperty, DoubleProperty,
 * BooleanProperty) over
 * primitive types allows for bidirectional binding with UI components, ensuring
 * that any
 * changes to the model are immediately reflected in the view without manual
 * refresh logic.
 * </p>
 *
 * <p>
 * <strong>Data Type Rationale:</strong>
 * </p>
 * <ul>
 * <li><strong>StringProperty:</strong> Used for name and category to enable
 * automatic
 * TableView cell updates when values change.</li>
 * <li><strong>DoubleProperty:</strong> Used for rating to support decimal
 * precision (0.0-5.0)
 * and automatic UI binding for rating displays.</li>
 * <li><strong>BooleanProperty:</strong> Used for favorite status to enable
 * checkbox binding
 * and automatic favorites list updates.</li>
 * <li><strong>ObservableList&lt;String&gt;:</strong> Used for deals to allow
 * dynamic
 * addition/removal with automatic UI refresh in the deals pane.</li>
 * </ul>
 *
 * <p>
 * <strong>Variable Scope:</strong> All fields are private with public
 * getter/setter methods
 * following JavaBean conventions, ensuring encapsulation and controlled access
 * to business data.
 * </p>
 *
 * @author FBLA Development Team
 * @version 1.0
 */
public class Business {

    /** Business name - StringProperty for UI binding */
    private final StringProperty name;

    /**
     * Business category (Food, Retail, or Services) - StringProperty for filtering
     */
    private final StringProperty category;

    /**
     * Average customer rating (0.0 to 5.0) - DoubleProperty for precise decimal
     * values
     */
    private final DoubleProperty rating;

    /** List of current deals and coupons - ObservableList for dynamic UI updates */
    private final ObservableList<String> deals;

    /** Description of the business */
    private final StringProperty description;

    /** Favorite/bookmark status - BooleanProperty for checkbox binding */
    private final BooleanProperty favorite;

    /** URL of the business cover image from Yelp */
    private final StringProperty imageUrl;

    /** Display address of the business */
    private final StringProperty address;

    /** Price level: "$", "$$", "$$$", or "$$$$" */
    private final StringProperty price;

    /** Tags for recommendation engine (e.g. #outdoor, #budget, #date-night) */
    private final ObservableList<String> tags;

    /** URL to the business's Yelp page */
    private final StringProperty yelpUrl;

    /** Phone number of the business */
    private final StringProperty phone;

    /** Website of the business */
    private final StringProperty website;

    /** Opening hours of the business */
    private final StringProperty hours;

    /** Geographic latitude from OpenStreetMap (used for static map thumbnail) */
    private double latitude = 0.0;

    /** Geographic longitude from OpenStreetMap (used for static map thumbnail) */
    private double longitude = 0.0;

    /**
     * Constructs a new Business with the specified attributes.
     *
     * @param name     The business name (must not be null)
     * @param category The business category: "Food", "Retail", or "Services"
     * @param rating   The average rating (0.0 to 5.0)
     */
    public Business(String name, String category, double rating) {
        this.name = new SimpleStringProperty(name);
        this.category = new SimpleStringProperty(category);
        this.rating = new SimpleDoubleProperty(rating);
        this.deals = FXCollections.observableArrayList();
        this.tags = FXCollections.observableArrayList();
        this.description = new SimpleStringProperty("");
        this.favorite = new SimpleBooleanProperty(false);
        this.imageUrl = new SimpleStringProperty("");
        this.address = new SimpleStringProperty("");
        this.price = new SimpleStringProperty("");
        this.yelpUrl = new SimpleStringProperty("");
        this.phone = new SimpleStringProperty("");
        this.website = new SimpleStringProperty("");
        this.hours = new SimpleStringProperty("");
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

    // ==================== Description Property ====================

    public String getDescription() {
        return description.get();
    }

    public void setDescription(String desc) {
        this.description.set(desc);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    // ==================== Image URL Property ====================

    public String getImageUrl() {
        return imageUrl.get();
    }

    public void setImageUrl(String url) {
        this.imageUrl.set(url);
    }

    public StringProperty imageUrlProperty() {
        return imageUrl;
    }

    // ==================== Address Property ====================

    public String getAddress() {
        return address.get();
    }

    public void setAddress(String addr) {
        this.address.set(addr);
    }

    public StringProperty addressProperty() {
        return address;
    }

    // ==================== Price Property ====================

    public String getPrice() {
        return price.get();
    }

    public void setPrice(String price) {
        this.price.set(price);
    }

    public StringProperty priceProperty() {
        return this.price;
    }

    // ==================== Yelp URL Property ====================

    public String getYelpUrl() {
        return yelpUrl.get();
    }

    public void setYelpUrl(String url) {
        this.yelpUrl.set(url);
    }

    public StringProperty yelpUrlProperty() {
        return yelpUrl;
    }

    // ==================== Phone Property ====================

    public String getPhone() {
        return phone.get();
    }

    public void setPhone(String phone) {
        this.phone.set(phone);
    }

    public StringProperty phoneProperty() {
        return phone;
    }

    // ==================== Website Property ====================

    public String getWebsite() {
        return website.get();
    }

    public void setWebsite(String website) {
        this.website.set(website);
    }

    public StringProperty websiteProperty() {
        return website;
    }

    // ==================== Hours Property ====================

    public String getHours() {
        return hours.get();
    }

    public void setHours(String hours) {
        this.hours.set(hours);
    }

    public StringProperty hoursProperty() {
        return hours;
    }

    // ==================== Deals List ====================

    /**
     * Returns the observable list of deals.
     * This list can be modified directly and changes will automatically update the
     * UI.
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

    // ==================== Tags List ====================

    /**
     * Returns the observable list of tags for recommendation matching.
     *
     * @return ObservableList of tag strings
     */
    public ObservableList<String> getTags() {
        return tags;
    }

    /**
     * Adds tags to this business.
     *
     * @param newTags Tags to add
     */
    public void addTags(String... newTags) {
        this.tags.addAll(Arrays.asList(newTags));
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

    // ==================== Latitude / Longitude ====================

    /**
     * Returns the geographic latitude of this business (from OpenStreetMap).
     * Used to generate a static map thumbnail URL.
     *
     * @return latitude in decimal degrees, or 0.0 if not set
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Sets the geographic latitude.
     *
     * @param latitude latitude in decimal degrees
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * Returns the geographic longitude of this business (from OpenStreetMap).
     *
     * @return longitude in decimal degrees, or 0.0 if not set
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Sets the geographic longitude.
     *
     * @param longitude longitude in decimal degrees
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - Rating: %.1f", getName(), getCategory(), getRating());
    }
}
