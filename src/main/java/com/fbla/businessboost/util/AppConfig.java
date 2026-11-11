package com.fbla.businessboost.util;

/**
 * Reads service credentials from environment variables so that none are stored
 * in source control. See .env.example for the full list.
 */
final class AppConfig {

    private AppConfig() {
    }

    /** Returns the named environment variable, or the fallback if unset or blank. */
    static String get(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
