package com.fbla.businessboost.util;

import com.fbla.businessboost.model.Business;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Tier 3.5 - Smart Coupon Engine.
 * Generates single-use redemption codes with expiration countdowns.
 * Logs redemptions to prove anti-fraud mechanics.
 */
public class SmartCouponEngine {

    private static final Map<String, List<Coupon>> businessCoupons = new HashMap<>();
    private static final Set<String> redeemedCodes = new HashSet<>();

    public static class Coupon {
        public String code;
        public String description;
        public long expiryTimeMs;

        public Coupon(String code, String description, long expiryTimeMs) {
            this.code = code;
            this.description = description;
            this.expiryTimeMs = expiryTimeMs;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTimeMs;
        }

        public String getFormattedExpiry() {
            long remaining = expiryTimeMs - System.currentTimeMillis();
            if (remaining <= 0) return "Expired";
            
            long hours = remaining / (1000 * 60 * 60);
            long minutes = (remaining / (1000 * 60)) % 60;
            return String.format("Expires in %dh %dm", hours, minutes);
        }
    }

    /**
     * Retrieves or generates smart coupons for a business.
     */
    public static List<Coupon> getCouponsForBusiness(Business b) {
        if (!businessCoupons.containsKey(b.getName())) {
            List<Coupon> coupons = new ArrayList<>();
            List<String> deals = b.getDeals();
            if (deals != null) {
                for (String deal : deals) {
                    // Generate a unique alphanumeric code
                    String code = "TRV-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                    // Expire between 2 to 48 hours from now
                    long expiryOffset = (long) (Math.random() * 46 * 3600000) + 7200000;
                    coupons.add(new Coupon(code, deal, System.currentTimeMillis() + expiryOffset));
                }
            }
            businessCoupons.put(b.getName(), coupons);
        }
        return businessCoupons.get(b.getName());
    }

    /**
     * Redeems a coupon code if it is valid.
     */
    public static boolean redeemCoupon(String code) {
        if (redeemedCodes.contains(code)) {
            return false; // Already redeemed
        }
        
        for (List<Coupon> list : businessCoupons.values()) {
            for (Coupon c : list) {
                if (c.code.equals(code)) {
                    if (c.isExpired()) return false;
                    redeemedCodes.add(code);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets a simple log of redemptions for the Data Analysis / Impact dashboard.
     */
    public static int getRedeemedCount() {
        return redeemedCodes.size();
    }
}
