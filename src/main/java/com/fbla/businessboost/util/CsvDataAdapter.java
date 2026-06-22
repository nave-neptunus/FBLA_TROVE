package com.fbla.businessboost.util;

import com.fbla.businessboost.model.Business;
import java.util.ArrayList;
import java.util.List;

public class CsvDataAdapter implements DataSourceAdapter {

    private String csvFilePath;

    public CsvDataAdapter(String csvFilePath) {
        this.csvFilePath = csvFilePath;
    }

    @Override
    public List<Business> fetchBusinesses() {
        List<Business> businesses = new ArrayList<>();
        // In a real implementation, this would parse the CSV file at csvFilePath
        // For now, it returns an empty list to demonstrate pluggability.
        System.out.println("Loading businesses from CSV: " + csvFilePath);
        return businesses;
    }
}
