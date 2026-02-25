package com.fbla.businessboost.util;

import java.util.prefs.Preferences;

/**
 * Manages the global session state for the application.
 * Sessions are persisted to disk using Java Preferences, so users
 * stay logged in between app restarts.
 *
 * @author FBLA Development Team
 */
public class SessionManager {

    private static String currentUsername = null;
    private static final Preferences prefs = Preferences.userNodeForPackage(SessionManager.class);
    private static final String PREF_KEY = "logged_in_user";

    /**
     * Set the currently logged-in user and persist to disk.
     */
    public static void setCurrentUser(String username) {
        currentUsername = username;
        if (username != null) {
            prefs.put(PREF_KEY, username);
        } else {
            prefs.remove(PREF_KEY);
        }
    }

    /**
     * Gets the current user (checks persisted session on first call).
     */
    public static String getCurrentUser() {
        if (currentUsername == null) {
            currentUsername = prefs.get(PREF_KEY, null);
        }
        return currentUsername;
    }

    /**
     * Checks if someone is currently logged in (including persisted sessions).
     */
    public static boolean isLoggedIn() {
        return getCurrentUser() != null;
    }

    /**
     * Clears the active session and removes persisted data (full logout).
     */
    public static void logout() {
        currentUsername = null;
        prefs.remove(PREF_KEY);
    }
}
