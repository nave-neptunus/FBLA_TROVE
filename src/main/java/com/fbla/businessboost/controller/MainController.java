package com.fbla.businessboost.controller;

import com.fbla.businessboost.model.Business;
import com.fbla.businessboost.model.Review;
import com.fbla.businessboost.util.DataManager;
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
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main controller for the FBLA Business Boost dashboard.
 * Manages the DoorDash-style card grid UI, filtering, sorting,
 * favorites, reviews, and report generation.
 *
 * @author FBLA Development Team
 * @version 2.0
 */
public class MainController {

    // ==================== FXML Injected Components ====================

    @FXML
    private FlowPane businessCardGrid;
    @FXML
    private TextField zipCodeField;
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
    private ImageView detailImageView;
    @FXML
    private HBox resultBanner;
    @FXML
    private Label resultBannerLabel;

    // ==================== Data Collections ====================

    /** Master list of all businesses */
    private ObservableList<Business> allBusinesses;

    /** Separate list of ALL favorited businesses (drives the sidebar) */
    private ObservableList<Business> favoritedBusinesses;

    /** Currently selected business for the detail pane */
    private Business selectedBusiness;

    /** Whether to sort by rating descending */
    private boolean sortByRating = false;

    /**
     * Initializes the controller after FXML loading.
     */
    @FXML
    public void initialize() {
        // Fetch user favorites
        String currentUser = com.fbla.businessboost.util.SessionManager.getCurrentUser();
        List<String> userFavorites = currentUser != null ? SupabaseClient.getUserFavorites(currentUser)
                : new java.util.ArrayList<>();

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
                // Fallback: gray placeholder
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

        // Hover elevation effect
        DropShadow restShadow = new DropShadow(8, Color.rgb(0, 0, 0, 0.08));
        DropShadow hoverShadow = new DropShadow(20, Color.rgb(0, 0, 0, 0.15));
        card.setEffect(restShadow);
        card.setOnMouseEntered(e -> card.setEffect(hoverShadow));
        card.setOnMouseExited(e -> card.setEffect(restShadow));

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

            // Persist to Supabase
            String username = com.fbla.businessboost.util.SessionManager.getCurrentUser();
            if (username == null)
                return;

            new Thread(() -> {
                if (isNowFavorite) {
                    SupabaseClient.addFavorite(username, business.getName());
                } else {
                    SupabaseClient.removeFavorite(username, business.getName());
                }
            }).start();
        });
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

        String currentUser = com.fbla.businessboost.util.SessionManager.getCurrentUser();
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
                    List<String> userFavoritesList = currentUser != null
                            ? SupabaseClient.getUserFavorites(currentUser)
                            : new java.util.ArrayList<>();
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
        ReportGenerator.generateReport(FXCollections.observableArrayList(favoritedBusinesses));
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
