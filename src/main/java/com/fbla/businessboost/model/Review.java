package com.fbla.businessboost.model;

/**
 * Represents a single customer review for a business.
 */
public class Review {
    private String businessName;
    private double rating;
    private String reviewText;
    private String timestamp;

    public Review(String businessName, double rating, String reviewText, String timestamp) {
        this.businessName = businessName;
        this.rating = rating;
        this.reviewText = reviewText;
        this.timestamp = timestamp;
    }

    public String getBusinessName() {
        return businessName;
    }

    public double getRating() {
        return rating;
    }

    public String getReviewText() {
        return reviewText;
    }

    public String getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("%.1f ⭐ - \"%s\"", rating, reviewText);
    }
}
