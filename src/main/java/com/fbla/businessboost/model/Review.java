package com.fbla.businessboost.model;

import javafx.beans.property.*;

/**
 * Represents a single customer review for a business.
 */
public class Review {
    private final StringProperty businessName;
    private final DoubleProperty rating;
    private final StringProperty reviewText;
    private final StringProperty timestamp;

    public Review(String businessName, double rating, String reviewText, String timestamp) {
        this.businessName = new SimpleStringProperty(businessName);
        this.rating = new SimpleDoubleProperty(rating);
        this.reviewText = new SimpleStringProperty(reviewText);
        this.timestamp = new SimpleStringProperty(timestamp);
    }

    public String getBusinessName() {
        return businessName.get();
    }

    public StringProperty businessNameProperty() {
        return businessName;
    }

    public double getRating() {
        return rating.get();
    }

    public DoubleProperty ratingProperty() {
        return rating;
    }

    public String getReviewText() {
        return reviewText.get();
    }

    public StringProperty reviewTextProperty() {
        return reviewText;
    }

    public String getTimestamp() {
        return timestamp.get();
    }

    public StringProperty timestampProperty() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("%.1f / 5.0 - \"%s\"", getRating(), getReviewText());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Review review = (Review) o;
        return Double.compare(getRating(), review.getRating()) == 0 &&
               java.util.Objects.equals(getBusinessName(), review.getBusinessName()) &&
               java.util.Objects.equals(getReviewText(), review.getReviewText()) &&
               java.util.Objects.equals(getTimestamp(), review.getTimestamp());
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getBusinessName(), getRating(), getReviewText(), getTimestamp());
    }
}
