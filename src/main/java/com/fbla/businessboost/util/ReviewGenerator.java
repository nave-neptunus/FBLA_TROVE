package com.fbla.businessboost.util;

import com.fbla.businessboost.model.Review;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates realistic, deterministic placeholder reviews for businesses
 * that do not yet have any user-submitted reviews.
 *
 * <p>Reviews are seeded by {@code businessName.hashCode()} so the same
 * business always receives the same set of generated reviews, ensuring
 * a consistent user experience across sessions.</p>
 */
public class ReviewGenerator {

    // ── Reviewer name pool (20 names) ────────────────────────────────
    private static final String[] REVIEWER_NAMES = {
        "Sarah M.", "James T.", "Maria L.", "David K.", "Emily R.",
        "Michael B.", "Jessica W.", "Robert H.", "Ashley C.", "Daniel P.",
        "Sophia N.", "Chris G.", "Olivia F.", "Andrew S.", "Hannah J.",
        "Kevin D.", "Rachel V.", "Brian A.", "Lauren E.", "Tyler Q."
    };

    // ── Food review templates (18) ───────────────────────────────────
    private static final String[] FOOD_POSITIVE = {
        "Absolutely love %s! The food is incredible and the staff always makes you feel welcome. Best spot in town.",
        "The portions at %s are generous and everything tastes fresh. Our family's go-to place for dinner.",
        "%s never disappoints. Amazing flavors, cozy atmosphere, and the desserts are out of this world!",
        "Had the best meal of the year at %s. The chef really knows what they're doing. Highly recommend!",
        "We eat at %s at least twice a month. Consistently great food and friendly service every single time.",
        "The atmosphere at %s is perfect for a date night. Delicious food and wonderful ambiance."
    };

    private static final String[] FOOD_NEUTRAL = {
        "Decent food at %s. Nothing extraordinary, but solid options and fair prices for the area.",
        "%s is a reliable choice when you're not sure where to eat. Good portions, average wait times.",
        "The menu at %s has some hits and some misses. The appetizers are better than the entrees in my opinion.",
        "Went to %s with friends — food was fine, service was a little slow but they were busy. Would go again.",
        "Pretty standard experience at %s. The food is good but the prices have gone up recently.",
        "%s has a nice vibe but the food could use more seasoning. Still a pleasant place to grab a meal."
    };

    private static final String[] FOOD_CRITICAL = {
        "%s was okay but the wait was too long for what we got. Food was lukewarm when it arrived.",
        "Expected more from %s based on the hype. Portions were small and the service was inattentive.",
        "Not my favorite experience at %s. The food was mediocre and the place could use a good cleaning.",
        "The food at %s tastes like it used to be better. Quality seems to have dropped off lately.",
        "Overpriced for what you get at %s. Decent taste but definitely not worth the premium.",
        "%s needs to work on consistency. Some visits are great, others are disappointing."
    };

    // ── Retail review templates (18) ─────────────────────────────────
    private static final String[] RETAIL_POSITIVE = {
        "Love shopping at %s! Great selection and the staff is always helpful. My favorite local store.",
        "%s has an amazing variety of products. Found exactly what I needed and the prices were fair.",
        "The team at %s really knows their products. They helped me pick the perfect gift. Five stars!",
        "Always a pleasant experience at %s. Clean store, well-organized, and they carry quality brands.",
        "%s is a gem. Unique finds you won't see at big box stores and the customer service is top notch.",
        "I keep coming back to %s because they genuinely care about their customers. Great store layout too."
    };

    private static final String[] RETAIL_NEUTRAL = {
        "Decent selection at %s but prices are a bit higher than I expected. Staff was friendly though.",
        "%s has some nice items but the store can feel cramped during busy hours. Worth a visit still.",
        "Found a few good things at %s. Not the widest selection but what they carry is decent quality.",
        "Average shopping experience at %s. Nothing wrong, but nothing that really stood out either.",
        "%s is fine for quick errands. The checkout process could be faster during peak times.",
        "The products at %s are okay. Some items feel overpriced compared to online alternatives."
    };

    private static final String[] RETAIL_CRITICAL = {
        "Limited selection at %s and the prices don't match the quality. Staff seemed disinterested.",
        "%s could really benefit from better organization. Hard to find what you're looking for.",
        "Was hoping for more from %s. The store felt dated and many items were out of stock.",
        "Not impressed with %s. Prices are too high for the quality and the return policy is strict.",
        "%s has potential but needs work. The layout is confusing and the lighting is harsh.",
        "Visited %s recently and left empty-handed. Not much that caught my eye unfortunately."
    };

    // ── Services review templates (18) ───────────────────────────────
    private static final String[] SERVICES_POSITIVE = {
        "Had a great experience at %s. Professional and thorough. Will definitely be coming back.",
        "%s exceeded my expectations. Punctual, efficient, and the quality of work was outstanding.",
        "Can't recommend %s enough! They did an amazing job and the pricing was very reasonable.",
        "The team at %s is incredibly skilled. They finished ahead of schedule and the results were perfect.",
        "%s provides top-tier service. They communicated clearly throughout the whole process.",
        "Excellent work by %s. They really listen to what you need and deliver beyond expectations."
    };

    private static final String[] SERVICES_NEUTRAL = {
        "%s did a solid job overall. Took a bit longer than quoted but the end result was acceptable.",
        "Decent service from %s. Nothing spectacular but they got the job done at a fair price.",
        "Used %s for the first time. Communication could be better but the work quality was fine.",
        "%s is a reasonable option. Not the cheapest, not the most expensive. Average turnaround time.",
        "The work from %s was satisfactory. A few minor issues but they addressed them when I pointed them out.",
        "%s was okay. Showed up on time and did the job. Would consider using them again for smaller projects."
    };

    private static final String[] SERVICES_CRITICAL = {
        "%s took much longer than promised. The quality was decent but the delays were frustrating.",
        "Mixed feelings about %s. The pricing was higher than the estimate and communication was lacking.",
        "Wouldn't rush back to %s. The work was passable but they missed some details I specifically asked for.",
        "%s needs to improve on punctuality. Showed up late twice and had to reschedule once.",
        "The experience with %s was below average. Ended up having to get some of the work redone.",
        "Not fully satisfied with %s. The final result was okay but the process was stressful."
    };

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // Prevent instantiation
    private ReviewGenerator() { }

    /**
     * Generates a deterministic list of 3-5 reviews for the given business.
     *
     * @param businessName the name of the business (used for seeding and display)
     * @param category     business category — should contain "food", "retail",
     *                     or "service" (case-insensitive); defaults to services
     * @param rating       the business's overall star rating (1.0 – 5.0)
     * @return an unmodifiable list of generated {@link Review} objects
     */
    public static List<Review> generateReviews(String businessName, String category, double rating) {
        Random rng = new Random(businessName.hashCode());

        int reviewCount = 3 + rng.nextInt(3); // 3, 4, or 5

        // Determine which template set to use based on category
        String[][] templateSets = pickTemplateSets(category);

        List<Review> reviews = new ArrayList<>();
        for (int i = 0; i < reviewCount; i++) {
            // Individual review rating: base ±0.5, clamped to [1.0, 5.0]
            double offset = (rng.nextDouble() - 0.5); // range -0.5 to +0.5
            double reviewRating = Math.round(Math.max(1.0, Math.min(5.0, rating + offset)) * 2.0) / 2.0;

            // Choose template bucket by rating
            String[] bucket = pickBucket(templateSets, reviewRating);
            String template = bucket[rng.nextInt(bucket.length)];
            String text = String.format(template, businessName);

            // Pick a reviewer name (avoid repeats within the same set)
            String reviewer = REVIEWER_NAMES[rng.nextInt(REVIEWER_NAMES.length)];

            // Generate a timestamp 1-6 months in the past
            String timestamp = generateTimestamp(rng);

            // Prepend reviewer name to the review text for display
            String fullText = reviewer + ": " + text;

            reviews.add(new Review(businessName, reviewRating, fullText, timestamp));
        }

        return List.copyOf(reviews);
    }

    // ── Private helpers ──────────────────────────────────────────────

    /**
     * Selects positive / neutral / critical template arrays based on category.
     * Returns String[3][] — index 0 = positive, 1 = neutral, 2 = critical.
     */
    private static String[][] pickTemplateSets(String category) {
        if (category == null) {
            return new String[][] { SERVICES_POSITIVE, SERVICES_NEUTRAL, SERVICES_CRITICAL };
        }
        String cat = category.toLowerCase();
        if (cat.contains("food") || cat.contains("restaurant") || cat.contains("café")
                || cat.contains("bakery") || cat.contains("coffee")) {
            return new String[][] { FOOD_POSITIVE, FOOD_NEUTRAL, FOOD_CRITICAL };
        } else if (cat.contains("retail") || cat.contains("shop") || cat.contains("store")
                || cat.contains("boutique") || cat.contains("market")) {
            return new String[][] { RETAIL_POSITIVE, RETAIL_NEUTRAL, RETAIL_CRITICAL };
        } else {
            return new String[][] { SERVICES_POSITIVE, SERVICES_NEUTRAL, SERVICES_CRITICAL };
        }
    }

    /**
     * Picks the appropriate review-tone bucket based on the individual
     * review's star rating.
     */
    private static String[] pickBucket(String[][] sets, double reviewRating) {
        if (reviewRating >= 4.0) {
            return sets[0]; // positive
        } else if (reviewRating >= 3.0) {
            return sets[1]; // neutral
        } else {
            return sets[2]; // critical
        }
    }

    /**
     * Generates an ISO-formatted timestamp 1-6 months in the past
     * using the supplied RNG for determinism.
     */
    private static String generateTimestamp(Random rng) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 24, 12, 0, 0); // fixed anchor for determinism
        int daysAgo = 30 + rng.nextInt(151); // 30 – 180 days (≈ 1-6 months)
        int hour = 8 + rng.nextInt(13);       // 8:00 – 20:00
        int minute = rng.nextInt(60);

        LocalDateTime reviewTime = now.minusDays(daysAgo)
                                      .withHour(hour)
                                      .withMinute(minute)
                                      .withSecond(0);
        return reviewTime.format(TIMESTAMP_FMT);
    }
}
