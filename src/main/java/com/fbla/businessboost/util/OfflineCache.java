package com.fbla.businessboost.util;

import com.fbla.businessboost.model.Business;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Caches the last successful business search result to a local JSON file
 * so the app can still show data when there is no internet connection.
 *
 * <p>
 * <strong>Cache format:</strong> A JSON file at {@code ~/.commercio_cache.json}
 * containing a JSON object with a {@code zip} string and a {@code businesses}
 * array. Each business element stores the fields needed to reconstruct a
 * {@link Business} object for display.
 * </p>
 *
 * <p>
 * Only one ZIP code's data is cached at a time. Loading a cache for a
 * different ZIP than the one stored returns an empty list.
 * </p>
 *
 * @author FBLA Development Team
 * @version 1.0
 */
public class OfflineCache {

    /** Path to the local cache file, stored in the user's home directory. */
    private static final Path CACHE_FILE =
            Paths.get(System.getProperty("user.home"), ".commercio_cache.json");

    /**
     * Serializes a list of businesses to the local cache file.
     *
     * <p>
     * This should be called on a background thread after a successful API fetch
     * so it does not block the UI.
     * </p>
     *
     * @param zipCode    the ZIP code associated with this data
     * @param businesses the businesses to cache
     */
    public static void save(String zipCode, ObservableList<Business> businesses) {
        if (zipCode == null || businesses == null) return;
        try {
            JSONObject root = new JSONObject();
            root.put("zip", zipCode.trim());

            JSONArray arr = new JSONArray();
            for (Business b : businesses) {
                JSONObject obj = new JSONObject();
                obj.put("name",        b.getName());
                obj.put("category",    b.getCategory());
                obj.put("rating",      b.getRating());
                obj.put("description", b.getDescription());
                obj.put("imageUrl",    b.getImageUrl());
                obj.put("address",     b.getAddress());
                obj.put("price",       b.getPrice());
                obj.put("phone",       b.getPhone());
                obj.put("website",     b.getWebsite());
                obj.put("hours",       b.getHours());
                obj.put("latitude",    b.getLatitude());
                obj.put("longitude",   b.getLongitude());

                JSONArray tags = new JSONArray(b.getTags());
                obj.put("tags", tags);

                JSONArray deals = new JSONArray(b.getDeals());
                obj.put("deals", deals);

                arr.put(obj);
            }
            root.put("businesses", arr);

            Files.writeString(CACHE_FILE, root.toString(2), StandardCharsets.UTF_8);
            System.out.println("Offline cache saved: " + businesses.size() + " businesses for ZIP " + zipCode);
        } catch (Exception e) {
            System.err.println("Failed to write offline cache: " + e.getMessage());
        }
    }

    /**
     * Loads cached businesses for the given ZIP code.
     *
     * <p>
     * Returns an empty list if the cache does not exist, is corrupted, or
     * was saved for a different ZIP code.
     * </p>
     *
     * @param zipCode the ZIP code to load cached data for
     * @return an {@link ObservableList} of cached businesses, or empty list
     */
    public static ObservableList<Business> load(String zipCode) {
        ObservableList<Business> result = FXCollections.observableArrayList();
        if (zipCode == null || !hasCachedData(zipCode)) return result;

        try {
            String json = Files.readString(CACHE_FILE, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(json);

            // Verify ZIP matches
            if (!zipCode.trim().equalsIgnoreCase(root.optString("zip", ""))) {
                return result;
            }

            JSONArray arr = root.getJSONArray("businesses");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Business b = new Business(
                        obj.optString("name", "Unknown"),
                        obj.optString("category", "Retail"),
                        obj.optDouble("rating", 3.5)
                );
                b.setDescription(obj.optString("description", ""));
                b.setImageUrl(obj.optString("imageUrl", ""));
                b.setAddress(obj.optString("address", ""));
                b.setPrice(obj.optString("price", "$$"));
                b.setPhone(obj.optString("phone", ""));
                b.setWebsite(obj.optString("website", ""));
                b.setHours(obj.optString("hours", ""));
                b.setLatitude(obj.optDouble("latitude", 0.0));
                b.setLongitude(obj.optDouble("longitude", 0.0));

                JSONArray tags = obj.optJSONArray("tags");
                if (tags != null) {
                    for (int j = 0; j < tags.length(); j++) {
                        b.addTags(tags.getString(j));
                    }
                }

                JSONArray deals = obj.optJSONArray("deals");
                if (deals != null) {
                    for (int j = 0; j < deals.length(); j++) {
                        b.addDeal(deals.getString(j));
                    }
                }

                result.add(b);
            }
            System.out.println("Loaded " + result.size() + " businesses from offline cache.");
        } catch (Exception e) {
            System.err.println("Failed to load offline cache: " + e.getMessage());
        }
        return result;
    }

    /**
     * Returns true if a cache file exists and contains data for the given ZIP.
     *
     * @param zipCode the ZIP code to check
     * @return true if cached data is available for this ZIP
     */
    public static boolean hasCachedData(String zipCode) {
        if (!Files.exists(CACHE_FILE)) return false;
        try {
            String json = Files.readString(CACHE_FILE, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(json);
            return zipCode.trim().equalsIgnoreCase(root.optString("zip", ""));
        } catch (Exception e) {
            return false;
        }
    }

    /** Private constructor — utility class, do not instantiate. */
    private OfflineCache() {
        throw new UnsupportedOperationException("Utility class");
    }
}
