package com.fbla.businessboost.util;

import com.fbla.businessboost.model.Business;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;

public class BusinessFetcher {

    // Curated stock images by category (Unsplash permanent URLs)
    private static final String[] FOOD_IMAGES = {
            "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1476224203421-9ac39bcb3327?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1558326567-98ae2405596b?w=400&h=300&fit=crop"
    };

    private static final String[] RETAIL_IMAGES = {
            "https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1528698827591-e19cef791f48?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1472851294608-062f824d29cc?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1526243741027-444d633d7365?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1604719312566-8912e9227c6a?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1555529771-7888783a18d3?w=400&h=300&fit=crop"
    };

    private static final String[] SERVICE_IMAGES = {
            "https://images.unsplash.com/photo-1560066984-138dadb4c035?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1487754180451-c456f719a1fc?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1521590832167-7228fcb04b0e?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1580587771525-78b9dba3b914?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1600880292203-757bb62b4baf?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?w=400&h=300&fit=crop",
            "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=400&h=300&fit=crop"
    };

    private static int foodIdx = 0, retailIdx = 0, serviceIdx = 0;

    // Load API Key securely from environment variable
    private static final String GOOGLE_PLACES_API_KEY = System.getenv("GOOGLE_PLACES_API_KEY");

    public static ObservableList<Business> fetchBusinessesByZipCode(String zipCode) throws Exception {
        ObservableList<Business> businesses = FXCollections.observableArrayList();

        // Reset image indices for fresh search
        foodIdx = 0;
        retailIdx = 0;
        serviceIdx = 0;

        // 1. Get Lat/Lon from Zip Code using Zippopotam
        String zipUrlStr = "http://api.zippopotam.us/us/" + URLEncoder.encode(zipCode.trim(), "UTF-8");
        URL zipUrl = URI.create(zipUrlStr).toURL();
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
        String placeName = place.optString("place name", "");
        String state = place.optString("state abbreviation", "");

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
        URL overpassUrl = URI.create(overpassUrlStr).toURL();
        HttpURLConnection overpassCon = (HttpURLConnection) overpassUrl.openConnection();
        overpassCon.setRequestMethod("GET");
        overpassCon.setRequestProperty("User-Agent", "Commercio/1.0");

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

                    // Build description from OSM tags
                    String description = buildDescription(tags, category);

                    double rating = 3.5 + Math.random() * 1.5;
                    rating = Math.round(rating * 10.0) / 10.0;

                    Business b = new Business(name, category, rating);
                    b.setDescription(description);

                    // Fetch real image if API key is set, otherwise fall back to stock image
                    String realImageUrl = fetchGooglePlacePhoto(name, lat, lon);
                    if (realImageUrl != null) {
                        b.setImageUrl(realImageUrl);
                    } else {
                        b.setImageUrl(getImageForCategory(category));
                    }

                    // Build address from OSM tags + zip code context
                    String address = buildAddress(tags, placeName, state, zipCode);
                    b.setAddress(address);

                    // Set price from cuisine/shop type
                    b.setPrice(estimatePrice(tags));

                    // Add dynamic deals
                    java.util.List<String> generatedDeals = generateDeals(category, rating);
                    for (String deal : generatedDeals) {
                        b.addDeal(deal);
                    }

                    // Auto-assign tags based on OSM data
                    assignTags(b, tags, category);

                    businesses.add(b);
                }
            }
        }

        return businesses;
    }

    /**
     * Returns a stock image URL based on the business category, cycling through
     * the array so each card gets a different image.
     */
    private static String getImageForCategory(String category) {
        switch (category) {
            case "Food":
                return FOOD_IMAGES[foodIdx++ % FOOD_IMAGES.length];
            case "Services":
                return SERVICE_IMAGES[serviceIdx++ % SERVICE_IMAGES.length];
            default:
                return RETAIL_IMAGES[retailIdx++ % RETAIL_IMAGES.length];
        }
    }

    /**
     * Builds a description from OpenStreetMap tags.
     */
    private static String buildDescription(JSONObject tags, String category) {
        StringBuilder desc = new StringBuilder();

        if (tags.has("cuisine")) {
            desc.append(capitalize(tags.getString("cuisine").replace(";", ", ")));
        } else if (tags.has("shop")) {
            desc.append(capitalize(tags.getString("shop").replace("_", " ")));
        } else if (tags.has("amenity")) {
            desc.append(capitalize(tags.getString("amenity").replace("_", " ")));
        }

        if (tags.has("opening_hours")) {
            if (desc.length() > 0)
                desc.append(" · ");
            desc.append(tags.getString("opening_hours"));
        }

        return desc.toString();
    }

    /**
     * Builds an address string from OSM tags.
     */
    private static String buildAddress(JSONObject tags, String placeName, String state, String zipCode) {
        StringBuilder addr = new StringBuilder();

        if (tags.has("addr:street")) {
            if (tags.has("addr:housenumber")) {
                addr.append(tags.getString("addr:housenumber")).append(" ");
            }
            addr.append(tags.getString("addr:street"));
        }

        if (addr.length() > 0) {
            addr.append(", ");
        }

        if (tags.has("addr:city")) {
            addr.append(tags.getString("addr:city"));
        } else if (!placeName.isEmpty()) {
            addr.append(placeName);
        }

        if (!state.isEmpty()) {
            if (addr.length() > 0)
                addr.append(", ");
            addr.append(state);
        }

        if (addr.length() > 0)
            addr.append(" ");
        addr.append(zipCode.trim());

        return addr.toString();
    }

    /**
     * Estimates a price level from OSM tags.
     */
    private static String estimatePrice(JSONObject tags) {
        if (tags.has("amenity")) {
            String amenity = tags.getString("amenity");
            if ("fast_food".equals(amenity))
                return "$";
            if ("cafe".equals(amenity))
                return "$";
            if ("bar".equals(amenity))
                return "$$";
            if ("restaurant".equals(amenity))
                return "$$";
        }
        if (tags.has("shop")) {
            String shop = tags.getString("shop");
            if ("supermarket".equals(shop) || "convenience".equals(shop))
                return "$";
            if ("clothes".equals(shop) || "electronics".equals(shop))
                return "$$";
            if ("jewelry".equals(shop))
                return "$$$";
        }
        return "$$";
    }

    /**
     * Auto-assigns tags to a business based on its OSM data and category.
     * Tags power the recommendation engine for API-fetched businesses.
     */
    private static void assignTags(Business b, JSONObject tags, String category) {
        // Category-based tags
        switch (category) {
            case "Food":
                b.addTags("#dine-in");
                if (tags.has("amenity")) {
                    String amenity = tags.getString("amenity");
                    if ("fast_food".equals(amenity))
                        b.addTags("#budget", "#takeout");
                    if ("cafe".equals(amenity))
                        b.addTags("#coffee", "#cozy");
                    if ("bar".equals(amenity))
                        b.addTags("#late-night", "#date-night");
                    if ("restaurant".equals(amenity))
                        b.addTags("#family");
                }
                if (tags.has("cuisine")) {
                    String cuisine = tags.getString("cuisine").toLowerCase();
                    if (cuisine.contains("pizza") || cuisine.contains("italian"))
                        b.addTags("#italian");
                    if (cuisine.contains("chinese") || cuisine.contains("asian"))
                        b.addTags("#asian");
                    if (cuisine.contains("mexican"))
                        b.addTags("#mexican");
                    if (cuisine.contains("coffee"))
                        b.addTags("#breakfast");
                }
                break;
            case "Retail":
                if (tags.has("shop")) {
                    String shop = tags.getString("shop").toLowerCase();
                    if ("clothes".equals(shop) || "fashion".equals(shop))
                        b.addTags("#fashion", "#trending");
                    if ("electronics".equals(shop))
                        b.addTags("#electronics", "#gadgets");
                    if ("books".equals(shop))
                        b.addTags("#books", "#cozy");
                    if ("supermarket".equals(shop) || "convenience".equals(shop))
                        b.addTags("#budget", "#family");
                    if ("jewelry".equals(shop))
                        b.addTags("#premium", "#date-night");
                }
                break;
            case "Services":
                if (tags.has("leisure"))
                    b.addTags("#fitness", "#health", "#outdoor");
                break;
        }

        // Price-based tags
        String price = b.getPrice();
        if ("$".equals(price))
            b.addTags("#budget");
        if ("$$$".equals(price) || "$$$$".equals(price))
            b.addTags("#premium");
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty())
            return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * Generates a random list of realistic deals based on the business category.
     */
    private static java.util.List<String> generateDeals(String category, double rating) {
        java.util.List<String> deals = new java.util.ArrayList<>();
        java.util.Random random = new java.util.Random();

        int numDeals = random.nextInt(3) + 1;

        String[] foodDeals = {
                "Free drink with any meal purchase over $15",
                "Buy one get one 50% off appetizers",
                "10% off total bill for students with ID",
                "Lunch special: $9.99 including drink",
                "Free dessert on your birthday",
                "Family meal deal: 20% off orders over $40"
        };

        String[] retailDeals = {
                "20% off your entire purchase today",
                "Clearance event: Up to 50% off select items",
                "Buy 2 get 1 free on all accessories",
                "Sign up for our newsletter and get 15% off",
                "Free gift with purchase over $50",
                "Weekend sale: $10 off every $50 spent"
        };

        String[] serviceDeals = {
                "First time customers get 15% off",
                "$20 off your next service",
                "Refer a friend and both get $10 credit",
                "Free consultation or estimate",
                "Loyalty reward: 5th visit is 50% off",
                "Bundle services for a 20% discount"
        };

        String[] sourceArray;
        switch (category) {
            case "Food":
                sourceArray = foodDeals;
                break;
            case "Retail":
                sourceArray = retailDeals;
                break;
            case "Services":
                sourceArray = serviceDeals;
                break;
            default:
                sourceArray = retailDeals;
        }

        java.util.List<String> available = new java.util.ArrayList<>(java.util.Arrays.asList(sourceArray));
        java.util.Collections.shuffle(available, random);

        for (int i = 0; i < numDeals && i < available.size(); i++) {
            deals.add(available.get(i));
        }

        if (rating >= 4.5 && random.nextBoolean()) {
            deals.add("⭐ Top Rated Special: Extra 5% off!");
        }

        return deals;
    }

    /**
     * Attempts to fetch real storefront photo from Google Places API.
     */
    private static String fetchGooglePlacePhoto(String businessName, double lat, double lon) {
        if (GOOGLE_PLACES_API_KEY == null || GOOGLE_PLACES_API_KEY.trim().isEmpty()) {
            System.err.println("❌ Google API Key is missing from .env file!");
            return null; // Don't attempt if key isn't set securely via environment
        }

        try {
            System.out.println("🔍 Searching Google Places for: " + businessName);
            // 1. Find Place to get place_id
            String query = URLEncoder.encode(businessName, "UTF-8");
            String findPlaceUrl = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json" +
                    "?input=" + query +
                    "&inputtype=textquery" +
                    "&locationbias=circle:2000@" + lat + "," + lon +
                    "&fields=place_id" +
                    "&key=" + GOOGLE_PLACES_API_KEY;

            URL url1 = URI.create(findPlaceUrl).toURL();
            HttpURLConnection con1 = (HttpURLConnection) url1.openConnection();
            con1.setRequestMethod("GET");
            con1.setConnectTimeout(3000);
            con1.setReadTimeout(3000);

            if (con1.getResponseCode() != 200)
                return null;

            BufferedReader in1 = new BufferedReader(new InputStreamReader(con1.getInputStream()));
            StringBuilder response1 = new StringBuilder();
            String inputLine1;
            while ((inputLine1 = in1.readLine()) != null)
                response1.append(inputLine1);
            in1.close();

            JSONObject json1 = new JSONObject(response1.toString());
            if (!"OK".equals(json1.optString("status")) || json1.getJSONArray("candidates").length() == 0) {
                System.err.println("⚠️ Google search failed for " + businessName + ". Status: "
                        + json1.optString("status") + " | Error: " + json1.optString("error_message", "No message"));
                return null;
            }

            String placeId = json1.getJSONArray("candidates").getJSONObject(0).getString("place_id");
            System.out.println("✅ Found place ID for " + businessName + ": " + placeId);

            // 2. Get Place Details for photo_reference
            String detailsUrl = "https://maps.googleapis.com/maps/api/place/details/json" +
                    "?place_id=" + placeId +
                    "&fields=photos" +
                    "&key=" + GOOGLE_PLACES_API_KEY;

            URL url2 = URI.create(detailsUrl).toURL();
            HttpURLConnection con2 = (HttpURLConnection) url2.openConnection();
            con2.setRequestMethod("GET");
            con2.setConnectTimeout(3000);
            con2.setReadTimeout(3000);

            if (con2.getResponseCode() != 200)
                return null;

            BufferedReader in2 = new BufferedReader(new InputStreamReader(con2.getInputStream()));
            StringBuilder response2 = new StringBuilder();
            String inputLine2;
            while ((inputLine2 = in2.readLine()) != null)
                response2.append(inputLine2);
            in2.close();

            JSONObject json2 = new JSONObject(response2.toString());
            if (!"OK".equals(json2.optString("status"))) {
                System.err.println("⚠️ Could not get details for " + businessName + ". Status: "
                        + json2.optString("status") + " | Error: " + json2.optString("error_message", "No message"));
                return null;
            }

            JSONObject result = json2.optJSONObject("result");
            if (result == null || !result.has("photos")) {
                System.out.println("ℹ️ No photos found on Google for " + businessName + ". Falling back to stock.");
                return null;
            }

            JSONArray photos = result.getJSONArray("photos");
            if (photos.length() == 0) {
                return null;
            }

            String photoReference = photos.getJSONObject(0).getString("photo_reference");
            System.out.println("🖼️ Extracted photo reference for " + businessName + "!");

            // 3. Construct the actual image URL
            return "https://maps.googleapis.com/maps/api/place/photo" +
                    "?maxwidth=400" +
                    "&photo_reference=" + photoReference +
                    "&key=" + GOOGLE_PLACES_API_KEY;

        } catch (Exception e) {
            System.err.println("Failed to fetch Google photo for " + businessName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Attempts to fetch a short description from DuckDuckGo Instant Answer API.
     */
    private static String fetchBusinessDescription(String businessName) {
        try {
            String searchName = businessName.split("-")[0].split(",")[0].trim();
            String searchUrlStr = "https://api.duckduckgo.com/?q="
                    + URLEncoder.encode(searchName, "UTF-8") + "&format=json";

            URL url = URI.create(searchUrlStr).toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Commercio/1.0");
            con.setConnectTimeout(2000);
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
            System.err.println("Failed to fetch description for " + businessName + ": " + e.getMessage());
        }
        return null;
    }
}
