package com.fbla.businessboost.util;

import com.fbla.businessboost.model.Business;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class BusinessFetcher {

    public static ObservableList<Business> fetchBusinessesByZipCode(String zipCode) throws Exception {
        ObservableList<Business> businesses = FXCollections.observableArrayList();

        // 1. Get Lat/Lon from Zip Code using Zippopotam
        String zipUrlStr = "http://api.zippopotam.us/us/" + URLEncoder.encode(zipCode.trim(), "UTF-8");
        URL zipUrl = new URL(zipUrlStr);
        HttpURLConnection zipCon = (HttpURLConnection) zipUrl.openConnection();
        zipCon.setRequestMethod("GET");

        if (zipCon.getResponseCode() != 200) {
            throw new Exception("Invalid Zip Code or API error.");
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(zipCon.getInputStream()));
        String inputLine;
        StringBuilder zipResponse = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            zipResponse.append(inputLine);
        }
        in.close();

        JSONObject zipJson = new JSONObject(zipResponse.toString());
        JSONObject place = zipJson.getJSONArray("places").getJSONObject(0);
        double lat = place.getDouble("latitude");
        double lon = place.getDouble("longitude");

        // 2. Get businesses around this lat/lon using Overpass API
        String query = String.format(
                "[out:json][timeout:15];" +
                        "(" +
                        "node(around:2000,%f,%f)[\"amenity\"~\"restaurant|cafe|fast_food|bar\"];" +
                        "node(around:2000,%f,%f)[\"shop\"];" +
                        "node(around:2000,%f,%f)[\"leisure\"=\"fitness_centre\"];" +
                        ");" +
                        "out 40;",
                lat, lon, lat, lon, lat, lon);

        String overpassUrlStr = "https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(query, "UTF-8");
        URL overpassUrl = new URL(overpassUrlStr);
        HttpURLConnection overpassCon = (HttpURLConnection) overpassUrl.openConnection();
        overpassCon.setRequestMethod("GET");
        overpassCon.setRequestProperty("User-Agent", "FBLA-Business-Boost/1.0");

        int responseCode = overpassCon.getResponseCode();
        if (responseCode != 200) {
            java.io.InputStream errorStream = overpassCon.getErrorStream();
            if (errorStream != null) {
                BufferedReader errorIn = new BufferedReader(new InputStreamReader(errorStream));
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = errorIn.readLine()) != null) {
                    errorResponse.append(line);
                }
                errorIn.close();
                System.err.println("Overpass API Error Response: " + errorResponse.toString());
            }
            throw new Exception("Failed to fetch businesses from Overpass API. HTTP Code: " + responseCode);
        }

        BufferedReader opIn = new BufferedReader(new InputStreamReader(overpassCon.getInputStream()));
        StringBuilder opResponse = new StringBuilder();
        while ((inputLine = opIn.readLine()) != null) {
            opResponse.append(inputLine);
        }
        opIn.close();

        JSONObject opJson = new JSONObject(opResponse.toString());
        JSONArray elements = opJson.getJSONArray("elements");

        for (int i = 0; i < elements.length(); i++) {
            JSONObject node = elements.getJSONObject(i);
            if (node.has("tags")) {
                JSONObject tags = node.getJSONObject("tags");
                if (tags.has("name")) {
                    String name = tags.getString("name");

                    String category = "Retail";
                    if (tags.has("amenity")) {
                        category = "Food";
                    } else if (tags.has("leisure")) {
                        category = "Services";
                    }

                    String description = "A local " + category.toLowerCase() + " business in this area.";
                    if (tags.has("description")) {
                        description = tags.getString("description");
                    } else if (tags.has("cuisine")) {
                        description = "A " + tags.getString("cuisine") + " restaurant.";
                    }

                    // Try to get a better description from external API for famous brands
                    String externalDescription = fetchBusinessDescription(name);
                    if (externalDescription != null && !externalDescription.isEmpty()) {
                        description = externalDescription;
                    }

                    double rating = 3.5 + Math.random() * 1.5;
                    rating = Math.round(rating * 10.0) / 10.0;

                    Business b = new Business(name, category, rating);
                    b.setDescription(description);
                    // Add some placeholder deals
                    b.addDeal("Opening special discount!");

                    businesses.add(b);
                }
            }
        }

        return businesses;
    }

    /**
     * Attempts to fetch a short introductory description of a business from
     * DuckDuckGo Instant Answer API.
     * This API is better for entity lookups than direct Wikipedia because it won't
     * redirect to random articles.
     * 
     * @param businessName The name of the business to look up
     * @return The description summary, or null if not found/error
     */
    private static String fetchBusinessDescription(String businessName) {
        try {
            // Clean up name for search
            String searchName = businessName.split("-")[0].split(",")[0].trim();

            String searchUrlStr = "https://api.duckduckgo.com/?q="
                    + URLEncoder.encode(searchName, "UTF-8") + "&format=json";

            URL url = new URL(searchUrlStr);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "FBLA-Business-Boost/1.0");
            con.setConnectTimeout(2000); // Short timeout
            con.setReadTimeout(2000);

            if (con.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject json = new JSONObject(response.toString());
                if (json.has("AbstractText")) {
                    String abstractText = json.getString("AbstractText").trim();
                    if (!abstractText.isEmpty() && !abstractText.contains("may refer to")) {
                        // Truncate to first two sentences for brevity if it's very long
                        if (abstractText.length() > 200 && abstractText.contains(". ")) {
                            int secondDot = abstractText.indexOf(". ", abstractText.indexOf(". ") + 1);
                            if (secondDot > 0) {
                                abstractText = abstractText.substring(0, secondDot + 1);
                            }
                        }
                        return abstractText;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore API errors
            System.err.println("Failed to fetch description for " + businessName + ": " + e.getMessage());
        }
        return null;
    }
}
