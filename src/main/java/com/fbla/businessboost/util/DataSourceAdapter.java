package com.fbla.businessboost.util;

import com.fbla.businessboost.model.Business;
import java.util.List;

/**
 * Tier 4.2 - Pluggable Data-Source Adapters.
 * Defines the contract for fetching businesses from any backend
 * (Mock, CSV, Supabase, LocalDB).
 */
public interface DataSourceAdapter {
    /**
     * Fetches a list of businesses from the underlying data source.
     */
    List<Business> fetchBusinesses();
}
