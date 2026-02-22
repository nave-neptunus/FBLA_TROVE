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
}
