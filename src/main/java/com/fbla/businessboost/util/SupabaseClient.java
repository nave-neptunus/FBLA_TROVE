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
import java.util.ArrayList;
import java.util.List;

/**
 * Handles communication with the Supabase REST API for persistent data storage.
 */
public class SupabaseClient {

    private static final String SUPABASE_URL = "https://your-project.supabase.co";
    private static final String SUPABASE_KEY = "REDACTED_SUPABASE_KEY";

    /**
     * Saves a new review to the Supabase database.
     * 
     * @param businessName The name of the business being reviewed
     * @param rating       The numerical rating given (1.0 to 5.0)
     * @param reviewText   The text content of the review
     * @return true if successful, false otherwise
     */
    public static boolean saveReview(String businessName, double rating, String reviewText) {
        try {
            URL url = URI.create(SUPABASE_URL + "/rest/v1/reviews").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "return=minimal");
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
            return responseCode == 201; // 201 Created is the expected success code
        } catch (Exception e) {
            System.err.println("Error saving review to Supabase: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Fetches all reviews for a specific business from Supabase.
     * 
     * @param businessName The name of the business to fetch reviews for
     * @return A list of Review objects, or an empty list if none found/error
     */
    public static List<Review> getReviewsForBusiness(String businessName) {
        List<Review> reviews = new ArrayList<>();
        try {
            String encodedName = URLEncoder.encode(businessName, StandardCharsets.UTF_8.toString());
            // Query parameter: business_name=eq.encodedName
            URL url = URI.create(SUPABASE_URL + "/rest/v1/reviews?business_name=eq." + encodedName + "&select=*")
                    .toURL();

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);

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
                    // Extract fields, gracefully handle missing ones just in case
                    String name = obj.optString("business_name", businessName);
                    double rating = obj.optDouble("rating", 0.0);
                    String text = obj.optString("review_text", "");
                    String timestamp = obj.optString("created_at", "");

                    reviews.add(new Review(name, rating, text, timestamp));
                }
            } else {
                System.err.println("Failed to fetch reviews. HTTP Response Code: " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("Error fetching reviews from Supabase: " + e.getMessage());
            e.printStackTrace();
        }
        return reviews;
    }

    /**
     * Registers a new user with the given username and password.
     * 
     * @param username The desired username
     * @param password The plaintext password
     * @return true if successful, false if the username is taken or an error occurs
     */
    public static boolean registerUser(String username, String password) {
        String hashedPassword = PasswordHasher.hashPassword(password);
        if (hashedPassword == null)
            return false;

        try {
            URL url = URI.create(SUPABASE_URL + "/rest/v1/app_users").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "return=minimal");
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
                // Read the error response
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    System.err
                            .println("Supabase Registration Failed. HTTP " + responseCode + ": " + response.toString());
                }
                return false;
            }
        } catch (Exception e) {
            System.err.println("Registration failed (Exception): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Authenticates an existing user using bcrypt verification.
     *
     * <p>
     * Since bcrypt hashes cannot be compared directly in SQL (each hash is unique
     * even for the same password due to salting), we fetch the stored hash by
     * username and verify client-side using {@code BCrypt.checkpw}.
     * </p>
     *
     * @param username The username
     * @param password The plaintext password
     * @return true if credentials match, false otherwise
     */
    public static boolean authenticateUser(String username, String password) {
        if (username == null || password == null) return false;

        try {
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8.toString());

            // Fetch only the password_hash for this username
            URL url = URI
                    .create(SUPABASE_URL + "/rest/v1/app_users?username=eq." + encodedUsername
                            + "&select=password_hash")
                    .toURL();

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);

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
                    return false; // Username not found
                }

                String storedHash = jsonArray.getJSONObject(0).optString("password_hash", "");
                // Verify using bcrypt client-side
                return PasswordHasher.verifyPassword(password, storedHash);
            }
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
        try {
            String encodedUser = URLEncoder.encode(username, StandardCharsets.UTF_8.toString());
            URL url = URI.create(
                    SUPABASE_URL + "/rest/v1/user_favorites?username=eq." + encodedUser + "&select=business_name")
                    .toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);

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
            } else {
                System.err.println("Failed to fetch favorites. HTTP " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("Error fetching favorites: " + e.getMessage());
            e.printStackTrace();
        }
        return favorites;
    }

    /**
     * Adds a business to the user's favorites in Supabase.
     */
    public static boolean addFavorite(String username, String businessName) {
        try {
            URL url = URI.create(SUPABASE_URL + "/rest/v1/user_favorites").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "return=minimal");
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
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Removes a business from the user's favorites in Supabase.
     */
    public static boolean removeFavorite(String username, String businessName) {
        try {
            String encodedUser = URLEncoder.encode(username, StandardCharsets.UTF_8.toString());
            String encodedBusiness = URLEncoder.encode(businessName, StandardCharsets.UTF_8.toString());
            URL url = URI
                    .create(SUPABASE_URL + "/rest/v1/user_favorites?username=eq." + encodedUser + "&business_name=eq."
                            + encodedBusiness)
                    .toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);

            int responseCode = conn.getResponseCode();
            return responseCode == 204 || responseCode == 200;
        } catch (Exception e) {
            System.err.println("Failed to remove favorite: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Saves a snapshot of generated report metadata to Supabase.
     */
    public static boolean saveReportMetadata(String username, int totalBusinesses, double avgRating,
            String categories) {
        try {
            URL url = URI.create(SUPABASE_URL + "/rest/v1/user_reports").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "return=minimal");
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
            return responseCode == 201; // 201 Created
        } catch (Exception e) {
            System.err.println("Failed to save report metadata: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Fetches all past reports generated by a specific user.
     */
    public static JSONArray getUserReports(String username) {
        try {
            String encodedUser = URLEncoder.encode(username, StandardCharsets.UTF_8.toString());
            // Fetch ordered by newest first
            URL url = URI.create(
                    SUPABASE_URL + "/rest/v1/user_reports?username=eq." + encodedUser
                            + "&select=*&order=created_at.desc")
                    .toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);

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
            } else {
                System.err.println("Failed to fetch reports. HTTP " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("Error fetching user reports: " + e.getMessage());
            e.printStackTrace();
        }
        return new JSONArray();
    }

    /**
     * Saves/updates the current snapshot of user stats in Supabase.
     */
    public static boolean saveUserStats(String username, int totalFavorites, double avgRating, String topCategory) {
        try {
            URL url = URI.create(SUPABASE_URL + "/rest/v1/user_stats").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST"); // Upsert via POST
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            // Important for upsert logic (assumes 'username' is uniquely constrained if
            // creating single row per user)
            // But if we just want history snapshots, a regular insert works too. Assuming
            // snapshots as per user requirements.
            conn.setRequestProperty("Prefer", "return=minimal");
            conn.setDoOutput(true);

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("username", username);
            jsonBody.put("total_favorites", totalFavorites);
            jsonBody.put("avg_rating", avgRating);
            jsonBody.put("top_category", topCategory);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            return responseCode >= 200 && responseCode < 300;
        } catch (Exception e) {
            System.err.println("Failed to save user stats: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Fetches the latest stats snapshot for a specific user.
     */
    public static JSONObject getUserStats(String username) {
        try {
            String encodedUser = URLEncoder.encode(username, StandardCharsets.UTF_8.toString());
            // Fetch newest snapshot only
            URL url = URI.create(
                    SUPABASE_URL + "/rest/v1/user_stats?username=eq." + encodedUser
                            + "&select=*&order=updated_at.desc&limit=1")
                    .toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);

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
            } else {
                System.err.println("Failed to fetch stats. HTTP " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("Error fetching user stats: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
