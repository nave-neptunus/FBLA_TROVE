package com.fbla.businessboost.util;

/**
 * Manages the global session state for the application.
 * Currently keeps track of the globally logged-in user's username.
 */
public class SessionManager {

    // The presently logged-in user
    private static String currentUsername = null;

    /**
     * Set the currently logged-in user.
     * 
     * @param username The authenticated user's username.
     */
    public static void setCurrentUser(String username) {
        currentUsername = username;
    }

    /**
     * Gets the current user.
     * 
     * @return the username of the logged-in user, or null if no one is logged in.
     */
    public static String getCurrentUser() {
        return currentUsername;
    }

    /**
     * Checks if someone is currently logged in.
     * 
     * @return true if a user is logged in, false otherwise.
     */
    public static boolean isLoggedIn() {
        return currentUsername != null;
    }

    /**
     * Clears the active session (Logs out).
     */
    public static void logout() {
        currentUsername = null;
    }
}
