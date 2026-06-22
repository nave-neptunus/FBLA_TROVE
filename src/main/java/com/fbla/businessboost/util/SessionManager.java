package com.fbla.businessboost.util;

import java.util.*;
import java.util.prefs.Preferences;

/**
 * Manages the global session state for the application.
 * Sessions and favorites are persisted to disk using Java Preferences,
 * so users stay logged in and keep their favorites between app restarts.
 *
 * @author FBLA Development Team
 */
public class SessionManager {

    private static String currentUsername = null;
    private static final Preferences prefs = Preferences.userNodeForPackage(SessionManager.class);
    private static final String PREF_KEY = "logged_in_user";
    private static final String FAV_PREFIX = "favorites_";

    // ==================== Session ====================

    public static void setCurrentUser(String username) {
        currentUsername = username;
        if (username != null) {
            prefs.put(PREF_KEY, username);
        } else {
            prefs.remove(PREF_KEY);
        }
    }

    public static String getCurrentUser() {
        if (currentUsername == null) {
            currentUsername = prefs.get(PREF_KEY, null);
        }
        return currentUsername;
    }

    public static boolean isLoggedIn() {
        return getCurrentUser() != null;
    }

    public static void logout() {
        currentUsername = null;
        prefs.remove(PREF_KEY);
    }

    public static void clearSession() {
        try {
            prefs.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
        currentUsername = null;
    }

    // ==================== Favorites Persistence ====================

    /**
     * Gets the persisted favorites for the current user.
     * Returns a list of business names.
     */
    public static List<String> getFavorites() {
        String user = getCurrentUser();
        if (user == null)
            return new ArrayList<>();
        String stored = prefs.get(FAV_PREFIX + user, "");
        if (stored.isEmpty())
            return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(stored.split("\\|\\|")));
    }

    /**
     * Saves the full favorites list for the current user.
     */
    public static void saveFavorites(List<String> favoriteNames) {
        String user = getCurrentUser();
        if (user == null)
            return;
        String joined = String.join("||", favoriteNames);
        prefs.put(FAV_PREFIX + user, joined);
    }

    /**
     * Adds a single business to the persisted favorites.
     */
    public static void addFavorite(String businessName) {
        List<String> favs = getFavorites();
        if (!favs.contains(businessName)) {
            favs.add(businessName);
            saveFavorites(favs);
        }
    }

    /**
     * Removes a single business from the persisted favorites.
     */
    public static void removeFavorite(String businessName) {
        List<String> favs = getFavorites();
        favs.remove(businessName);
        saveFavorites(favs);
    }

    // ==================== Review Persistence ====================

    private static final String REVIEW_COUNT_PREFIX = "review_count_";

    public static int getReviewCount() {
        String user = getCurrentUser();
        if (user == null)
            return 0;
        return prefs.getInt(REVIEW_COUNT_PREFIX + user, 0);
    }

    public static void incrementReviewCount() {
        String user = getCurrentUser();
        if (user == null)
            return;
        int currentCount = getReviewCount();
        prefs.putInt(REVIEW_COUNT_PREFIX + user, currentCount + 1);
    }

    // ==================== Onboarding ====================

    private static final String ONBOARDING_KEY = "onboarding_complete";

    /**
     * Returns true if the current user has already completed the onboarding wizard.
     *
     * @return true if onboarding is done (or no user is logged in)
     */
    public static boolean isOnboardingComplete() {
        String user = getCurrentUser();
        if (user == null) return true; // No user — skip onboarding
        return prefs.getBoolean(ONBOARDING_KEY + "_" + user, false);
    }

    /**
     * Marks onboarding as complete for the current user.
     * After this, the wizard will never be shown again for this account.
     */
    public static void markOnboardingComplete() {
        String user = getCurrentUser();
        if (user != null) {
            prefs.putBoolean(ONBOARDING_KEY + "_" + user, true);
        }
    }

    // ==================== Preferred ZIP & Categories ====================

    private static final String PREF_ZIP_KEY = "preferred_zip";
    private static final String PREF_CATEGORIES_KEY = "preferred_categories";

    /**
     * Returns the ZIP code the user entered during onboarding, or empty string.
     *
     * @return preferred ZIP code string
     */
    public static String getPreferredZip() {
        String user = getCurrentUser();
        if (user == null) return "";
        return prefs.get(PREF_ZIP_KEY + "_" + user, "");
    }

    /**
     * Saves the user's preferred ZIP code (set during onboarding).
     *
     * @param zip the ZIP code to save
     */
    public static void setPreferredZip(String zip) {
        String user = getCurrentUser();
        if (user != null && zip != null) {
            prefs.put(PREF_ZIP_KEY + "_" + user, zip.trim());
        }
    }

    /**
     * Returns the pipe-separated list of preferred category names selected during
     * onboarding, or empty string if none were chosen.
     *
     * @return pipe-separated categories, e.g. "Food|Retail"
     */
    public static String getPreferredCategories() {
        String user = getCurrentUser();
        if (user == null) return "";
        return prefs.get(PREF_CATEGORIES_KEY + "_" + user, "");
    }

    /**
     * Saves the user's preferred categories (set during onboarding).
     *
     * @param categories pipe-separated category names, e.g. "Food|Retail"
     */
    public static void setPreferredCategories(String categories) {
        String user = getCurrentUser();
        if (user != null && categories != null) {
            prefs.put(PREF_CATEGORIES_KEY + "_" + user, categories);
        }
    }
}
