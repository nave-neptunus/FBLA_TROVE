package com.fbla.businessboost.util;

import com.fbla.businessboost.model.Review;

import java.util.*;

/**
 * Tier 3.3 - Aspect-Level Sentiment Parser.
 * Uses a local regex/keyword-based approach to score a business across four dimensions:
 * Service, Price, Vibe, and Quality.
 */
public class AspectSentimentParser {

    private static final Map<String, List<String>> ASPECT_KEYWORDS = new HashMap<>();

    static {
        ASPECT_KEYWORDS.put("Service", Arrays.asList("service", "staff", "wait", "friendly", "rude", "slow", "fast", "helpful"));
        ASPECT_KEYWORDS.put("Price", Arrays.asList("price", "expensive", "cheap", "cost", "value", "overpriced", "affordable"));
        ASPECT_KEYWORDS.put("Vibe", Arrays.asList("vibe", "atmosphere", "ambiance", "music", "loud", "quiet", "clean", "dirty", "cozy"));
        ASPECT_KEYWORDS.put("Quality", Arrays.asList("quality", "taste", "delicious", "terrible", "amazing", "awful", "fresh", "stale"));
    }

    private static final List<String> POSITIVE_MODIFIERS = Arrays.asList("great", "good", "amazing", "friendly", "fast", "helpful", "cheap", "affordable", "value", "cozy", "clean", "delicious", "fresh");
    private static final List<String> NEGATIVE_MODIFIERS = Arrays.asList("rude", "slow", "expensive", "overpriced", "loud", "dirty", "terrible", "awful", "stale", "bad", "poor");

    public static class AspectScores {
        public double service = 0.5;
        public double price = 0.5;
        public double vibe = 0.5;
        public double quality = 0.5;
    }

    /**
     * Parses a list of reviews and returns average aspect scores between 0.0 and 1.0.
     */
    public static AspectScores calculateAspectScores(List<Review> reviews) {
        AspectScores scores = new AspectScores();
        if (reviews == null || reviews.isEmpty()) {
            return scores;
        }

        Map<String, double[]> runningTotals = new HashMap<>();
        runningTotals.put("Service", new double[]{0, 0}); // [sum, count]
        runningTotals.put("Price", new double[]{0, 0});
        runningTotals.put("Vibe", new double[]{0, 0});
        runningTotals.put("Quality", new double[]{0, 0});

        for (Review r : reviews) {
            String text = r.getReviewText().toLowerCase();
            String[] words = text.replaceAll("[^a-z\\s]", "").split("\\s+");

            // Evaluate each sentence/word window for sentiment around aspect keywords
            for (String aspect : ASPECT_KEYWORDS.keySet()) {
                boolean aspectMentioned = false;
                double localSentiment = 0.5; // Default neutral

                for (String word : words) {
                    if (ASPECT_KEYWORDS.get(aspect).contains(word)) {
                        aspectMentioned = true;
                        if (POSITIVE_MODIFIERS.contains(word)) localSentiment = 1.0;
                        if (NEGATIVE_MODIFIERS.contains(word)) localSentiment = 0.0;
                    } else if (aspectMentioned) {
                        // Look at nearby modifiers
                        if (POSITIVE_MODIFIERS.contains(word)) localSentiment = Math.min(1.0, localSentiment + 0.3);
                        if (NEGATIVE_MODIFIERS.contains(word)) localSentiment = Math.max(0.0, localSentiment - 0.3);
                    }
                }

                if (aspectMentioned) {
                    // Fallback to star rating if no modifiers found
                    if (localSentiment == 0.5) {
                        localSentiment = (r.getRating() - 1.0) / 4.0; 
                    }
                    runningTotals.get(aspect)[0] += localSentiment;
                    runningTotals.get(aspect)[1] += 1;
                }
            }
        }

        if (runningTotals.get("Service")[1] > 0) scores.service = runningTotals.get("Service")[0] / runningTotals.get("Service")[1];
        if (runningTotals.get("Price")[1] > 0) scores.price = runningTotals.get("Price")[0] / runningTotals.get("Price")[1];
        if (runningTotals.get("Vibe")[1] > 0) scores.vibe = runningTotals.get("Vibe")[0] / runningTotals.get("Vibe")[1];
        if (runningTotals.get("Quality")[1] > 0) scores.quality = runningTotals.get("Quality")[0] / runningTotals.get("Quality")[1];

        return scores;
    }
}
