package com.fbla.businessboost.controller;

import com.fbla.businessboost.model.Business;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * Tier 3.4 - Local Crawl Route Planner.
 * Picks up to 3 businesses and generates a mock optimized walking route
 * and a themed itinerary.
 */
public class RoutePlannerController {

    @FXML private VBox crawlContainer;
    @FXML private Label themeLabel;
    @FXML private Label detailsLabel;

    public void setFavorites(List<Business> favorites) {
        if (favorites.size() < 2) {
            themeLabel.setText("Not Enough Favorites");
            detailsLabel.setText("Please favorite at least 2 businesses to plan a local crawl.");
            return;
        }

        // Pick up to 3 random favorites
        int count = Math.min(3, favorites.size());
        List<Business> crawlList = favorites.subList(0, count);

        // Determine Theme based on categories
        boolean hasFood = crawlList.stream().anyMatch(b -> b.getCategory().toLowerCase().contains("food") || b.getCategory().toLowerCase().contains("restaurant"));
        boolean hasRetail = crawlList.stream().anyMatch(b -> b.getCategory().toLowerCase().contains("retail") || b.getCategory().toLowerCase().contains("store"));
        
        if (hasFood && hasRetail) {
            themeLabel.setText("Theme: Dine & Shop 🛍️🍽️");
        } else if (hasFood) {
            themeLabel.setText("Theme: Foodie Adventure 🍔🍕");
        } else {
            themeLabel.setText("Theme: City Explorer 🏙️🚶");
        }

        detailsLabel.setText(String.format("Optimized walking route for %d stops. Estimated time: %d mins.", count, count * 15));

        for (int i = 0; i < count; i++) {
            Business b = crawlList.get(i);
            Label step = new Label((i + 1) + ". " + b.getName() + " (" + b.getCategory() + ")");
            step.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            
            Label desc = new Label("   📍 " + b.getAddress() + "\n   " + b.getDescription());
            desc.setWrapText(true);
            desc.setStyle("-fx-text-fill: #64748B; -fx-padding: 0 0 10 0;");
            
            crawlContainer.getChildren().addAll(step, desc);
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) themeLabel.getScene().getWindow();
        stage.close();
    }
}
