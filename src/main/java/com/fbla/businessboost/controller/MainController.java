package com.fbla.businessboost.controller;

import com.fbla.businessboost.model.Business;
import com.fbla.businessboost.model.Review;
import com.fbla.businessboost.util.DataManager;
import com.fbla.businessboost.util.RecommendationEngine;
import com.fbla.businessboost.util.ReportGenerator;
import com.fbla.businessboost.util.SupabaseClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main controller for the Commercio dashboard.
 * Manages the tabbed UI (Discover, My Impact, Smart Search),
 * card grid, favorites, reviews, charts, recommendations, and themes.
 *
 * @author FBLA Development Team
 * @version 3.0
 */
public class MainController {

    // ==================== FXML Injected Components ====================

    // --- Header ---
    @FXML
    private TextField zipCodeField;
    @FXML
    private Button themeToggleButton;

    // --- Tab Pane ---
    @FXML
    private TabPane mainTabPane;

    // --- Discover Tab ---
    @FXML
    private FlowPane businessCardGrid;
    @FXML
    private ComboBox<String> categoryFilter;
    @FXML
    private ListView<Business> favoritesList;
    @FXML
    private ListView<String> dealsPane;
    @FXML
    private Label selectedBusinessLabel;
    @FXML
    private Label businessDescriptionLabel;
    @FXML
    private Label businessPhoneLabel;
    @FXML
    private Label businessWebsiteLabel;
    @FXML
    private Label businessHoursLabel;
    @FXML
    private ImageView detailImageView;
    @FXML
    private HBox resultBanner;
    @FXML
    private Label resultBannerLabel;
    @FXML
    private VBox recommendationSection;
    @FXML
    private HBox recommendationCards;

    // --- My Impact Tab ---
    @FXML
    private BarChart<String, Number> categoryBarChart;
    @FXML
    private PieChart ratingPieChart;
    @FXML
    private Label statTotalFavorites;
    @FXML
    private Label statAvgRating;
    @FXML
    private Label statReviewsDone;
    @FXML
    private Label statTopCategory;

    // --- Smart Search Tab ---
    @FXML
    private TextField smartSearchField;
    @FXML
    private FlowPane smartSearchResults;
    @FXML
    private FlowPane tagCloudPane;

    // ==================== Data Collections ====================

    /** Master list of all businesses */
    private ObservableList<Business> allBusinesses;

    /** Separate list of ALL favorited businesses (drives the sidebar) */
    private ObservableList<Business> favoritedBusinesses;

    /** Currently selected business for the detail pane */
    private Business selectedBusiness;

    /** Whether to sort by rating descending */
    private boolean sortByRating = false;

    /** Whether dark mode is active */
    private boolean darkMode = false;

    // ==================== Initialization ====================

    /**
     * Initializes the controller after FXML loading.
     */
    @FXML
    public void initialize() {
        // Apply initial light stylesheet programmatically
        Platform.runLater(() -> {
            String lightCss = getClass().getResource("/styles.css").toExternalForm();
            themeToggleButton.getScene().getRoot().getStylesheets().add(lightCss);
        });

        // Fetch user favorites from local storage as initial fallback
        String currentUser = com.fbla.businessboost.util.SessionManager.getCurrentUser();
        List<String> userFavorites = com.fbla.businessboost.util.SessionManager.getFavorites();

        // Merge in favorites from Supabase (async, but block briefly for initial load)
        if (currentUser != null) {
            try {
                List<String> supabaseFavs = SupabaseClient.getUserFavorites(currentUser);
                for (String fav : supabaseFavs) {
                    if (!userFavorites.contains(fav)) {
                        userFavorites.add(fav);
                    }
                }
                // Update local cache with merged list
                com.fbla.businessboost.util.SessionManager.saveFavorites(userFavorites);
            } catch (Exception ex) {
                System.err.println("Could not fetch Supabase favorites, using local cache: " + ex.getMessage());
            }
        }

        // Load mock data
        ObservableList<Business> rawData = DataManager.initializeMockData();

        // Separate list for favorites sidebar
        favoritedBusinesses = FXCollections.observableArrayList(
                business -> new javafx.beans.Observable[] { business.favoriteProperty() });

        // Apply favorites and attach listener
        for (Business b : rawData) {
            if (userFavorites.contains(b.getName())) {
                b.setFavorite(true);
                favoritedBusinesses.add(b);
            }
            attachFavoriteListener(b);
        }

        allBusinesses = FXCollections.observableArrayList(
                business -> new javafx.beans.Observable[] { business.favoriteProperty() });
        allBusinesses.addAll(rawData);

        // Initialize category filter dropdown
        setupCategoryFilter();

        // Configure favorites sidebar
        setupFavoritesList();

        // Build the card grid
        refreshCardGrid();

        // Set detail pane placeholder
        clearDetailPane();

        // Build tag cloud on Smart Search tab
        buildTagCloud();

        // Update charts when tab is selected
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null && "📊 My Impact".equals(newTab.getText())) {
                refreshCharts();
            }
        });

        // Update recommendations
        refreshRecommendations();
    }

    // ==================== Card Grid ====================

    /**
     * Rebuilds the entire card grid from the current data + filters + sort.
     */
    private void refreshCardGrid() {
        businessCardGrid.getChildren().clear();

        String selectedCategory = categoryFilter.getValue();
        List<Business> filtered = allBusinesses.stream()
                .filter(b -> "All".equals(selectedCategory) || b.getCategory().equals(selectedCategory))
                .collect(Collectors.toList());

        if (sortByRating) {
            filtered.sort(Comparator.comparingDouble(Business::getRating).reversed());
        }

        for (Business b : filtered) {
            businessCardGrid.getChildren().add(createBusinessCard(b));
        }

        // Show empty state if no results
        if (filtered.isEmpty() && !"All".equals(selectedCategory)) {
            showResultBanner("No businesses match \"" + selectedCategory + "\" — try selecting \"All\"",
                    "result-banner-warning");
        }
    }

    /**
     * Creates a DoorDash-style business card VBox for the grid.
     *
     * @param business The business to display
     * @return A VBox node representing the card
     */
    private VBox createBusinessCard(Business business) {
        VBox card = new VBox(0);
        card.getStyleClass().add("business-card");
        card.setPrefWidth(260);
        card.setMaxWidth(260);
        card.setCursor(Cursor.HAND);

        // ---- Image ----
        StackPane imageContainer = new StackPane();
        imageContainer.getStyleClass().add("card-image-container");
        imageContainer.setPrefHeight(160);
        imageContainer.setMinHeight(160);
        imageContainer.setMaxHeight(160);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(260);
        imageView.setFitHeight(160);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);

        // Rounded clip for the image
        Rectangle clip = new Rectangle(260, 160);
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        imageView.setClip(clip);

        // Load image async
        String imgUrl = business.getImageUrl();
        if (imgUrl != null && !imgUrl.isEmpty()) {
            try {
                Image image = new Image(imgUrl, 260, 160, false, true, true);
                imageView.setImage(image);
            } catch (Exception e) {
                imageView.setStyle("-fx-background-color: #e0e0e0;");
            }
        }

        // Heart button overlay
        Button heartBtn = new Button(business.isFavorite() ? "❤️" : "🤍");
        heartBtn.getStyleClass().add("card-heart-button");
        heartBtn.setOnAction(e -> {
            business.setFavorite(!business.isFavorite());
            heartBtn.setText(business.isFavorite() ? "❤️" : "🤍");
        });
        StackPane.setAlignment(heartBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(heartBtn, new Insets(8, 8, 0, 0));

        imageContainer.getChildren().addAll(imageView, heartBtn);

        // ---- Info section ----
        VBox infoBox = new VBox(4);
        infoBox.getStyleClass().add("card-info");
        infoBox.setPadding(new Insets(10, 12, 12, 12));

        // Name
        Label nameLabel = new Label(business.getName());
        nameLabel.getStyleClass().add("card-title");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(236);

        // Category + Price row
        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label categoryBadge = new Label(business.getCategory());
        categoryBadge.getStyleClass().add("card-category-badge");
        switch (business.getCategory()) {
            case "Food":
                categoryBadge.getStyleClass().add("badge-food");
                break;
            case "Retail":
                categoryBadge.getStyleClass().add("badge-retail");
                break;
            case "Services":
                categoryBadge.getStyleClass().add("badge-services");
                break;
        }

        Label priceLabel = new Label(business.getPrice());
        priceLabel.getStyleClass().add("card-price");

        metaRow.getChildren().addAll(categoryBadge, priceLabel);

        // Rating row
        HBox ratingRow = new HBox(4);
        ratingRow.setAlignment(Pos.CENTER_LEFT);

        Label starsLabel = new Label(buildStarString(business.getRating()));
        starsLabel.getStyleClass().add("card-rating-stars");

        Label ratingLabel = new Label(String.format("%.1f", business.getRating()));
        ratingLabel.getStyleClass().add("card-rating-number");

        ratingRow.getChildren().addAll(starsLabel, ratingLabel);

        // Address
        Label addressLabel = new Label(business.getAddress());
        addressLabel.getStyleClass().add("card-address");
        addressLabel.setWrapText(true);
        addressLabel.setMaxWidth(236);

        infoBox.getChildren().addAll(nameLabel, metaRow, ratingRow, addressLabel);

        card.getChildren().addAll(imageContainer, infoBox);

        // Click handler → show business details
        card.setOnMouseClicked(e -> selectBusiness(business));

        return card;
    }

    /**
     * Builds a star string like "★★★★☆" from a rating.
     */
    private String buildStarString(double rating) {
        StringBuilder stars = new StringBuilder();
        int full = (int) Math.floor(rating);
        for (int i = 0; i < full; i++)
            stars.append("★");
        if (rating - full >= 0.5)
            stars.append("½");
        int empty = 5 - full - (rating - full >= 0.5 ? 1 : 0);
        for (int i = 0; i < empty; i++)
            stars.append("☆");
        return stars.toString();
    }

    // ==================== Selection & Detail Pane ====================

    /**
     * Selects a business and updates the detail pane.
     */
    private void selectBusiness(Business business) {
        this.selectedBusiness = business;
        selectedBusinessLabel.setText(business.getName());

        String desc = business.getDescription();
        businessDescriptionLabel.setText(desc != null && !desc.trim().isEmpty() ? desc : "");

        String phone = business.getPhone();
        businessPhoneLabel.setText(phone != null && !phone.isEmpty() ? "📞 " + phone : "📞 Phone unavailable");

        String website = business.getWebsite();
        businessWebsiteLabel
                .setText(website != null && !website.isEmpty() ? "🌐 " + website : "🌐 Website unavailable");

        String hours = business.getHours();
        businessHoursLabel.setText(hours != null && !hours.isEmpty() ? "🕒 " + hours : "🕒 Hours unavailable");

        // Load detail image
        String imgUrl = business.getImageUrl();
        if (imgUrl != null && !imgUrl.isEmpty()) {
            try {
                Image img = new Image(imgUrl, 290, 180, false, true, true);
                detailImageView.setImage(img);
            } catch (Exception e) {
                detailImageView.setImage(null);
            }
        } else {
            detailImageView.setImage(null);
        }

        // Show local deals first
        dealsPane.setItems(business.getDeals());

        // Fetch reviews from Supabase in background
        javafx.concurrent.Task<List<Review>> fetchReviewsTask = new javafx.concurrent.Task<>() {
            @Override
            protected List<Review> call() throws Exception {
                return SupabaseClient.getReviewsForBusiness(business.getName());
            }
        };

        fetchReviewsTask.setOnSucceeded(e -> {
            List<Review> reviews = fetchReviewsTask.getValue();
            if (reviews != null && !reviews.isEmpty()) {
                Platform.runLater(() -> {
                    if (selectedBusiness == business) {
                        ObservableList<String> combined = FXCollections.observableArrayList(business.getDeals());
                        combined.add("--- Community Reviews ---");
                        for (Review r : reviews) {
                            combined.add(r.toString());
                        }
                        dealsPane.setItems(combined);
                    }
                });
            }
        });

        new Thread(fetchReviewsTask).start();
    }

    /**
     * Clears the detail pane.
     */
    private void clearDetailPane() {
        selectedBusinessLabel.setText("Select a business");
        businessDescriptionLabel.setText("Click on a business card to view details, deals, and reviews.");
        detailImageView.setImage(null);
        dealsPane.setItems(FXCollections.observableArrayList());
    }

    // ==================== Filters & Sort ====================

    private void setupCategoryFilter() {
        categoryFilter.setItems(FXCollections.observableArrayList(
                "All", "Food", "Retail", "Services"));
        categoryFilter.setValue("All");
        categoryFilter.setOnAction(event -> {
            hideResultBanner();
            refreshCardGrid();
        });
    }

    @FXML
    private void handleSortByRating() {
        sortByRating = !sortByRating;
        refreshCardGrid();
    }

    @FXML
    private void handleCategoryFilter() {
        hideResultBanner();
        refreshCardGrid();
    }

    // ==================== Favorites ====================

    private void setupFavoritesList() {
        favoritesList.setItems(favoritedBusinesses);
        favoritesList.setCellFactory(listView -> new ListCell<Business>() {
            @Override
            protected void updateItem(Business business, boolean empty) {
                super.updateItem(business, empty);
                if (empty || business == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(String.format("%s (%.1f⭐)", business.getName(), business.getRating()));
                }
            }
        });

        favoritesList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        selectBusiness(newVal);
                    }
                });
    }

    private void attachFavoriteListener(Business business) {
        business.favoriteProperty().addListener((obs, wasFavorite, isNowFavorite) -> {
            if (isNowFavorite) {
                if (!favoritedBusinesses.contains(business)) {
                    favoritedBusinesses.add(business);
                }
            } else {
                favoritedBusinesses.remove(business);
            }

            // Update recommendations when favorites change
            refreshRecommendations();

            // Persist locally
            if (isNowFavorite) {
                com.fbla.businessboost.util.SessionManager.addFavorite(business.getName());
            } else {
                com.fbla.businessboost.util.SessionManager.removeFavorite(business.getName());
            }

            // Sync to Supabase in background
            String user = com.fbla.businessboost.util.SessionManager.getCurrentUser();
            if (user != null) {
                new Thread(() -> {
                    if (isNowFavorite) {
                        SupabaseClient.addFavorite(user, business.getName());
                    } else {
                        SupabaseClient.removeFavorite(user, business.getName());
                    }
                }).start();
            }
        });
    }

    // ==================== Recommendations ====================

    /**
     * Refreshes the "You might also like..." section using the recommendation
     * engine.
     */
    private void refreshRecommendations() {
        recommendationCards.getChildren().clear();

        List<Business> recommendations = RecommendationEngine.getRecommendations(
                new ArrayList<>(favoritedBusinesses), new ArrayList<>(allBusinesses));

        if (recommendations.isEmpty()) {
            recommendationSection.setVisible(false);
            recommendationSection.setManaged(false);
            return;
        }

        recommendationSection.setVisible(true);
        recommendationSection.setManaged(true);

        for (Business rec : recommendations) {
            VBox miniCard = createMiniCard(rec);
            recommendationCards.getChildren().add(miniCard);
        }
    }

    /**
     * Creates a compact recommendation card.
     */
    private VBox createMiniCard(Business business) {
        VBox card = new VBox(4);
        card.getStyleClass().add("mini-card");
        card.setPrefWidth(200);
        card.setMaxWidth(200);
        card.setCursor(Cursor.HAND);
        card.setPadding(new Insets(10));

        Label name = new Label(business.getName());
        name.getStyleClass().add("card-title");
        name.setWrapText(true);
        name.setStyle("-fx-font-size: 12px;");

        Label category = new Label(business.getCategory() + " · " + buildStarString(business.getRating()));
        category.getStyleClass().add("card-address");

        // Show matching tags
        String tagStr = business.getTags().stream().limit(3).collect(Collectors.joining(" "));
        Label tags = new Label(tagStr);
        tags.setStyle("-fx-font-size: 10px; -fx-text-fill: #5BA08C;");

        card.getChildren().addAll(name, category, tags);
        card.setOnMouseClicked(e -> selectBusiness(business));

        return card;
    }

    // ==================== My Impact (Charts) ====================

    /**
     * Refreshes the charts and stats on the My Impact tab.
     */
    private void refreshCharts() {
        // Update stats
        int totalFavs = favoritedBusinesses.size();
        statTotalFavorites.setText(String.valueOf(totalFavs));

        int reviewsDone = com.fbla.businessboost.util.SessionManager.getReviewCount();
        statReviewsDone.setText(String.valueOf(reviewsDone));

        if (totalFavs > 0) {
            double avgRating = favoritedBusinesses.stream()
                    .mapToDouble(Business::getRating)
                    .average()
                    .orElse(0.0);
            statAvgRating.setText(String.format("%.1f", avgRating));

            // Find top category
            Map<String, Long> catCount = favoritedBusinesses.stream()
                    .collect(Collectors.groupingBy(Business::getCategory, Collectors.counting()));
            String topCat = catCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("-");
            statTopCategory.setText(topCat);

            // Bar Chart: businesses by category
            categoryBarChart.getData().clear();

            // By putting each category in its own Series, JavaFX will automatically
            // color them differently instead of coloring them all as a single "Favorites"
            // series.
            catCount.forEach((cat, count) -> {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName(cat);

                // Add the data point (empty string for x-axis so they cluster nicely, or use
                // cat)
                XYChart.Data<String, Number> dataNode = new XYChart.Data<>(cat, count);
                dataNode.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        if ("Food".equals(cat)) {
                            newNode.setStyle("-fx-bar-fill: #E67E22;"); // Orange
                        } else if ("Retail".equals(cat)) {
                            newNode.setStyle("-fx-bar-fill: #3498DB;"); // Blue
                        } else if ("Services".equals(cat)) {
                            newNode.setStyle("-fx-bar-fill: #9B59B6;"); // Purple
                        } else {
                            newNode.setStyle("-fx-bar-fill: #5BA08C;"); // Default Commercio Green
                        }
                    }
                });

                series.getData().add(dataNode);

                categoryBarChart.getData().add(series);
            });

            // Pie Chart: rating distribution
            ratingPieChart.getData().clear();
            Map<String, Long> ratingDist = favoritedBusinesses.stream()
                    .collect(Collectors.groupingBy(b -> {
                        double r = b.getRating();
                        if (r >= 4.5)
                            return "★★★★★ (4.5+)";
                        if (r >= 4.0)
                            return "★★★★ (4.0-4.4)";
                        if (r >= 3.5)
                            return "★★★½ (3.5-3.9)";
                        return "★★★ (< 3.5)";
                    }, Collectors.counting()));
            ratingDist.forEach((label, count) -> {
                PieChart.Data slice = new PieChart.Data(label, count);
                ratingPieChart.getData().add(slice);

                slice.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        if (label.contains("4.5+"))
                            newNode.setStyle("-fx-pie-color: #2ECC71;"); // Green
                        else if (label.contains("4.0-4.4"))
                            newNode.setStyle("-fx-pie-color: #F1C40F;"); // Yellow
                        else if (label.contains("3.5-3.9"))
                            newNode.setStyle("-fx-pie-color: #E67E22;"); // Orange
                        else
                            newNode.setStyle("-fx-pie-color: #E74C3C;"); // Red
                    }
                });
            });

            // Save stats snapshot to Supabase
            String user = com.fbla.businessboost.util.SessionManager.getCurrentUser();
            if (user != null) {
                new Thread(() -> {
                    SupabaseClient.saveUserStats(user, totalFavs, avgRating, topCat);
                }).start();
            }
        } else {
            statAvgRating.setText("0.0");
            statTopCategory.setText("-");
            categoryBarChart.getData().clear();
            ratingPieChart.getData().clear();
        }
    }

    // ==================== Smart Search ====================

    /**
     * Handles fuzzy search by name, category, or tags.
     */
    @FXML
    private void handleSmartSearch() {
        String query = smartSearchField.getText().trim().toLowerCase();
        smartSearchResults.getChildren().clear();

        if (query.isEmpty()) {
            // Show all businesses
            for (Business b : allBusinesses) {
                smartSearchResults.getChildren().add(createBusinessCard(b));
            }
            return;
        }

        List<Business> results = allBusinesses.stream()
                .filter(b -> {
                    // Match by name (fuzzy)
                    if (b.getName().toLowerCase().contains(query))
                        return true;
                    // Match by category
                    if (b.getCategory().toLowerCase().contains(query))
                        return true;
                    // Match by description
                    if (b.getDescription() != null && b.getDescription().toLowerCase().contains(query))
                        return true;
                    // Match by tags
                    for (String tag : b.getTags()) {
                        if (tag.toLowerCase().contains(query))
                            return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());

        if (results.isEmpty()) {
            Label noResults = new Label("No businesses match \"" + query + "\". Try another search term.");
            noResults.getStyleClass().add("loading-label");
            smartSearchResults.getChildren().add(noResults);
        } else {
            for (Business b : results) {
                smartSearchResults.getChildren().add(createBusinessCard(b));
            }
        }
    }

    /**
     * Builds clickable tag chips from all businesses.
     */
    private void buildTagCloud() {
        Set<String> allTags = new TreeSet<>();
        for (Business b : allBusinesses) {
            allTags.addAll(b.getTags());
        }

        for (String tag : allTags) {
            Button tagBtn = new Button(tag);
            tagBtn.getStyleClass().add("tag-chip");
            tagBtn.setOnAction(e -> {
                smartSearchField.setText(tag);
                handleSmartSearch();
            });
            tagCloudPane.getChildren().add(tagBtn);
        }
    }

    // ==================== Theme Toggle ====================

    /**
     * Toggles between light and dark mode by swapping CSS stylesheets.
     * Uses root node stylesheets (not scene) for reliable toggling.
     */
    @FXML
    private void handleThemeToggle() {
        ObservableList<String> stylesheets = themeToggleButton.getScene().getRoot().getStylesheets();

        String lightCss = getClass().getResource("/styles.css").toExternalForm();
        String darkCss = getClass().getResource("/styles-dark.css").toExternalForm();

        stylesheets.clear();

        if (darkMode) {
            stylesheets.add(lightCss);
            themeToggleButton.setText("🌙");
            darkMode = false;
        } else {
            stylesheets.add(darkCss);
            themeToggleButton.setText("☀️");
            darkMode = true;
        }
    }

    /**
     * Logs out the current user, clears saved session, and returns to login.
     */
    @FXML
    private void handleLogout() {
        com.fbla.businessboost.util.SessionManager.logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/LoginView.fxml"));
            Stage stage = (Stage) themeToggleButton.getScene().getWindow();
            stage.setTitle("Commercio - Login");
            stage.setScene(new Scene(loader.load(), 500, 600));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== Zip Search ====================

    @FXML
    private void handleZipSearch() {
        String zipCode = zipCodeField.getText();
        if (zipCode == null || zipCode.trim().isEmpty()) {
            showResultBanner("⚠ Please enter a zip code.", "result-banner-warning");
            return;
        }

        hideResultBanner();
        businessCardGrid.getChildren().clear();

        // Show loading state
        Label loadingLabel = new Label("🔍 Searching businesses...");
        loadingLabel.getStyleClass().add("loading-label");
        businessCardGrid.getChildren().add(loadingLabel);

        String trimmedZip = zipCode.trim();

        javafx.concurrent.Task<ObservableList<Business>> fetchTask = new javafx.concurrent.Task<>() {
            @Override
            protected ObservableList<Business> call() throws Exception {
                return com.fbla.businessboost.util.BusinessFetcher.fetchBusinessesByZipCode(trimmedZip);
            }
        };

        fetchTask.setOnSucceeded(e -> {
            ObservableList<Business> results = fetchTask.getValue();
            if (results.isEmpty()) {
                businessCardGrid.getChildren().clear();
                showResultBanner("No businesses found near " + trimmedZip + " — try a different zip code.",
                        "result-banner-warning");
            } else {
                new Thread(() -> {
                    List<String> userFavoritesList = com.fbla.businessboost.util.SessionManager.getFavorites();
                    Platform.runLater(() -> {
                        processZipResults(results, userFavoritesList);
                        showResultBanner("✓ Found " + results.size() + " businesses near " + trimmedZip,
                                "result-banner-success");
                    });
                }).start();
            }
        });

        fetchTask.setOnFailed(e -> {
            businessCardGrid.getChildren().clear();
            Throwable ex = fetchTask.getException();
            String msg = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "";
            if (msg.toLowerCase().contains("invalid zip")) {
                showResultBanner("❌ Invalid zip code — please try again.", "result-banner-error");
            } else {
                showResultBanner("❌ Search failed — please check your connection and try again.",
                        "result-banner-error");
            }
            System.err.println("Zip Search fetchTask failed:");
            if (ex != null)
                ex.printStackTrace();
        });

        new Thread(fetchTask).start();
    }

    private void processZipResults(ObservableList<Business> results, List<String> userFavoritesList) {
        allBusinesses.clear();

        for (Business newBiz : results) {
            if (userFavoritesList.contains(newBiz.getName())) {
                newBiz.setFavorite(true);
            }
            attachFavoriteListener(newBiz);
            allBusinesses.add(newBiz);
        }

        categoryFilter.setValue("All");
        sortByRating = false;
        refreshCardGrid();
        refreshRecommendations();

        // Rebuild tag cloud with new data
        tagCloudPane.getChildren().clear();
        buildTagCloud();
    }

    // ==================== Reviews ====================

    @FXML
    private void handleAddReview() {
        if (selectedBusiness == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection",
                    "Please select a business to review.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ReviewDialog.fxml"));
            Scene scene = new Scene(loader.load());

            // Apply current theme to dialog
            if (darkMode) {
                String darkCss = getClass().getResource("/styles-dark.css").toExternalForm();
                scene.getStylesheets().add(darkCss);
            }

            ReviewDialogController controller = loader.getController();
            controller.setBusiness(selectedBusiness);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Submit Review - " + selectedBusiness.getName());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();

            // Refresh detail pane to show new review
            selectBusiness(selectedBusiness);

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to load review dialog: " + e.getMessage());
        }
    }

    // ==================== Reports ====================

    @FXML
    private void handleGenerateReport() {
        String username = com.fbla.businessboost.util.SessionManager.getCurrentUser();
        ReportGenerator.generateReport(username, FXCollections.observableArrayList(favoritedBusinesses));
    }

    // ==================== Inline Result Banner ====================

    private void showResultBanner(String message, String styleClass) {
        resultBannerLabel.setText(message);
        resultBanner.getStyleClass().removeAll(
                "result-banner-success", "result-banner-warning", "result-banner-error");
        resultBanner.getStyleClass().add(styleClass);
        resultBanner.setVisible(true);
        resultBanner.setManaged(true);
    }

    @FXML
    private void hideResultBanner() {
        resultBanner.setVisible(false);
        resultBanner.setManaged(false);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
