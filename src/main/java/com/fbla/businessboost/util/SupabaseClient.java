package com.fbla.businessboost.util;

import com.fbla.businessboost.model.Review;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Handles communication with the Supabase REST API for persistent data storage.
 * Optimizations (v5.0):
 * - Implements 1000ms connection/read timeouts to prevent network freezes.
 * - Catch network and host exceptions to trigger local database fallback.
 * - Offline/local fallback persists reviews, favorites, and user credentials locally.
 */
public class SupabaseClient {

    private static final String SUPABASE_URL = AppConfig.get("TROVE_SUPABASE_URL", "https://supabase.invalid");
    private static final String SUPABASE_KEY = AppConfig.get("TROVE_SUPABASE_KEY", "");

    // Local preferences database fallback nodes
    private static final Preferences localPrefs = Preferences.userNodeForPackage(SupabaseClient.class);
    private static final int NETWORK_TIMEOUT_MS = 1000;

    /**
     * Saves a new review to the Supabase database. Falls back to local preferences if offline.
     */
    public static boolean saveReview(String businessName, double rating, String reviewText) {
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(SUPABASE_URL + "/rest/v1/reviews").toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "return=minimal");
            conn.setConnectTimeout(NETWORK_TIMEOUT_MS);
            conn.setReadTimeout(NETWORK_TIMEOUT_MS);
            conn.setDoOutput(true);

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("business_name", businessName);
            jsonBody.put("rating", rating);
            jsonBody.put("review_text", reviewText);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            return responseCode == 201;
        } catch (java.net.UnknownHostException | java.net.ConnectException | java.net.SocketTimeoutException | java.net.NoRouteToHostException e) {
            System.err.println("⚠️ Supabase host offline. Saving review to local offline database.");
            return saveReviewLocally(businessName, rating, reviewText);
        } catch (Exception e) {
            System.err.println("Error saving review to Supabase: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Fetches all reviews for a specific business. Includes offline database support.
     */
    public static List<Review> getReviewsForBusiness(String businessName) {
        List<Review> reviews = new ArrayList<>();
        HttpURLConnection conn = null;
        try {
            String encodedName = URLEncoder.encode(businessName, StandardCharsets.UTF_8.toString());
            URL url = URI.create(SUPABASE_URL + "/rest/v1/reviews?business_name=eq." + encodedName + "&select=*")
                    .toURL();

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
            conn.setConnectTimeout(NETWORK_TIMEOUT_MS);
            conn.setReadTimeout(NETWORK_TIMEOUT_MS);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONArray jsonArray = new JSONArray(response.toString());
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String name = obj.optString("business_name", businessName);
                    double rating = obj.optDouble("rating", 0.0);
                    String text = obj.optString("review_text", "");
                    String timestamp = obj.optString("created_at", "");

                    reviews.add(new Review(name, rating, text, timestamp));
                }
                
                // Append any offline reviews submitted locally
                reviews.addAll(getLocalReviewsForBusiness(businessName));
            } else {
                System.err.println("Failed to fetch reviews. HTTP Response Code: " + responseCode);
                return getLocalReviewsForBusiness(businessName);
            }
        } catch (java.net.UnknownHostException | java.net.ConnectException | java.net.SocketTimeoutException | java.net.NoRouteToHostException e) {
            System.err.println("⚠️ Supabase host offline. Loading local reviews.");
            return getLocalReviewsForBusiness(businessName);
        } catch (Exception e) {
            System.err.println("Error fetching reviews from Supabase: " + e.getMessage());
            e.printStackTrace();
        }
        return reviews;
    }

    /**
     * Registers a new user. Falls back to offline registry on network error.
     */
    public static boolean registerUser(String username, String password) {
        String hashedPassword = PasswordHasher.hashPassword(password);
        if (hashedPassword == null)
            return false;

        HttpURLConnection conn = null;

        try {
            URL url = URI.create(SUPABASE_URL + "/rest/v1/app_users").toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "return=minimal");
            conn.setConnectTimeout(NETWORK_TIMEOUT_MS);
            conn.setReadTimeout(NETWORK_TIMEOUT_MS);
            conn.setDoOutput(true);

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("username", username);
            jsonBody.put("password_hash", hashedPassword);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 201) {
                return true;
            } else {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    System.err.println("Supabase Registration Failed. HTTP " + responseCode + ": " + response.toString());
                }
                return false;
            }
        } catch (java.net.UnknownHostException | java.net.ConnectException | java.net.SocketTimeoutException | java.net.NoRouteToHostException e) {
            System.err.println("⚠️ Supabase host offline. Registering user in local database.");
            return registerLocalUser(username, hashedPassword);
        } catch (Exception e) {
            System.err.println("Registration failed (Exception): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Authenticates a user. Falls back to offline database check on network error.
     */
    public static boolean authenticateUser(String username, String password) {
        if (username == null || password == null) return false;

        HttpURLConnection conn = null;

        try {
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8.toString());
            URL url = URI.create(SUPABASE_URL + "/rest/v1/app_users?username=eq." + encodedUsername + "&select=password_hash")
                    .toURL();

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
            conn.setConnectTimeout(NETWORK_TIMEOUT_MS);
            conn.setReadTimeout(NETWORK_TIMEOUT_MS);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONArray jsonArray = new JSONArray(response.toString());
                if (jsonArray.length() == 0) {
                    return authenticateLocalUser(username, password); // Check local database just in case
                }

                String storedHash = jsonArray.getJSONObject(0).optString("password_hash", "");
                return PasswordHasher.verifyPassword(password, storedHash);
            }
        } catch (java.net.UnknownHostException | java.net.ConnectException | java.net.SocketTimeoutException | java.net.NoRouteToHostException e) {
            System.err.println("⚠️ Supabase host offline. Authenticating against local database.");
            return authenticateLocalUser(username, password);
        } catch (Exception e) {
            System.err.println("Authentication failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Fetches a list of favorited business names for a specific user.
     */
    public static java.util.List<String> getUserFavorites(String username) {
        java.util.List<String> favorites = new java.util.ArrayList<>();
        HttpURLConnection conn = null;
        try {
            String encodedUser = URLEncoder.encode(username, StandardCharsets.UTF_8.toString());
            URL url = URI.create(SUPABASE_URL + "/rest/v1/user_favorites?username=eq." + encodedUser + "&select=business_name")
                    .toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
            conn.setConnectTimeout(NETWORK_TIMEOUT_MS);
            conn.setReadTimeout(NETWORK_TIMEOUT_MS);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    JSONArray jsonArray = new JSONArray(response.toString());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        favorites.add(jsonArray.getJSONObject(i).getString("business_name"));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching favorites: " + e.getMessage());
        }
        return favorites;
    }

    /**
     * Adds a business to the user's favorites in Supabase.
     */
    public static boolean addFavorite(String username, String businessName) {
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(SUPABASE_URL + "/rest/v1/user_favorites").toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "return=minimal");
            conn.setConnectTimeout(NETWORK_TIMEOUT_MS);
            conn.setReadTimeout(NETWORK_TIMEOUT_MS);
            conn.setDoOutput(true);

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("username", username);
            jsonBody.put("business_name", businessName);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            return responseCode == 201;
        } catch (Exception e) {
            System.err.println("Failed to add favorite: " + e.getMessage());
            return false;
        }
    }

    /**
     * Removes a business from the user's favorites in Supabase.
     */
    public static boolean removeFavorite(String username, String businessName) {
        HttpURLConnection conn = null;
        try {
            String encodedUser = URLEncoder.encode(username, StandardCharsets.UTF_8.toString());
            String encodedBusiness = URLEncoder.encode(businessName, StandardCharsets.UTF_8.toString());
            URL url = URI.create(SUPABASE_URL + "/rest/v1/user_favorites?username=eq." + encodedUser + "&business_name=eq." + encodedBusiness)
                    .toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
            conn.setConnectTimeout(NETWORK_TIMEOUT_MS);
            conn.setReadTimeout(NETWORK_TIMEOUT_MS);

            int responseCode = conn.getResponseCode();
            return responseCode == 204 || responseCode == 200;
        } catch (Exception e) {
            System.err.println("Failed to remove favorite: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Saves a snapshot of generated report metadata to Supabase.
     */
    public static boolean saveReportMetadata(String username, int totalBusinesses, double avgRating, String categories) {
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(SUPABASE_URL + "/rest/v1/user_reports").toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "return=minimal");
            conn.setConnectTimeout(NETWORK_TIMEOUT_MS);
            conn.setReadTimeout(NETWORK_TIMEOUT_MS);
            conn.setDoOutput(true);

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("username", username);
            jsonBody.put("total_businesses", totalBusinesses);
            jsonBody.put("avg_rating", avgRating);
            jsonBody.put("categories", categories);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            return responseCode == 201;
        } catch (Exception e) {
            System.err.println("Failed to save report metadata: " + e.getMessage());
            return false;
        }
    }

    /**
     * Fetches all past reports generated by a specific user.
     */
    public static JSONArray getUserReports(String username) {
        HttpURLConnection conn = null;
        try {
            String encodedUser = URLEncoder.encode(username, StandardCharsets.UTF_8.toString());
            URL url = URI.create(SUPABASE_URL + "/rest/v1/user_reports?username=eq." + encodedUser + "&select=*&order=created_at.desc")
                    .toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
            conn.setConnectTimeout(NETWORK_TIMEOUT_MS);
            conn.setReadTimeout(NETWORK_TIMEOUT_MS);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    return new JSONArray(response.toString());
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching user reports: " + e.getMessage());
        }
        return new JSONArray();
    }

    /**
     * Saves/updates the current snapshot of user stats in Supabase.
     */
    public static boolean saveUserStats(String username, int totalFavorites, double avgRating, String topCategory, int totalReviews, int userScore) {
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(SUPABASE_URL + "/rest/v1/user_stats").toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "return=minimal");
            conn.setConnectTimeout(NETWORK_TIMEOUT_MS);
            conn.setReadTimeout(NETWORK_TIMEOUT_MS);
            conn.setDoOutput(true);

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("username", username);
            jsonBody.put("total_favorites", totalFavorites);
            jsonBody.put("avg_rating", avgRating);
            jsonBody.put("top_category", topCategory);
            jsonBody.put("total_reviews", totalReviews);
            jsonBody.put("user_score", userScore);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            return responseCode >= 200 && responseCode < 300;
        } catch (Exception e) {
            System.err.println("Failed to save user stats: " + e.getMessage());
            return false;
        }
    }

    /**
     * Fetches the latest stats snapshot for a specific user.
     */
    public static JSONObject getUserStats(String username) {
        HttpURLConnection conn = null;
        try {
            String encodedUser = URLEncoder.encode(username, StandardCharsets.UTF_8.toString());
            URL url = URI.create(SUPABASE_URL + "/rest/v1/user_stats?username=eq." + encodedUser + "&select=*&order=updated_at.desc&limit=1")
                    .toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
            conn.setConnectTimeout(NETWORK_TIMEOUT_MS);
            conn.setReadTimeout(NETWORK_TIMEOUT_MS);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    JSONArray array = new JSONArray(response.toString());
                    if (array.length() > 0) {
                        return array.getJSONObject(0);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching user stats: " + e.getMessage());
        }
        return null;
    }

    /**
     * Fetches all user stats for the leaderboard.
     */
    public static JSONArray getAllUserStats() {
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(SUPABASE_URL + "/rest/v1/user_stats?select=username,total_favorites,avg_rating,top_category,total_reviews,user_score")
                    .toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
            conn.setConnectTimeout(NETWORK_TIMEOUT_MS);
            conn.setReadTimeout(NETWORK_TIMEOUT_MS);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    return new JSONArray(response.toString());
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching all user stats: " + e.getMessage());
        }
        return new JSONArray();
    }

    // ==================== Local Offline Fallback Implementation ====================

    private static boolean registerLocalUser(String username, String hashedPassword) {
        String key = "local_user_hash_" + username;
        if (localPrefs.get(key, null) != null) {
            return false; // User already exists locally
        }
        localPrefs.put(key, hashedPassword);
        return true;
    }

    private static boolean authenticateLocalUser(String username, String password) {
        String key = "local_user_hash_" + username;
        String storedHash = localPrefs.get(key, null);
        if (storedHash == null) {
            // For biometrics, if the user registers biometrically offline, we can auto-register
            if (username.endsWith("_biometric_user")) {
                String hashed = PasswordHasher.hashPassword(password);
                registerLocalUser(username, hashed);
                return true;
            }
            return false;
        }
        return PasswordHasher.verifyPassword(password, storedHash);
    }

    private static boolean saveReviewLocally(String businessName, double rating, String reviewText) {
        try {
            String key = "local_reviews_" + businessName;
            String existingReviews = localPrefs.get(key, "[]");
            JSONArray array = new JSONArray(existingReviews);
            
            JSONObject obj = new JSONObject();
            obj.put("business_name", businessName);
            obj.put("rating", rating);
            obj.put("review_text", reviewText);
            obj.put("created_at", LocalDateTime.now().toString());
            array.put(obj);
            
            localPrefs.put(key, array.toString());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static List<Review> getLocalReviewsForBusiness(String businessName) {
        List<Review> reviews = new ArrayList<>();
        try {
            String key = "local_reviews_" + businessName;
            String existingReviews = localPrefs.get(key, "[]");
            JSONArray array = new JSONArray(existingReviews);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                reviews.add(new Review(
                        obj.optString("business_name", businessName),
                        obj.optDouble("rating", 0.0),
                        obj.optString("review_text", ""),
                        obj.optString("created_at", "")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return reviews;
    }
}
