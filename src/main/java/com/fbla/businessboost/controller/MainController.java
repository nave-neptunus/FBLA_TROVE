package com.fbla.businessboost.controller;

import com.fbla.businessboost.model.Business;
import com.fbla.businessboost.model.Review;
import com.fbla.businessboost.util.*;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
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
import javafx.util.Duration;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main controller for the Commercio dashboard.
 *
 * <p>
 * Manages the tabbed UI (Discover, My Impact, Smart Search), card grid,
 * favorites, reviews, charts, recommendations, themes, and the offline cache.
 * </p>
 *
 * <p>
 * <strong>New features in v4.0:</strong>
 * <ul>
 *   <li>Skeleton loading animation during ZIP search</li>
 *   <li>Empty state when no results / filter match</li>
 *   <li>Live client-side search (no API call)</li>
 *   <li>Distance radius slider (re-fires Overpass API)</li>
 *   <li>Static OpenStreetMap thumbnail in the detail pane</li>
 *   <li>Rich review cards (styled VBox per review)</li>
 *   <li>Local-jobs economic impact stat</li>
 *   <li>Recommendation explainability ("Because you love: #cozy #budget")</li>
 *   <li>Offline cache fallback on network failure</li>
 *   <li>Accessibility labels on all interactive elements</li>
 * </ul>
 * </p>
 *
 * @author FBLA Development Team
 * @version 4.0
 */
public class MainController {

    // ==================== FXML Injected Components ====================

    // --- Header ---
    @FXML private TextField zipCodeField;
    @FXML private Button themeToggleButton;

    // --- Tab Pane ---
    @FXML private TabPane mainTabPane;

    // --- Discover Tab ---
    @FXML private FlowPane businessCardGrid;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private TextField liveSearchField;
    @FXML private Slider distanceSlider;
    @FXML private Label distanceLabel;
    @FXML private ListView<Business> favoritesList;
    @FXML private VBox dealsContainer;
    @FXML private Label selectedBusinessLabel;
    @FXML private Label businessDescriptionLabel;
    @FXML private Label businessPhoneLabel;
    @FXML private Label businessWebsiteLabel;
    @FXML private Label businessHoursLabel;
    @FXML private ImageView detailImageView;
    @FXML private ImageView mapImageView;
    @FXML private VBox reviewsContainer;
    @FXML private VBox aspectScoresContainer;
    @FXML private Label reviewCountLabel;
    @FXML private HBox resultBanner;
    @FXML private Label resultBannerLabel;
    @FXML private VBox recommendationSection;
    @FXML private HBox recommendationCards;

    // --- My Impact Tab ---
    @FXML private BarChart<String, Number> categoryBarChart;
    @FXML private PieChart ratingPieChart;
    @FXML private Label statTotalFavorites;
    @FXML private Label statAvgRating;
    @FXML private Label statReviewsDone;
    @FXML private Label statTopCategory;
    @FXML private HBox jobsImpactBanner;
    @FXML private Label jobsCountLabel;

    // --- Smart Search Tab ---
    @FXML private TextField smartSearchField;
    @FXML private FlowPane smartSearchResults;
    @FXML private FlowPane tagCloudPane;

    // --- Leaderboard Tab ---
    @FXML private ToggleButton btnGlobalRank;
    @FXML private ToggleButton btnMonthlyRank;
    @FXML private ToggleButton btnCategoryRank;
    @FXML private ToggleButton btnStreakRank;
    @FXML private TableView<LeaderboardUser> leaderboardTable;
    @FXML private TableColumn<LeaderboardUser, Integer> colRank;
    @FXML private TableColumn<LeaderboardUser, String> colUser;
    @FXML private TableColumn<LeaderboardUser, Integer> colScore;
    @FXML private TableColumn<LeaderboardUser, String> colBadge;
    @FXML private TableColumn<LeaderboardUser, String> colStreak;
    @FXML private TableColumn<LeaderboardUser, String> colTitle;
    @FXML private Label lblUserRank;
    @FXML private Label lblUserScore;
    @FXML private Label lblUserPercentile;

    // ==================== Data ====================

    /** Master list of all loaded businesses. */
    private ObservableList<Business> allBusinesses;

    /** Sidebar favorites list (reactive via ObservableList). */
    private ObservableList<Business> favoritedBusinesses;
    private com.fbla.businessboost.util.LocalAIConcierge aiConcierge;

    /** Currently selected business for the detail pane. */
    private Business selectedBusiness;

    /** Whether to sort by rating descending. */
    private boolean sortByRating = false;

    /** Whether dark mode is currently active. */
    private boolean darkMode = false;

    /**
     * Radius values in miles corresponding to distanceSlider positions 0-3.
     * Labels: "0.5 miles", "1 mile", "2 miles", "5 miles"
     */
    private static final int[] RADIUS_VALUES = {500, 1000, 2000, 5000};
    private static final String[] RADIUS_LABELS = {"0.5 miles", "1 mile", "2 miles", "5 miles"};

    // ==================== Initialization ====================

    /**
     * Initializes the controller after FXML loading.
     * Applies the light stylesheet, loads mock data, restores favorites,
     * wires up all listeners, and seeds from onboarding preferences.
     */
    @FXML
    public void initialize() {
        // Apply initial light stylesheet
        Platform.runLater(() -> {
            String lightCss = getClass().getResource("/styles.css").toExternalForm();
            themeToggleButton.getScene().getRoot().getStylesheets().add(lightCss);
        });

        // Fetch user favorites (local + Supabase merge)
        String currentUser = SessionManager.getCurrentUser();
        List<String> userFavorites = SessionManager.getFavorites();

        if (currentUser != null) {
            try {
                List<String> supabaseFavs = SupabaseClient.getUserFavorites(currentUser);
                for (String fav : supabaseFavs) {
                    if (!userFavorites.contains(fav)) {
                        userFavorites.add(fav);
                    }
                }
                SessionManager.saveFavorites(userFavorites);
            } catch (Exception ex) {
                System.err.println("Could not fetch Supabase favorites: " + ex.getMessage());
            }
        }

        // Load mock data
        ObservableList<Business> rawData = DataManager.initializeMockData();

        favoritedBusinesses = FXCollections.observableArrayList(
                business -> new javafx.beans.Observable[]{business.favoriteProperty()});

        for (Business b : rawData) {
            if (userFavorites.contains(b.getName())) {
                b.setFavorite(true);
                favoritedBusinesses.add(b);
            }
            attachFavoriteListener(b);
        }

        allBusinesses = FXCollections.observableArrayList(
                business -> new javafx.beans.Observable[]{business.favoriteProperty()});
        allBusinesses.addAll(rawData);

        // Wire up UI components
        setupCategoryFilter();
        setupFavoritesList();
        setupLiveSearch();
        setupDistanceSlider();
        refreshCardGrid();
        clearDetailPane();
        buildTagCloud();
        setupLeaderboard();

        // Refresh charts/leaderboard when tabs are selected
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                if ("My Impact".equals(newTab.getText())) {
                    refreshCharts();
                } else if ("Leaderboard".equals(newTab.getText())) {
                    refreshLeaderboard();
                }
            }
        });

        refreshRecommendations();
        aiConcierge = new com.fbla.businessboost.util.LocalAIConcierge(allBusinesses);

        // Pre-fill ZIP from onboarding preference
        String preferredZip = SessionManager.getPreferredZip();
        if (!preferredZip.isEmpty()) {
            zipCodeField.setText(preferredZip);
        }
    }

    // ==================== Distance Slider ====================

    /**
     * Wires the distance slider to update its label and mirrors values
     * to a human-readable string.
     */
    private void setupDistanceSlider() {
        distanceSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int idx = Math.min(newVal.intValue(), RADIUS_LABELS.length - 1);
            distanceLabel.setText("Within " + RADIUS_LABELS[idx]);
        });
        // Default: index 1 = 1km (but slider value starts at 1 which maps to index 1)
        distanceLabel.setText("Within " + RADIUS_LABELS[1]);
    }

    /**
     * Returns the currently selected search radius in metres from the distance slider.
     *
     * @return radius in metres
     */
    private int getSelectedRadiusMeters() {
        int idx = (int) Math.round(distanceSlider.getValue());
        idx = Math.max(0, Math.min(idx, RADIUS_VALUES.length - 1));
        return RADIUS_VALUES[idx];
    }

    // ==================== Live Search ====================

    /**
     * Wires the live search text field so every keystroke filters the card grid
     * client-side without firing any API calls.
     */
    private void setupLiveSearch() {
        liveSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterCardGridLive(newVal);
        });
    }

    /**
     * Filters the visible cards by the given query string without touching the
     * {@code allBusinesses} list or making network calls.
     *
     * @param query the text to match against name, category, description, and tags
     */
    private void filterCardGridLive(String query) {
        businessCardGrid.getChildren().clear();
        String selectedCategory = categoryFilter.getValue();
        String q = query == null ? "" : query.trim().toLowerCase();

        List<Business> filtered = allBusinesses.stream()
                .filter(b -> "All".equals(selectedCategory) || b.getCategory().equals(selectedCategory))
                .filter(b -> {
                    if (q.isEmpty()) return true;
                    if (b.getName().toLowerCase().contains(q)) return true;
                    if (b.getCategory().toLowerCase().contains(q)) return true;
                    if (b.getDescription() != null && b.getDescription().toLowerCase().contains(q)) return true;
                    return b.getTags().stream().anyMatch(t -> t.toLowerCase().contains(q));
                })
                .collect(Collectors.toList());

        if (sortByRating) {
            filtered.sort(Comparator.comparingDouble(Business::getRating).reversed());
        }

        if (filtered.isEmpty()) {
            showEmptyState(businessCardGrid, "No results match \"" + query + "\"",
                    "Try removing the filter or searching a different term.");
        } else {
            for (Business b : filtered) {
                businessCardGrid.getChildren().add(createBusinessCard(b));
            }
        }
    }

    // ==================== Skeleton Loading ====================

    /**
     * Replaces the card grid content with 6 shimmer skeleton cards to indicate
     * that a network fetch is in progress.
     */
    private void showSkeletonCards() {
        businessCardGrid.getChildren().clear();
        for (int i = 0; i < 6; i++) {
            businessCardGrid.getChildren().add(buildSkeletonCard());
        }
    }

    /**
     * Builds a single skeleton placeholder card.
     *
     * @return a VBox styled as a skeleton loading card
     */
    private VBox buildSkeletonCard() {
        VBox card = new VBox(0);
        card.getStyleClass().add("skeleton-card");
        card.setPrefWidth(260);
        card.setMaxWidth(260);

        // Skeleton image placeholder
        Region imgPlaceholder = new Region();
        imgPlaceholder.getStyleClass().add("skeleton-image");
        imgPlaceholder.setPrefWidth(260);
        imgPlaceholder.setPrefHeight(160);

        VBox info = new VBox(8);
        info.setPadding(new Insets(12));

        Region line1 = new Region();
        line1.getStyleClass().add("skeleton-line");
        line1.setPrefHeight(14);
        line1.setMaxWidth(200);

        Region line2 = new Region();
        line2.getStyleClass().addAll("skeleton-line", "skeleton-line-short");
        line2.setPrefHeight(12);
        line2.setMaxWidth(140);

        Region line3 = new Region();
        line3.getStyleClass().addAll("skeleton-line", "skeleton-line-short");
        line3.setPrefHeight(11);
        line3.setMaxWidth(160);

        info.getChildren().addAll(line1, line2, line3);
        card.getChildren().addAll(imgPlaceholder, info);
        return card;
    }

    // ==================== Empty State ====================

    /**
     * Shows a friendly empty state illustration inside the given FlowPane.
     *
     * @param pane      the FlowPane to show the empty state in
     * @param title     main empty state message
     * @param subtitle  secondary hint text
     */
    private void showEmptyState(FlowPane pane, String title, String subtitle) {
        pane.getChildren().clear();
        VBox emptyState = new VBox(12);
        emptyState.getStyleClass().add("empty-state-container");
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(60, 40, 60, 40));
        emptyState.setPrefWidth(pane.getWidth() > 100 ? pane.getWidth() : 600);

        Label icon = new Label("🗺️");
        icon.getStyleClass().add("empty-state-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-state-title");
        titleLabel.setWrapText(true);

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("empty-state-subtitle");
        subtitleLabel.setWrapText(true);

        emptyState.getChildren().addAll(icon, titleLabel, subtitleLabel);
        pane.getChildren().add(emptyState);
    }

    // ==================== Card Grid ====================

    /**
     * Rebuilds the entire card grid from the current data + filters + sort.
     * Respects both the category filter and the live search field.
     */
    private void refreshCardGrid() {
        String liveQuery = liveSearchField != null ? liveSearchField.getText() : "";
        if (!liveQuery.isEmpty()) {
            filterCardGridLive(liveQuery);
            return;
        }

        businessCardGrid.getChildren().clear();
        String selectedCategory = categoryFilter.getValue();

        List<Business> filtered = allBusinesses.stream()
                .filter(b -> "All".equals(selectedCategory) || b.getCategory().equals(selectedCategory))
                .collect(Collectors.toList());

        if (sortByRating) {
            filtered.sort(Comparator.comparingDouble(Business::getRating).reversed());
        }

        if (filtered.isEmpty()) {
            showEmptyState(businessCardGrid,
                    "No businesses in \"" + selectedCategory + "\"",
                    "Try selecting \"All\" or searching a different ZIP code.");
        } else {
            for (Business b : filtered) {
                businessCardGrid.getChildren().add(createBusinessCard(b));
            }
        }
    }

    /**
     * Creates a DoorDash-style business card for the card grid.
     *
     * @param business The business to render
     * @return a styled VBox card node
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

        Rectangle clip = new Rectangle(260, 160);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        imageView.setClip(clip);

        String imgUrl = business.getImageUrl();
        if (imgUrl != null && !imgUrl.isEmpty()) {
            try {
                Image image = new Image(imgUrl, 260, 160, false, true, true);
                imageView.setImage(image);
            } catch (Exception e) {
                imageView.getStyleClass().add("card-image-placeholder");
            }
        }

        // Heart button overlay with accessibility text
        Button heartBtn = new Button(business.isFavorite() ? "❤️" : "🤍");
        heartBtn.getStyleClass().add("card-heart-button");
        heartBtn.setAccessibleText(business.isFavorite()
                ? "Remove " + business.getName() + " from favorites"
                : "Add " + business.getName() + " to favorites");
        heartBtn.setOnAction(e -> {
            business.setFavorite(!business.isFavorite());
            heartBtn.setText(business.isFavorite() ? "❤️" : "🤍");
            heartBtn.setAccessibleText(business.isFavorite()
                    ? "Remove " + business.getName() + " from favorites"
                    : "Add " + business.getName() + " to favorites");
        });
        StackPane.setAlignment(heartBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(heartBtn, new Insets(8, 8, 0, 0));
        imageContainer.getChildren().addAll(imageView, heartBtn);

        // ---- Info section ----
        VBox infoBox = new VBox(4);
        infoBox.getStyleClass().add("card-info");
        infoBox.setPadding(new Insets(10, 12, 12, 12));

        Label nameLabel = new Label(business.getName());
        nameLabel.getStyleClass().add("card-title");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(236);

        // Category badge + price row
        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label categoryBadge = new Label(business.getCategory());
        categoryBadge.getStyleClass().add("card-category-badge");
        switch (business.getCategory()) {
            case "Food":     categoryBadge.getStyleClass().add("badge-food");     break;
            case "Retail":   categoryBadge.getStyleClass().add("badge-retail");   break;
            case "Services": categoryBadge.getStyleClass().add("badge-services"); break;
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
        card.setOnMouseClicked(e -> selectBusiness(business));

        // Subtle hover scale animation (Apple-style lift)
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), card);
        scaleUp.setToX(1.02);
        scaleUp.setToY(1.02);
        scaleUp.setInterpolator(Interpolator.EASE_BOTH);
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), card);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);
        scaleDown.setInterpolator(Interpolator.EASE_BOTH);
        card.setOnMouseEntered(e -> scaleUp.playFromStart());
        card.setOnMouseExited(e -> scaleDown.playFromStart());

        return card;
    }

    /** Builds a star string like "★★★★☆" from a 0-5 rating. */
    private String buildStarString(double rating) {
        StringBuilder stars = new StringBuilder();
        int full = (int) Math.floor(rating);
        for (int i = 0; i < full; i++) stars.append("★");
        if (rating - full >= 0.5) stars.append("½");
        int empty = 5 - full - (rating - full >= 0.5 ? 1 : 0);
        for (int i = 0; i < empty; i++) stars.append("☆");
        return stars.toString();
    }

    // ==================== Selection & Detail Pane ====================

    /**
     * Selects a business and fully populates the detail pane, including the
     * static map thumbnail and styled review cards.
     *
     * @param business the business to display
     */
    private void selectBusiness(Business business) {
        this.selectedBusiness = business;
        selectedBusinessLabel.setText(business.getName());

        String desc = business.getDescription();
        businessDescriptionLabel.setText(desc != null && !desc.trim().isEmpty() ? desc : "");

        String phone = business.getPhone();
        businessPhoneLabel.setText(phone != null && !phone.isEmpty() ? "📞 " + phone : "📞 Phone unavailable");

        String website = business.getWebsite();
        businessWebsiteLabel.setText(website != null && !website.isEmpty() ? "🌐 " + website : "🌐 Website unavailable");

        String hours = business.getHours();
        businessHoursLabel.setText(hours != null && !hours.isEmpty() ? "🕒 " + hours : "🕒 Hours unavailable");

        // Load business image
        String imgUrl = business.getImageUrl();
        if (imgUrl != null && !imgUrl.isEmpty()) {
            try {
                Image img = new Image(imgUrl, 290, 160, false, true, true);
                detailImageView.setImage(img);
            } catch (Exception e) {
                detailImageView.setImage(null);
            }
        } else {
            detailImageView.setImage(null);
        }

        // Load static map thumbnail if lat/lon are available
        double lat = business.getLatitude();
        double lon = business.getLongitude();
        if (lat != 0.0 && lon != 0.0) {
            String mapUrl = String.format(
                    "https://staticmap.openstreetmap.de/staticmap.php" +
                    "?center=%.6f,%.6f&zoom=15&size=280x110&markers=%.6f,%.6f,red-pushpin",
                    lat, lon, lat, lon);
            try {
                Image mapImg = new Image(mapUrl, 290, 110, false, true, true);
                mapImageView.setImage(mapImg);
                mapImageView.setVisible(true);
                mapImageView.setManaged(true);
            } catch (Exception e) {
                mapImageView.setVisible(false);
                mapImageView.setManaged(false);
            }
        } else {
            mapImageView.setVisible(false);
            mapImageView.setManaged(false);
        }

        // Show local deals
        // Smart Coupon Engine
        ObservableList<HBox> couponBoxes = FXCollections.observableArrayList();
        List<com.fbla.businessboost.util.SmartCouponEngine.Coupon> coupons = com.fbla.businessboost.util.SmartCouponEngine.getCouponsForBusiness(business);
        for (com.fbla.businessboost.util.SmartCouponEngine.Coupon c : coupons) {
            HBox box = new HBox(10);
            box.setAlignment(Pos.CENTER_LEFT);
            VBox textCol = new VBox(2);
            Label couponDesc = new Label(c.description);
            couponDesc.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            Label codeLabel = new Label(c.code + " (" + c.getFormattedExpiry() + ")");
            codeLabel.setStyle("-fx-font-family: monospace; -fx-text-fill: #10B981; -fx-font-size: 12px;");
            textCol.getChildren().addAll(couponDesc, codeLabel);
            
            Button redeemBtn = new Button("Redeem");
            redeemBtn.getStyleClass().add("action-button");
            redeemBtn.setOnAction(e -> {
                if (com.fbla.businessboost.util.SmartCouponEngine.redeemCoupon(c.code)) {
                    redeemBtn.setText("Redeemed!");
                    redeemBtn.setDisable(true);
                    codeLabel.setText(c.code + " (Redeemed)");
                    codeLabel.setStyle("-fx-font-family: monospace; -fx-text-fill: #94A3B8;");
                } else {
                    redeemBtn.setText("Invalid");
                }
            });
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            box.getChildren().addAll(textCol, spacer, redeemBtn);
            couponBoxes.add(box);
        }
        dealsContainer.getChildren().clear();
        dealsContainer.getChildren().addAll(couponBoxes);

        // Clear review pane
        reviewsContainer.getChildren().clear();
        reviewCountLabel.setText("");

        // Fetch reviews from Supabase in background and render as cards
        javafx.concurrent.Task<List<Review>> fetchReviewsTask = new javafx.concurrent.Task<>() {
            @Override
            protected List<Review> call() throws Exception {
                return SupabaseClient.getReviewsForBusiness(business.getName());
            }
        };

        fetchReviewsTask.setOnSucceeded(e -> {
            List<Review> reviews = fetchReviewsTask.getValue();
            Platform.runLater(() -> {
                if (selectedBusiness == business) {
                    reviewsContainer.getChildren().clear();
                    if (reviews == null || reviews.isEmpty()) {
                        Label noReview = new Label("No reviews yet — be the first!");
                        noReview.getStyleClass().add("detail-caption");
                        reviewsContainer.getChildren().add(noReview);
                        reviewCountLabel.setText("");
                    } else {
                        reviewCountLabel.setText("(" + reviews.size() + ")");
                        for (Review r : reviews) {
                            reviewsContainer.getChildren().add(buildReviewCard(r));
                        }
                        
                        // Aspect Sentiment Parsing
                        com.fbla.businessboost.util.AspectSentimentParser.AspectScores scores = 
                                com.fbla.businessboost.util.AspectSentimentParser.calculateAspectScores(reviews);
                                
                        aspectScoresContainer.getChildren().clear();
                        aspectScoresContainer.getChildren().add(new Label("Community Vibe Matrix:"));
                        
                        aspectScoresContainer.getChildren().add(createAspectBar("Service", scores.service));
                        aspectScoresContainer.getChildren().add(createAspectBar("Price", scores.price));
                        aspectScoresContainer.getChildren().add(createAspectBar("Vibe", scores.vibe));
                        aspectScoresContainer.getChildren().add(createAspectBar("Quality", scores.quality));
                        
                        aspectScoresContainer.setVisible(true);
                        aspectScoresContainer.setManaged(true);
                    }
                }
            });
        });

        fetchReviewsTask.setOnFailed(e -> Platform.runLater(() -> {
            if (selectedBusiness == business) {
                Label errLabel = new Label("Could not load reviews.");
                errLabel.getStyleClass().add("detail-caption");
                reviewsContainer.getChildren().setAll(errLabel);
            }
        }));

        new Thread(fetchReviewsTask).start();
    }

    private HBox createAspectBar(String label, double score) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label);
        l.setPrefWidth(60);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        
        ProgressBar pb = new ProgressBar(score);
        pb.setPrefWidth(120);
        pb.setPrefHeight(10);
        if (score >= 0.7) pb.setStyle("-fx-accent: #10B981;");
        else if (score >= 0.4) pb.setStyle("-fx-accent: #F59E0B;");
        else pb.setStyle("-fx-accent: #EF4444;");
        
        box.getChildren().addAll(l, pb);
        return box;
    }

    /**
     * Builds a styled review card VBox for the detail pane.
     *
     * @param review the review to render
     * @return a styled VBox card
     */
    private VBox buildReviewCard(Review review) {
        VBox card = new VBox(4);
        card.getStyleClass().add("review-card");

        // Star rating row
        int stars = (int) Math.round(review.getRating());
        String starStr = "★".repeat(Math.max(0, stars)) + "☆".repeat(Math.max(0, 5 - stars));
        Label starsLabel = new Label(starStr + "  " + String.format("%.1f", review.getRating()));
        starsLabel.getStyleClass().add("review-stars");

        // Review text
        Label textLabel = new Label(review.getReviewText());
        textLabel.getStyleClass().add("review-text");
        textLabel.setWrapText(true);

        // Timestamp (truncate to date only if long)
        String ts = review.getTimestamp();
        if (ts != null && ts.length() > 10) ts = ts.substring(0, 10);
        Label tsLabel = new Label(ts != null ? ts : "");
        tsLabel.getStyleClass().add("review-timestamp");

        // Text-To-Speech Button
        Button ttsButton = new Button("🔊");
        ttsButton.getStyleClass().add("tts-button");
        ttsButton.setOnAction(e -> {
            new Thread(() -> {
                try {
                    Runtime.getRuntime().exec(new String[]{"say", review.getReviewText()});
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        HBox headerRow = new HBox(10, starsLabel, ttsButton);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(headerRow, textLabel, tsLabel);
        return card;
    }

    /**
     * Clears the detail pane to its placeholder state.
     */
    private void clearDetailPane() {
        selectedBusinessLabel.setText("Select a business");
        businessDescriptionLabel.setText("Click on a business card to view details, deals, and reviews.");
        detailImageView.setImage(null);
        mapImageView.setImage(null);
        mapImageView.setVisible(false);
        mapImageView.setManaged(false);
        dealsContainer.getChildren().clear();
        reviewsContainer.getChildren().clear();
        aspectScoresContainer.getChildren().clear();
        aspectScoresContainer.setVisible(false);
        aspectScoresContainer.setManaged(false);
        reviewCountLabel.setText("");
    }

    // ==================== Filters & Sort ====================

    private void setupCategoryFilter() {
        categoryFilter.setItems(FXCollections.observableArrayList("All", "Food", "Retail", "Services"));
        categoryFilter.setValue("All");
        categoryFilter.setOnAction(event -> {
            hideResultBanner();
            liveSearchField.clear();
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
                    if (newVal != null) selectBusiness(newVal);
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

            refreshRecommendations();

            if (isNowFavorite) {
                SessionManager.addFavorite(business.getName());
            } else {
                SessionManager.removeFavorite(business.getName());
            }

            // Sync to Supabase in background
            String user = SessionManager.getCurrentUser();
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
     * Refreshes the "You Might Also Like…" section.
     * Each recommendation card now shows <em>why</em> it was recommended:
     * "Because you love: #cozy #budget" using the tag overlap explanation.
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

        // Build tag-frequency map from favorites for explainability
        Map<String, Integer> tagFrequency = new HashMap<>();
        for (Business fav : favoritedBusinesses) {
            for (String tag : fav.getTags()) {
                tagFrequency.merge(tag, 1, Integer::sum);
            }
        }

        for (Business rec : recommendations) {
            VBox miniCard = createMiniCard(rec, tagFrequency);
            recommendationCards.getChildren().add(miniCard);
        }
    }

    /**
     * Creates a compact recommendation mini-card with explainability text.
     *
     * @param business     the recommended business
     * @param tagFrequency frequency map of tags from the user's favorites
     * @return a styled mini-card VBox
     */
    private VBox createMiniCard(Business business, Map<String, Integer> tagFrequency) {
        VBox card = new VBox(4);
        card.getStyleClass().add("mini-card");
        card.setPrefWidth(200);
        card.setMaxWidth(200);
        card.setCursor(Cursor.HAND);
        card.setPadding(new Insets(10));

        Label name = new Label(business.getName());
        name.getStyleClass().addAll("card-title", "mini-card-name");
        name.setWrapText(true);

        Label category = new Label(business.getCategory() + " · " + buildStarString(business.getRating()));
        category.getStyleClass().add("card-address");

        // Build explainability: find matching tags sorted by frequency
        List<String> matchingTags = business.getTags().stream()
                .filter(tagFrequency::containsKey)
                .sorted(Comparator.comparingInt(t -> -tagFrequency.getOrDefault(t, 0)))
                .limit(3)
                .collect(Collectors.toList());

        if (!matchingTags.isEmpty()) {
            Label whyLabel = new Label("Because you love: " + String.join(" ", matchingTags));
            whyLabel.getStyleClass().add("rec-why-label");
            whyLabel.setWrapText(true);
            card.getChildren().addAll(name, category, whyLabel);
        } else {
            card.getChildren().addAll(name, category);
        }

        card.setOnMouseClicked(e -> selectBusiness(business));
        return card;
    }

    // ==================== My Impact (Charts) ====================

    /**
     * Refreshes all My Impact stats, charts, and the jobs impact banner.
     */
    private void refreshCharts() {
        int totalFavs = favoritedBusinesses.size();
        statTotalFavorites.setText(String.valueOf(totalFavs));
        statReviewsDone.setText(String.valueOf(SessionManager.getReviewCount()));

        // Update jobs impact banner
        if (totalFavs > 0) {
            int estimatedJobs = (int) Math.round(totalFavs * 4.7);
            jobsCountLabel.setText(String.valueOf(estimatedJobs));
            jobsImpactBanner.setVisible(true);
            jobsImpactBanner.setManaged(true);
        } else {
            jobsImpactBanner.setVisible(false);
            jobsImpactBanner.setManaged(false);
        }

        if (totalFavs > 0) {
            double avgRating = favoritedBusinesses.stream()
                    .mapToDouble(Business::getRating)
                    .average()
                    .orElse(0.0);
            statAvgRating.setText(String.format("%.1f", avgRating));

            Map<String, Long> catCount = favoritedBusinesses.stream()
                    .collect(Collectors.groupingBy(Business::getCategory, Collectors.counting()));
            String topCat = catCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("-");
            statTopCategory.setText(topCat);

            // Bar Chart
            categoryBarChart.getData().clear();
            catCount.forEach((cat, count) -> {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName(cat);
                XYChart.Data<String, Number> dataNode = new XYChart.Data<>(cat, count);
                dataNode.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        switch (cat) {
                            case "Food":     newNode.getStyleClass().add("chart-bar-food"); break;
                            case "Retail":   newNode.getStyleClass().add("chart-bar-retail"); break;
                            case "Services": newNode.getStyleClass().add("chart-bar-services"); break;
                            default:         newNode.getStyleClass().add("chart-bar-default"); break;
                        }
                    }
                });
                series.getData().add(dataNode);
                categoryBarChart.getData().add(series);
            });

            // Pie Chart
            ratingPieChart.getData().clear();
            Map<String, Long> ratingDist = favoritedBusinesses.stream()
                    .collect(Collectors.groupingBy(b -> {
                        double r = b.getRating();
                        if (r >= 4.5) return "★★★★★ (4.5+)";
                        if (r >= 4.0) return "★★★★ (4.0-4.4)";
                        if (r >= 3.5) return "★★★½ (3.5-3.9)";
                        return "★★★ (< 3.5)";
                    }, Collectors.counting()));
            ratingDist.forEach((label, count) -> {
                PieChart.Data slice = new PieChart.Data(label, count);
                ratingPieChart.getData().add(slice);
                slice.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        if (label.contains("4.5+"))     newNode.getStyleClass().add("pie-excellent");
                        else if (label.contains("4.0")) newNode.getStyleClass().add("pie-good");
                        else if (label.contains("3.5")) newNode.getStyleClass().add("pie-fair");
                        else                             newNode.getStyleClass().add("pie-poor");
                    }
                });
            });

            // Async Supabase stat snapshot
            String user = SessionManager.getCurrentUser();
            final double finalAvgRating = avgRating;
            final String finalTopCat = topCat;
            if (user != null) {
                int totalReviews = SessionManager.getReviewCount();
                int score = (totalFavs * 10) + (totalReviews * 25) + (3 * 50);
                new Thread(() -> SupabaseClient.saveUserStats(user, totalFavs, finalAvgRating, finalTopCat, totalReviews, score)).start();
            }
        } else {
            statAvgRating.setText("0.0");
            statTopCategory.setText("-");
            categoryBarChart.getData().clear();
            ratingPieChart.getData().clear();
        }
    }

    // ==================== Smart Search ====================

    @FXML
    private void handleSmartSearch() {
        String query = smartSearchField.getText().trim();
        smartSearchResults.getChildren().clear();

        if (query.isEmpty() || aiConcierge == null) {
            return;
        }

        List<com.fbla.businessboost.util.LocalAIConcierge.SearchResult> aiResults = aiConcierge.search(query);

        if (aiResults.isEmpty()) {
            Label noMatch = new Label("No matching businesses found.");
            noMatch.getStyleClass().add("card-subtitle");
            smartSearchResults.getChildren().add(noMatch);
            return;
        }

        // Display up to 5 best matches
        for (int i = 0; i < Math.min(5, aiResults.size()); i++) {
            com.fbla.businessboost.util.LocalAIConcierge.SearchResult res = aiResults.get(i);
            Business b = res.business;
            
            VBox card = new VBox(5);
            card.getStyleClass().add("business-card");
            card.setPadding(new Insets(10));
            card.setCursor(Cursor.HAND);

            Label name = new Label(b.getName());
            name.getStyleClass().add("card-title");

            Label cat = new Label(b.getCategory() + " • " + b.getRating() + " ⭐");
            cat.getStyleClass().add("card-subtitle");

            Label reason = new Label("🤖 " + res.explanation);
            reason.getStyleClass().add("help-text");
            reason.setStyle("-fx-text-fill: #10B981; -fx-font-style: italic;");

            card.getChildren().addAll(name, cat, reason);

            card.setOnMouseClicked(e -> {
                mainTabPane.getSelectionModel().select(0);
                selectBusiness(b);
            });

            smartSearchResults.getChildren().add(card);
        }
    }

    private void buildTagCloud() {
        Set<String> allTags = new TreeSet<>();
        for (Business b : allBusinesses) {
            allTags.addAll(b.getTags());
        }
        for (String tag : allTags) {
            Button tagBtn = new Button(tag);
            tagBtn.getStyleClass().add("tag-chip");
            tagBtn.setAccessibleText("Filter by tag " + tag);
            tagBtn.setOnAction(e -> {
                smartSearchField.setText(tag);
                handleSmartSearch();
            });
            tagCloudPane.getChildren().add(tagBtn);
        }
    }

    // ==================== Theme Toggle ====================

    @FXML
    private void handleThemeToggle() {
        ObservableList<String> stylesheets = themeToggleButton.getScene().getRoot().getStylesheets();
        String lightCss = getClass().getResource("/styles.css").toExternalForm();
        String darkCss  = getClass().getResource("/styles-dark.css").toExternalForm();

        stylesheets.clear();
        if (darkMode) {
            stylesheets.add(lightCss);
            themeToggleButton.setText("Theme: Dark");
            darkMode = false;
        } else {
            stylesheets.add(darkCss);
            themeToggleButton.setText("Theme: Light");
            darkMode = true;
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/LoginView.fxml"));
            Stage stage = (Stage) themeToggleButton.getScene().getWindow();
            stage.setTitle("Trove — Sign In");
            stage.setScene(new Scene(loader.load(), 850, 550));
            stage.setMinWidth(850);
            stage.setMinHeight(550);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== ZIP Search ====================

    @FXML
    private void handleZipSearch() {
        String zipCode = zipCodeField.getText();
        if (zipCode == null || zipCode.trim().isEmpty()) {
            showResultBanner("⚠ Please enter a zip code.", "result-banner-warning");
            return;
        }

        hideResultBanner();
        showSkeletonCards();

        String trimmedZip = zipCode.trim();
        int radiusMeters = getSelectedRadiusMeters();

        javafx.concurrent.Task<ObservableList<Business>> fetchTask = new javafx.concurrent.Task<>() {
            @Override
            protected ObservableList<Business> call() throws Exception {
                return BusinessFetcher.fetchBusinessesByZipCode(trimmedZip, radiusMeters);
            }
        };

        fetchTask.setOnSucceeded(e -> {
            ObservableList<Business> results = fetchTask.getValue();
            if (results.isEmpty()) {
                businessCardGrid.getChildren().clear();
                showResultBanner("No businesses found near " + trimmedZip + " — try a different zip code.",
                        "result-banner-warning");
            } else {
                // Save to offline cache in background
                new Thread(() -> OfflineCache.save(trimmedZip, results)).start();

                List<String> userFavoritesList = SessionManager.getFavorites();
                Platform.runLater(() -> {
                    processZipResults(results, userFavoritesList);
                    showResultBanner("✓ Found " + results.size() + " local businesses near " + trimmedZip,
                            "result-banner-success");
                });
            }
        });

        fetchTask.setOnFailed(e -> {
            Throwable ex = fetchTask.getException();

            // Try offline cache fallback
            ObservableList<Business> cached = OfflineCache.load(trimmedZip);
            if (!cached.isEmpty()) {
                List<String> userFavoritesList = SessionManager.getFavorites();
                Platform.runLater(() -> {
                    processZipResults(cached, userFavoritesList);
                    showResultBanner("📦 Network unavailable — showing cached results for " + trimmedZip,
                            "result-banner-warning");
                });
            } else {
                Platform.runLater(() -> {
                    businessCardGrid.getChildren().clear();
                    String msg = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "";
                    if (msg.toLowerCase().contains("invalid zip")) {
                        showResultBanner("❌ Invalid zip code — please try again.", "result-banner-error");
                    } else {
                        showResultBanner("❌ Search failed — check your connection and try again.", "result-banner-error");
                    }
                    showEmptyState(businessCardGrid, "No results found",
                            "Check your connection or try a different ZIP code.");
                });
            }

            if (ex != null) ex.printStackTrace();
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
        liveSearchField.clear();
        // Refresh recommendations whenever a new favorite is added
        refreshRecommendations();
        
        // Initialize Local AI Concierge model
        aiConcierge = new com.fbla.businessboost.util.LocalAIConcierge(allBusinesses);
        tagCloudPane.getChildren().clear();
        buildTagCloud();
    }

    // ==================== Reviews ====================

    @FXML
    private void handleAddReview() {
        if (selectedBusiness == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a business to review.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ReviewDialog.fxml"));
            Scene scene = new Scene(loader.load());
            if (darkMode) {
                scene.getStylesheets().add(getClass().getResource("/styles-dark.css").toExternalForm());
            }
            ReviewDialogController controller = loader.getController();
            controller.setBusiness(selectedBusiness);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Submit Review — " + selectedBusiness.getName());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();

            selectBusiness(selectedBusiness);
            refreshLeaderboard();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load review dialog: " + e.getMessage());
        }
    }

    // ==================== Reports ====================

    @FXML
    private void handleGenerateReport() {
        String username = SessionManager.getCurrentUser();
        ReportGenerator.generateReport(username, FXCollections.observableArrayList(favoritedBusinesses));
    }

    // ==================== Result Banner ====================

    private void showResultBanner(String message, String styleClass) {
        resultBannerLabel.setText(message);
        resultBanner.getStyleClass().removeAll("result-banner-success", "result-banner-warning", "result-banner-error");
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

    // ==================== Leaderboard ====================

    private void setupLeaderboard() {
        colRank.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getRank()));
        colUser.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEmail()));
        colScore.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getScore()));
        colBadge.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCategoryBadge()));
        colStreak.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStreak()));
        colTitle.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getRankTitle()));

        colRank.setCellFactory(column -> new TableCell<LeaderboardUser, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(item));
                    if (item == 1) {
                        setStyle("-fx-text-fill: #EAB308; -fx-font-weight: bold;");
                    } else if (item == 2) {
                        setStyle("-fx-text-fill: #94A3B8; -fx-font-weight: bold;");
                    } else if (item == 3) {
                        setStyle("-fx-text-fill: #B45309; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        ToggleGroup group = new ToggleGroup();
        btnGlobalRank.setToggleGroup(group);
        btnMonthlyRank.setToggleGroup(group);
        btnCategoryRank.setToggleGroup(group);
        btnStreakRank.setToggleGroup(group);

        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true);
            } else {
                refreshLeaderboard();
            }
        });
    }

    private void refreshLeaderboard() {
        String mode = "Global";
        if (btnMonthlyRank.isSelected()) {
            mode = "Monthly";
        } else if (btnCategoryRank.isSelected()) {
            mode = "Category";
        } else if (btnStreakRank.isSelected()) {
            mode = "Streak";
        }
        refreshLeaderboard(mode);
    }

    private void refreshLeaderboard(String mode) {
        String currentUser = SessionManager.getCurrentUser();
        if (currentUser == null || currentUser.isEmpty()) {
            currentUser = "you@example.com";
        }

        int totalFavs = favoritedBusinesses.size();
        int reviews = SessionManager.getReviewCount();
        int streakWeeks = 3;
        int userScore = (totalFavs * 10) + (reviews * 25) + (streakWeeks * 50);

        String specialty = "Local Explorer";
        if (!favoritedBusinesses.isEmpty()) {
            Map<String, Long> categoryCounts = favoritedBusinesses.stream()
                    .collect(Collectors.groupingBy(Business::getCategory, Collectors.counting()));
            String topCat = categoryCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("");
            if (!topCat.isEmpty()) {
                specialty = topCat + " Supporter";
            }
        }

        String userTitle;
        if (userScore >= 300) {
            userTitle = "Trove Legend";
        } else if (userScore >= 150) {
            userTitle = "Community Hero";
        } else if (userScore >= 50) {
            userTitle = "Local Advocate";
        } else {
            userTitle = "Rising Star";
        }

        final String finalCurrentUser = currentUser;
        final int finalUserScore = userScore;
        final String finalSpecialty = specialty;
        final String finalUserTitle = userTitle;
        final int finalReviews = reviews;
        
        // Ensure user stats are saved to Supabase before fetching leaderboard
        new Thread(() -> {
            SupabaseClient.saveUserStats(finalCurrentUser, totalFavs, 5.0, finalSpecialty, finalReviews, finalUserScore);
            org.json.JSONArray statsArray = SupabaseClient.getAllUserStats();
            List<LeaderboardRaw> rawData = new ArrayList<>();
            boolean currentUserFound = false;
            
            Set<String> seenEmails = new HashSet<>();
            for (int i = 0; i < statsArray.length(); i++) {
                org.json.JSONObject obj = statsArray.getJSONObject(i);
                String email = obj.optString("username", "unknown");
                if (seenEmails.contains(email)) continue;
                seenEmails.add(email);
                
                int f = obj.optInt("total_favorites", 0);
                double avg = obj.optDouble("avg_rating", 0.0);
                String cat = obj.optString("top_category", "Local");
                
                int r = obj.optInt("total_reviews", f / 2);
                int fakeStreak = (f % 5) + 1;
                int score = obj.optInt("user_score", (f * 10) + (r * 25) + (fakeStreak * 50));
                int monthlyScore = score / 2 + 10;
                
                String s = cat == null || cat.isEmpty() ? "Local Explorer" : cat + " Supporter";
                String title;
                if (score >= 300) title = "Trove Legend";
                else if (score >= 150) title = "Community Hero";
                else if (score >= 50) title = "Local Advocate";
                else title = "Rising Star";
                
                if (email.equals(finalCurrentUser)) {
                    // Use local computed stats for current user
                    rawData.add(new LeaderboardRaw(finalCurrentUser, finalUserScore, finalUserScore / 2 + 10, finalSpecialty, 3, finalUserTitle));
                    currentUserFound = true;
                } else {
                    rawData.add(new LeaderboardRaw(email, score, monthlyScore, s, fakeStreak, title));
                }
            }
            
            if (!currentUserFound) {
                rawData.add(new LeaderboardRaw(finalCurrentUser, finalUserScore, finalUserScore / 2 + 10, finalSpecialty, 3, finalUserTitle));
            }
            
            // If offline or no data returned, populate with mock community members
            if (statsArray.length() == 0) {
                rawData.add(new LeaderboardRaw("alex.m@example.com", 320, 150, "Retail", 5, "Trove Legend"));
                rawData.add(new LeaderboardRaw("jordan.k@example.com", 280, 130, "Food", 4, "Community Hero"));
                rawData.add(new LeaderboardRaw("sam.taylor@example.com", 190, 80, "Services", 2, "Community Hero"));
                rawData.add(new LeaderboardRaw("casey.r@example.com", 110, 45, "Food", 1, "Local Advocate"));
                rawData.add(new LeaderboardRaw("morgan.b@example.com", 60, 20, "Retail", 0, "Rising Star"));
            }
            
            if ("Monthly".equals(mode)) {
                rawData.sort((a, b) -> Integer.compare(b.monthlyScore, a.monthlyScore));
            } else if ("Streak".equals(mode)) {
                rawData.sort((a, b) -> Integer.compare(b.streakWeeks, a.streakWeeks));
            } else if ("Category".equals(mode)) {
                rawData.sort((a, b) -> {
                    int cmp = a.specialty.compareTo(b.specialty);
                    if (cmp == 0) return Integer.compare(b.globalScore, a.globalScore);
                    return cmp;
                });
            } else {
                rawData.sort((a, b) -> Integer.compare(b.globalScore, a.globalScore));
            }

            Platform.runLater(() -> {
                ObservableList<LeaderboardUser> usersList = FXCollections.observableArrayList();
                int userRankPosition = rawData.size() + 1;
                int displayScore = 0;

                for (int i = 0; i < rawData.size(); i++) {
                    LeaderboardRaw raw = rawData.get(i);
                    int rank = i + 1;
                    int score = "Monthly".equals(mode) ? raw.monthlyScore : raw.globalScore;
                    String streakDisplay = raw.streakWeeks + (raw.streakWeeks == 1 ? " week" : " weeks");

                    LeaderboardUser lUser = new LeaderboardUser(rank, raw.email, score, raw.specialty, streakDisplay, raw.title);
                    usersList.add(lUser);

                    if (raw.email.equals(finalCurrentUser)) {
                        userRankPosition = rank;
                        displayScore = score;
                    }
                }

                leaderboardTable.setItems(usersList);

                lblUserRank.setText("#" + userRankPosition);
                lblUserScore.setText(displayScore + " pts");
                double percentile = rawData.size() == 0 ? 100.0 : ((double)(rawData.size() - userRankPosition + 1) / rawData.size()) * 100.0;
                if (percentile > 100) percentile = 100;
                lblUserPercentile.setText(String.format("Top %.0f%% of community supporters", percentile));
            });
        }).start();
    }

    private static class LeaderboardRaw {
        String email;
        int globalScore;
        int monthlyScore;
        String specialty;
        int streakWeeks;
        String title;

        LeaderboardRaw(String email, int globalScore, int monthlyScore, String specialty, int streakWeeks, String title) {
            this.email = email;
            this.globalScore = globalScore;
            this.monthlyScore = monthlyScore;
            this.specialty = specialty;
            this.streakWeeks = streakWeeks;
            this.title = title;
        }
    }

    public static class LeaderboardUser {
        private final int rank;
        private final String email;
        private final int score;
        private final String categoryBadge;
        private final String streak;
        private final String rankTitle;

        public LeaderboardUser(int rank, String email, int score, String categoryBadge, String streak, String rankTitle) {
            this.rank = rank;
            this.email = email;
            this.score = score;
            this.categoryBadge = categoryBadge;
            this.streak = streak;
            this.rankTitle = rankTitle;
        }

        public int getRank() { return rank; }
        public String getEmail() { return email; }
        public int getScore() { return score; }
        public String getCategoryBadge() { return categoryBadge; }
        public String getStreak() { return streak; }
        public String getRankTitle() { return rankTitle; }
    }

    // ==================== Accessibility ====================

    @FXML
    private void handleHighContrastToggle() {
        boolean isHighContrast = themeToggleButton.getScene().getRoot().getStyleClass().contains("high-contrast");
        if (isHighContrast) {
            themeToggleButton.getScene().getRoot().getStyleClass().remove("high-contrast");
        } else {
            themeToggleButton.getScene().getRoot().getStyleClass().add("high-contrast");
        }
    }

    @FXML
    private void handleDyslexiaFontToggle() {
        boolean isDyslexic = themeToggleButton.getScene().getRoot().getStyleClass().contains("dyslexia-font");
        if (isDyslexic) {
            themeToggleButton.getScene().getRoot().getStyleClass().remove("dyslexia-font");
        } else {
            themeToggleButton.getScene().getRoot().getStyleClass().add("dyslexia-font");
        }
    }

    private double currentTextSize = 14.0;

    @FXML
    private void handleIncreaseTextSize() {
        currentTextSize += 2.0;
        themeToggleButton.getScene().getRoot().setStyle("-fx-font-size: " + currentTextSize + "px;");
    }

    @FXML
    private void handleDecreaseTextSize() {
        if (currentTextSize > 10.0) {
            currentTextSize -= 2.0;
            themeToggleButton.getScene().getRoot().setStyle("-fx-font-size: " + currentTextSize + "px;");
        }
    }

    // ==================== Local Crawl ====================

    @FXML
    private void handlePlanLocalCrawl() {
        if (favoritedBusinesses.size() < 2) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Not Enough Favorites");
            alert.setHeaderText("Need more stops for a crawl!");
            alert.setContentText("Please favorite at least 2 businesses to generate a local crawl route.");
            alert.showAndWait();
            return;
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/RoutePlannerDialog.fxml"));
            javafx.scene.Parent root = loader.load();

            RoutePlannerController controller = loader.getController();
            controller.setFavorites(favoritedBusinesses);

            Stage stage = new Stage();
            stage.setTitle("Local Crawl Planner");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not load route planner dialog.");
        }
    }

    // ==================== Demo Tools ====================

    @FXML
    private void handleResetDemo() {
        com.fbla.businessboost.util.SessionManager.clearSession();
        // Just show an alert that it's reset
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Demo Reset");
        alert.setHeaderText("Demo State Purged");
        alert.setContentText("All local preferences and caches have been cleared. Restart the app for a fresh demo state.");
        alert.showAndWait();
    }

    @FXML
    private void handleToggleStatsOverlay() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Live Stats Overlay");
        alert.setHeaderText("Behind the scenes engine firing...");
        alert.setContentText("Trust Score Engine: ONLINE\nTF-IDF Local Concierge: ONLINE\nActive Honeypots: 1\nAspect Parsers: 4\nCoupons Redeemed: " + com.fbla.businessboost.util.SmartCouponEngine.getRedeemedCount());
        alert.showAndWait();
    }

    @FXML
    private void handleGuidedTour() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Guided Tour");
        alert.setHeaderText("Starting Guided Tour...");
        alert.setContentText("Please wait. The app will now automatically present the core features.");
        alert.showAndWait();
        
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                javafx.application.Platform.runLater(() -> mainTabPane.getSelectionModel().select(1)); // Impact
                Thread.sleep(2000);
                javafx.application.Platform.runLater(() -> mainTabPane.getSelectionModel().select(2)); // Smart Search
                Thread.sleep(2000);
                javafx.application.Platform.runLater(() -> mainTabPane.getSelectionModel().select(3)); // Leaderboard
                Thread.sleep(2000);
                javafx.application.Platform.runLater(() -> mainTabPane.getSelectionModel().select(0)); // Back to Discover
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
