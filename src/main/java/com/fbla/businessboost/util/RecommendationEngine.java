package com.fbla.businessboost.util;

import com.fbla.businessboost.model.Business;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Recommendation engine using "Collaborative Filtering Lite" — a tag-based
 * matching algorithm that suggests businesses based on the user's favorites.
 *
 * <p>
 * <strong>Algorithm:</strong> Collects all tags from the user's favorited
 * businesses,
 * then scores each non-favorited business by counting how many of its tags
 * match.
 * Returns the top N highest-scoring businesses as recommendations.
 * </p>
 *
 * @author FBLA Development Team
 * @version 1.0
 */
public class RecommendationEngine {

    /** Maximum number of recommendations to return */
    private static final int MAX_RECOMMENDATIONS = 3;

    /**
     * Generates business recommendations based on the user's favorites.
     *
     * <p>
     * <strong>Collaborative Filtering Lite:</strong> Instead of tracking multiple
     * users'
     * preferences (full collaborative filtering), this approach uses the current
     * user's
     * favorite tags as a preference profile and matches against unfavorited
     * businesses.
     * </p>
     *
     * @param favorites     The user's currently favorited businesses
     * @param allBusinesses All available businesses to recommend from
     * @return List of up to 3 recommended Business objects, sorted by relevance
     */
    public static List<Business> getRecommendations(
            List<Business> favorites, List<Business> allBusinesses) {

        if (favorites == null || favorites.isEmpty() || allBusinesses == null) {
            return Collections.emptyList();
        }

        // Step 1: Build a tag frequency map from favorites
        Map<String, Integer> tagFrequency = new HashMap<>();
        for (Business fav : favorites) {
            for (String tag : fav.getTags()) {
                tagFrequency.merge(tag, 1, Integer::sum);
            }
        }

        if (tagFrequency.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 2: Score each non-favorited business by tag overlap
        Set<String> favoriteNames = favorites.stream()
                .map(Business::getName)
                .collect(Collectors.toSet());

        List<Map.Entry<Business, Integer>> scored = new ArrayList<>();
        for (Business b : allBusinesses) {
            if (favoriteNames.contains(b.getName())) {
                continue; // Skip already-favorited businesses
            }

            int score = 0;
            for (String tag : b.getTags()) {
                score += tagFrequency.getOrDefault(tag, 0);
            }

            if (score > 0) {
                scored.add(new AbstractMap.SimpleEntry<>(b, score));
            }
        }

        // Step 3: Sort by score descending, return top N
        scored.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        return scored.stream()
                .limit(MAX_RECOMMENDATIONS)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /** Private constructor — utility class */
    private RecommendationEngine() {
        throw new UnsupportedOperationException("Utility class");
    }
}
