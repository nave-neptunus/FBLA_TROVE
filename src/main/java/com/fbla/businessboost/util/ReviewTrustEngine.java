package com.fbla.businessboost.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Tier 2.1 - Layered Bot & Fake-Review Defense.
 * Handles rate limiting, simple proof-of-work, and trust scoring.
 */
public class ReviewTrustEngine {

    // Rate Limiting
    private static final long RATE_LIMIT_MS = 10000; // 10 seconds between reviews
    private static final Map<String, Long> lastReviewTimeMap = new HashMap<>();

    // Trust Scoring Base
    private static final Map<String, Double> userTrustScores = new HashMap<>();

    /**
     * Checks if the user is submitting reviews too fast.
     */
    public static boolean isRateLimited(String username) {
        long now = System.currentTimeMillis();
        long lastTime = lastReviewTimeMap.getOrDefault(username, 0L);
        if (now - lastTime < RATE_LIMIT_MS) {
            return true;
        }
        lastReviewTimeMap.put(username, now);
        return false;
    }

    /**
     * Calculates a simple Proof of Work challenge.
     * @return an integer array where [0] is operand1, [1] is operand2, [2] is the expected sum.
     */
    public static int[] generateProofOfWork() {
        int op1 = (int) (Math.random() * 10) + 1;
        int op2 = (int) (Math.random() * 10) + 1;
        return new int[]{op1, op2, op1 + op2};
    }

    /**
     * Calculates a dynamic trust score (0.1 to 2.0 multiplier) for a user.
     * Factors in their total favorites, streaks, and previous reviews.
     */
    public static double getTrustScore(String username) {
        // Base score starts at 1.0
        double baseScore = 1.0;

        // If they are anonymous or local stub, trust is lower
        if (username == null || username.equals("you@example.com")) {
            return 0.8;
        }

        // We can fetch local session stats to build trust
        int reviews = SessionManager.getReviewCount();
        int favorites = SessionManager.getFavorites().size();

        // More engagement = higher trust (cap at 2.0)
        double trust = baseScore + (reviews * 0.1) + (favorites * 0.05);
        if (trust > 2.0) {
            trust = 2.0;
        }
        
        userTrustScores.put(username, trust);
        return trust;
    }

    /**
     * Weights a new review rating based on the user's trust score.
     * This pushes the aggregate rating towards the opinion of trusted users.
     */
    public static double weightRating(String username, double rawRating, double currentAggregate, int totalReviews) {
        double trustMultiplier = getTrustScore(username);
        
        // Compute new weighted rating
        double totalWeightedScore = currentAggregate * totalReviews;
        double newTotalWeightedScore = totalWeightedScore + (rawRating * trustMultiplier);
        double newTotalDivisor = totalReviews + trustMultiplier;
        
        return Math.round((newTotalWeightedScore / newTotalDivisor) * 10.0) / 10.0;
    }
}
