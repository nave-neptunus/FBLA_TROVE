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
}
