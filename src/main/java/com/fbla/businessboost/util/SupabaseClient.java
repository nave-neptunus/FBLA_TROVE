package com.fbla.businessboost.util;

import com.fbla.businessboost.model.Review;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
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
            URL url = new URL(SUPABASE_URL + "/rest/v1/reviews");
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
            URL url = new URL(SUPABASE_URL + "/rest/v1/reviews?business_name=eq." + encodedName + "&select=*");

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
            URL url = new URL(SUPABASE_URL + "/rest/v1/app_users");
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
     * Authenticates an existing user.
     * 
     * @param username The username
     * @param password The plaintext password
     * @return true if credentials match, false otherwise
     */
    public static boolean authenticateUser(String username, String password) {
        String hashedPassword = PasswordHasher.hashPassword(password);
        if (hashedPassword == null)
            return false;

        try {
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8.toString());
            String encodedPassword = URLEncoder.encode(hashedPassword, StandardCharsets.UTF_8.toString());

            // Query exact match for username AND password_hash
            URL url = new URL(SUPABASE_URL + "/rest/v1/app_users?username=eq." + encodedUsername + "&password_hash=eq."
                    + encodedPassword + "&select=id");

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
                // If the array is not empty, a matching row was found!
                return jsonArray.length() > 0;
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
            URL url = new URL(
                    SUPABASE_URL + "/rest/v1/user_favorites?username=eq." + encodedUser + "&select=business_name");
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
            URL url = new URL(SUPABASE_URL + "/rest/v1/user_favorites");
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
            URL url = new URL(SUPABASE_URL + "/rest/v1/user_favorites?username=eq." + encodedUser + "&business_name=eq."
                    + encodedBusiness);
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
}
