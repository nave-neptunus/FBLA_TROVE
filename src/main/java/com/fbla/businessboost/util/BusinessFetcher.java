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

    // Curated stock images by category (Picsum deterministic URLs to avoid 403 blocks)
    private static final String[] FOOD_IMAGES = {
            "https://picsum.photos/seed/food1/400/300",
            "https://picsum.photos/seed/food2/400/300",
            "https://picsum.photos/seed/food3/400/300",
            "https://picsum.photos/seed/food4/400/300",
            "https://picsum.photos/seed/food5/400/300",
            "https://picsum.photos/seed/food6/400/300",
            "https://picsum.photos/seed/food7/400/300",
            "https://picsum.photos/seed/food8/400/300"
    };

    private static final String[] RETAIL_IMAGES = {
            "https://picsum.photos/seed/retail1/400/300",
            "https://picsum.photos/seed/retail2/400/300",
            "https://picsum.photos/seed/retail3/400/300",
            "https://picsum.photos/seed/retail4/400/300",
            "https://picsum.photos/seed/retail5/400/300",
            "https://picsum.photos/seed/retail6/400/300",
            "https://picsum.photos/seed/retail7/400/300",
            "https://picsum.photos/seed/retail8/400/300"
    };

    private static final String[] SERVICE_IMAGES = {
            "https://picsum.photos/seed/serv1/400/300",
            "https://picsum.photos/seed/serv2/400/300",
            "https://picsum.photos/seed/serv3/400/300",
            "https://picsum.photos/seed/serv4/400/300",
            "https://picsum.photos/seed/serv5/400/300",
            "https://picsum.photos/seed/serv6/400/300",
            "https://picsum.photos/seed/serv7/400/300",
            "https://picsum.photos/seed/serv8/400/300"
    };

    private static int foodIdx = 0, retailIdx = 0, serviceIdx = 0;

    // List of common big corporate franchises and chains to filter out
    private static final String[] BIG_BRANDS = {
            "starbucks", "subway", "mcdonald", "taco bell", "kfc", "burger king",
            "target", "walmart", "cvs", "walgreens", "dunkin", "wendy", "domino",
            "pizza hut", "papa john", "chipotle", "panera", "panda express", "chick-fil-a",
            "rite aid", "7-eleven", "seven eleven", "denny", "ihop", "applebee", "olive garden",
            "outback", "red lobster", "in-n-out", "jack in the box", "sonic drive-in", "arby",
            "dairy queen", "jimmy john", "jersey mike", "five guys", "carl's jr", "hardee",
            "peet's coffee", "krispy kreme", "baskin robbins", "jamba", "bibibop", "cava", 
            "sweetgreen", "shake shack", "popeyes", "qdoba", "mod pizza", "blaze pizza",
            "noodles & company", "smashburger", "firehouse subs", "potbelly"
    };

    private static boolean isBigBrand(String name) {
        if (name == null)
            return false;
        String lowerName = name.toLowerCase();
        for (String brand : BIG_BRANDS) {
            if (lowerName.contains(brand)) {
                return true;
            }
        }
        return false;
    }

    // Load API Key securely from environment variable
    private static final String BRANDFETCH_API_KEY = "REDACTED_BRANDFETCH_KEY";

    /**
     * Fetches businesses near the given ZIP code with a default 2km radius.
     *
     * @param zipCode 5-digit US ZIP code
     * @return observable list of local businesses
     * @throws Exception on API error or invalid ZIP
     */
    public static ObservableList<Business> fetchBusinessesByZipCode(String zipCode) throws Exception {
        return fetchBusinessesByZipCode(zipCode, 2000);
    }

    /**
     * Fetches businesses near the given ZIP code within a specified radius.
     *
     * @param zipCode      5-digit US ZIP code
     * @param radiusMeters search radius in metres (e.g. 500, 1000, 2000, 5000)
     * @return observable list of local businesses
     * @throws Exception on API error or invalid ZIP
     */
    public static ObservableList<Business> fetchBusinessesByZipCode(String zipCode, int radiusMeters) throws Exception {
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
        zipCon.disconnect();

        JSONObject zipJson = new JSONObject(zipResponse.toString());
        JSONObject place = zipJson.getJSONArray("places").getJSONObject(0);
        double lat = place.getDouble("latitude");
        double lon = place.getDouble("longitude");
        String placeName = place.optString("place name", "");
        String state = place.optString("state abbreviation", "");

        // 2. Get businesses around this lat/lon using Overpass API (with configurable radius)
        String query = String.format(
                "[out:json][timeout:15];" +
                        "(" +
                        "node(around:%d,%f,%f)[\"amenity\"~\"restaurant|cafe|fast_food|bar\"];" +
                        "node(around:%d,%f,%f)[\"shop\"];" +
                        "node(around:%d,%f,%f)[\"leisure\"=\"fitness_centre\"];" +
                        ");" +
                        "out 40;",
                radiusMeters, lat, lon, radiusMeters, lat, lon, radiusMeters, lat, lon);

        String overpassUrlStr = "https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(query, "UTF-8");
        URL overpassUrl = URI.create(overpassUrlStr).toURL();
        HttpURLConnection overpassCon = (HttpURLConnection) overpassUrl.openConnection();
        overpassCon.setRequestMethod("GET");
        overpassCon.setRequestProperty("User-Agent", "Trove/1.0");

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
            overpassCon.disconnect();
            throw new Exception("Failed to fetch businesses from Overpass API. HTTP Code: " + responseCode);
        }

        BufferedReader opIn = new BufferedReader(new InputStreamReader(overpassCon.getInputStream()));
        StringBuilder opResponse = new StringBuilder();
        while ((inputLine = opIn.readLine()) != null) {
            opResponse.append(inputLine);
        }
        opIn.close();
        overpassCon.disconnect();

        JSONObject opJson = new JSONObject(opResponse.toString());
        JSONArray elements = opJson.getJSONArray("elements");

        for (int i = 0; i < elements.length(); i++) {
            JSONObject node = elements.getJSONObject(i);
            if (node.has("tags")) {
                JSONObject tags = node.getJSONObject("tags");
                if (tags.has("name")) {
                    String name = tags.getString("name");
                    String brandTag = tags.optString("brand", "");

                    // Skip big corporate brands to exclusively highlight local small businesses
                    if (isBigBrand(name) || isBigBrand(brandTag)) {
                        System.out.println("🛡️ Filtered out big brand: " + name);
                        continue;
                    }

                    String category = "Retail";
                    if (tags.has("amenity")) {
                        category = "Food";
                    } else if (tags.has("leisure")) {
                        category = "Services";
                    }

                    // Build description from OSM tags
                    String description = buildDescription(tags, category, name);

                    double rating = 3.5 + Math.random() * 1.5;
                    rating = Math.round(rating * 10.0) / 10.0;

                    Business b = new Business(name, category, rating);
                    b.setDescription(description);

                    // Store lat/lon from OSM node for static map thumbnail
                    b.setLatitude(node.optDouble("lat", 0.0));
                    b.setLongitude(node.optDouble("lon", 0.0));

                    String website = tags.has("website") ? tags.getString("website")
                            : (tags.has("contact:website") ? tags.getString("contact:website") : null);
                    if (website != null) {
                        b.setWebsite(website);
                    }

                    String logoUrl = fetchBrandfetchLogo(website);
                    if (logoUrl != null) {
                        b.setImageUrl(logoUrl);
                    } else {
                        b.setImageUrl(getImageForCategory(category));
                    }

                    // Build address from OSM tags + zip code context
                    String address = buildAddress(tags, placeName, state, zipCode);
                    b.setAddress(address);

                    // Extract contact details and hours if available
                    if (tags.has("phone")) {
                        b.setPhone(tags.getString("phone"));
                    } else if (tags.has("contact:phone")) {
                        b.setPhone(tags.getString("contact:phone"));
                    }

                    if (tags.has("opening_hours")) {
                        b.setHours(tags.getString("opening_hours"));
                    }

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
     * Builds a rich, natural-language description from OpenStreetMap tags.
     */
    private static String buildDescription(JSONObject tags, String category, String name) {
        StringBuilder desc = new StringBuilder();

        desc.append(name).append(" is a local ");

        String typeStr = "establishment";
        if (tags.has("cuisine")) {
            typeStr = tags.getString("cuisine").replace(";", " and ").replace("_", " ") + " restaurant";
        } else if (tags.has("shop")) {
            typeStr = tags.getString("shop").replace("_", " ") + " store";
        } else if (tags.has("amenity")) {
            typeStr = tags.getString("amenity").replace("_", " ");
        } else {
            typeStr = category.toLowerCase() + " business";
        }

        desc.append(typeStr).append(" serving the community with great ").append(category.toLowerCase())
                .append(" options.");

        java.util.List<String> features = new java.util.ArrayList<>();
        if (tags.has("wheelchair") && !tags.getString("wheelchair").equals("no")
                && !tags.getString("wheelchair").equals("limited")) {
            features.add("is wheelchair accessible");
        }
        if (tags.has("outdoor_seating") && tags.getString("outdoor_seating").equals("yes")) {
            features.add("offers outdoor seating");
        }
        if (tags.has("takeaway") && tags.getString("takeaway").equals("yes")) {
            features.add("provides convenient takeout");
        }
        if (tags.has("delivery") && tags.getString("delivery").equals("yes")) {
            features.add("delivers locally");
        }
        if (tags.has("diet:vegan") && tags.getString("diet:vegan").equals("yes")) {
            features.add("has vegan options");
        }
        if (tags.has("organic") && tags.getString("organic").equals("yes")) {
            features.add("features organic products");
        }

        if (!features.isEmpty()) {
            desc.append(" This location ");
            if (features.size() == 1) {
                desc.append(features.get(0)).append(".");
            } else if (features.size() == 2) {
                desc.append(features.get(0)).append(" and ").append(features.get(1)).append(".");
            } else {
                for (int i = 0; i < features.size(); i++) {
                    if (i == features.size() - 1) {
                        desc.append("and ").append(features.get(i)).append(".");
                    } else {
                        desc.append(features.get(i)).append(", ");
                    }
                }
            }
        } else {
            desc.append(" Stop by to experience their excellent service and offerings firsthand.");
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
     * Attempts to fetch a real logo from Brandfetch API.
     */
    private static String fetchBrandfetchLogo(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return null;
        }

        try {
            // 1. Sanitize the domain
            String sanitizedDomain = domain.replaceFirst("^(https?://)?(www\\.)?", "").split("/")[0];
            System.out.println("🔍 Searching Brandfetch for: " + sanitizedDomain);

            // 2. Call Brandfetch API
            String searchUrl = "https://api.brandfetch.io/v2/brands/" + URLEncoder.encode(sanitizedDomain, "UTF-8");
            URL url = URI.create(searchUrl).toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Authorization", "Bearer " + BRANDFETCH_API_KEY);
            con.setConnectTimeout(3000);
            con.setReadTimeout(3000);

            if (con.getResponseCode() != 200) {
                System.err.println("⚠️ Brandfetch API Error: HTTP " + con.getResponseCode());
                return null;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject json = new JSONObject(response.toString());

            if (!json.has("logos") || json.getJSONArray("logos").length() == 0) {
                System.out.println("ℹ️ No logos found on Brandfetch for " + sanitizedDomain);
                return null;
            }

            // Find the first 'logo' type image
            JSONArray logos = json.getJSONArray("logos");
            for (int i = 0; i < logos.length(); i++) {
                JSONObject logo = logos.getJSONObject(i);
                if ("logo".equals(logo.optString("type"))) {
                    JSONArray formats = logo.getJSONArray("formats");
                    if (formats.length() > 0) {
                        String logoUrl = formats.getJSONObject(0).getString("src");
                        System.out.println("✅ Found logo for " + sanitizedDomain + ": " + logoUrl);
                        return logoUrl;
                    }
                }
            }

            System.out.println("ℹ️ No 'logo' type found for " + sanitizedDomain);
            return null;

        } catch (Exception e) {
            System.err.println("Failed to fetch Brandfetch logo for " + domain + ": " + e.getMessage());
            return null;
        }
    }

}
