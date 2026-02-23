package com.fbla.businessboost.controller;

import com.fbla.businessboost.model.Business;
import com.fbla.businessboost.model.Review;
import com.fbla.businessboost.util.DataManager;
import com.fbla.businessboost.util.ReportGenerator;
import com.fbla.businessboost.util.SupabaseClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main controller for the FBLA Business Boost dashboard.
 * 
 * <p>
 * This controller manages all user interactions with the main application
 * window,
 * including business listing, filtering, sorting, favorites management, and
 * report generation.
 * It follows the MVC pattern by separating UI logic from business logic and
 * data models.
 * </p>
 * 
 * <p>
 * <strong>Architecture Decisions:</strong>
 * </p>
 * <ul>
 * <li><strong>FilteredList:</strong> Wraps the main ObservableList to provide
 * efficient
 * category filtering without modifying the original data. Changes to the filter
 * predicate
 * automatically update the TableView.</li>
 * <li><strong>SortedList:</strong> Wraps the FilteredList to enable sorting
 * while preserving
 * filter state. This layered approach allows independent filter and sort
 * operations.</li>
 * <li><strong>Bidirectional Binding:</strong> The favorites ListView is bound
 * to a filtered
 * view of businesses where favorite=true, ensuring automatic updates when users
 * toggle
 * favorite checkboxes.</li>
 * </ul>
 * 
 * <p>
 * <strong>Variable Scope Rationale:</strong>
 * </p>
 * <ul>
 * <li><strong>Instance variables (allBusinesses, filteredList, etc.):</strong>
 * Must persist
 * across multiple method calls and UI events, so declared at class level.</li>
 * <li><strong>FXML-injected components:</strong> Declared as instance variables
 * with @FXML
 * annotation to enable JavaFX runtime injection during FXML loading.</li>
 * <li><strong>Local variables:</strong> Used within methods for temporary
 * calculations or
 * single-use objects to minimize memory footprint.</li>
 * </ul>
 * 
 * @author FBLA Development Team
 * @version 1.0
 */
public class MainController {

    // ==================== FXML Injected Components ====================

    @FXML
    private TableView<Business> businessTable;
    @FXML
    private TableColumn<Business, String> nameColumn;
    @FXML
    private TableColumn<Business, String> categoryColumn;
    @FXML
    private TableColumn<Business, Double> ratingColumn;
    @FXML
    private TableColumn<Business, Boolean> favoriteColumn;

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
    private javafx.scene.layout.HBox resultBanner;
    @FXML
    private Label resultBannerLabel;

    // ==================== Data Collections ====================

    /** Master list of all businesses shown in the table */
    private ObservableList<Business> allBusinesses;

    /**
     * Separate list of ALL favorited businesses (drives the sidebar, independent of
     * table)
     */
    private ObservableList<Business> favoritedBusinesses;

    /** Filtered view based on category selection */
    private FilteredList<Business> filteredList;

    /** Sorted view of filtered businesses */
    private SortedList<Business> sortedList;

    /**
     * Initializes the controller after FXML loading.
     * Called automatically by JavaFX framework.
     * 
     * <p>
     * Initialization sequence:
     * </p>
     * <ol>
     * <li>Load mock data from DataManager</li>
     * <li>Configure TableView columns with property bindings</li>
     * <li>Set up filtering and sorting wrappers</li>
     * <li>Initialize category filter ComboBox</li>
     * <li>Configure favorites sidebar with automatic updates</li>
     * <li>Set up event listeners for business selection</li>
     * </ol>
     */
    @FXML
    public void initialize() {
        // Fetch user favorites
        String currentUser = com.fbla.businessboost.util.SessionManager.getCurrentUser();
        java.util.List<String> userFavorites = currentUser != null ? SupabaseClient.getUserFavorites(currentUser)
                : new java.util.ArrayList<>();

        // Load mock data into an extractor-backed list so FilteredList
        // re-evaluates when favoriteProperty() changes on any item.
        ObservableList<Business> rawData = DataManager.initializeMockData();

        // Separate list for favorites sidebar (independent of table data)
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

        // Configure table columns
        setupTableColumns();

        // Set up filtering and sorting
        setupFilteringAndSorting();

        // Initialize category filter dropdown
        setupCategoryFilter();

        // Configure favorites sidebar
        setupFavoritesList();

        // Set up business selection listener
        setupSelectionListener();
    }

    /**
     * Configures TableView columns with property bindings and custom cell
     * factories.
     * 
     * <p>
     * <strong>PropertyValueFactory:</strong> Automatically binds table columns to
     * Business
     * class properties using JavaFX reflection. For example, "name" maps to
     * nameProperty().
     * </p>
     * 
     * <p>
     * <strong>CheckBoxTableCell:</strong> Provides editable checkbox cells for the
     * favorite
     * column, enabling users to toggle favorite status directly in the table.
     * </p>
     */
    private void setupTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        ratingColumn.setCellValueFactory(new PropertyValueFactory<>("rating"));

        // Custom cell factory for rating to show one decimal place
        ratingColumn.setCellFactory(column -> new TableCell<Business, Double>() {
            @Override
            protected void updateItem(Double rating, boolean empty) {
                super.updateItem(rating, empty);
                if (empty || rating == null) {
                    setText(null);
                } else {
                    setText(String.format("%.1f ⭐", rating));
                }
            }
        });

        // Checkbox column for favorites
        favoriteColumn.setCellValueFactory(new PropertyValueFactory<>("favorite"));
        favoriteColumn.setCellFactory(CheckBoxTableCell.forTableColumn(favoriteColumn));
        favoriteColumn.setEditable(true);
        businessTable.setEditable(true);
    }

    /**
     * Initializes FilteredList and SortedList wrappers for dynamic filtering and
     * sorting.
     * 
     * <p>
     * <strong>Design Pattern:</strong> Uses the Decorator pattern - FilteredList
     * wraps
     * ObservableList, then SortedList wraps FilteredList. Each layer adds
     * functionality
     * without modifying the underlying data structure.
     * </p>
     */
    private void setupFilteringAndSorting() {
        // Create filtered list (initially shows all items)
        filteredList = new FilteredList<>(allBusinesses, business -> true);

        // Wrap filtered list in sorted list
        sortedList = new SortedList<>(filteredList);

        // Bind sorted list to table
        sortedList.comparatorProperty().bind(businessTable.comparatorProperty());
        businessTable.setItems(sortedList);
    }

    /**
     * Initializes the category filter ComboBox with all available categories.
     */
    private void setupCategoryFilter() {
        categoryFilter.setItems(FXCollections.observableArrayList(
                "All", "Food", "Retail", "Services"));
        categoryFilter.setValue("All");

        // Add listener to update filter when selection changes
        categoryFilter.setOnAction(event -> handleCategoryFilter());
    }

    /**
     * Configures the favorites ListView to automatically display favorited
     * businesses.
     * 
     * <p>
     * <strong>Automatic Updates:</strong> Uses a FilteredList that listens to
     * changes
     * in the favorite property. When a user checks/unchecks a favorite checkbox,
     * the
     * ListView automatically adds/removes the business without manual refresh.
     * </p>
     */
    private void setupFavoritesList() {
        // Sidebar is driven by the separate favoritedBusinesses list, NOT the table
        // data
        favoritesList.setItems(favoritedBusinesses);

        // Custom cell factory to display business name and rating
        favoritesList.setCellFactory(listView -> new ListCell<Business>() {
            @Override
            protected void updateItem(Business business, boolean empty) {
                super.updateItem(business, empty);
                if (empty || business == null) {
                    setText(null);
                } else {
                    setText(String.format("%s (%.1f⭐)", business.getName(), business.getRating()));
                }
            }
        });

        // Clicking a favorite in the sidebar selects and scrolls to it in the main
        // table
        favoritesList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        businessTable.getSelectionModel().select(newVal);
                        businessTable.scrollTo(newVal);
                    }
                });
    }

    /**
     * Attaches a listener to a Business's favorite property to sync with Supabase.
     */
    private void attachFavoriteListener(Business business) {
        business.favoriteProperty().addListener((obs, wasFavorite, isNowFavorite) -> {
            // Sync with sidebar list
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

    /**
     * Sets up listener for business selection in the TableView.
     * When a business is selected, displays its deals in the right pane.
     */
    private void setupSelectionListener() {
        businessTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        displayBusinessDeals(newValue);
                    } else {
                        clearDealsPane();
                    }
                });
    }

    /**
     * Handles category filter changes by updating the FilteredList predicate.
     * 
     * <p>
     * <strong>Predicate Logic:</strong> "All" shows all businesses, otherwise
     * filters
     * to match the selected category. The predicate is evaluated for each business
     * in
     * the list, and only matching items appear in the TableView.
     * </p>
     */
    @FXML
    private void handleCategoryFilter() {
        String selectedCategory = categoryFilter.getValue();

        if ("All".equals(selectedCategory)) {
            filteredList.setPredicate(business -> true);
        } else {
            filteredList.setPredicate(business -> business.getCategory().equals(selectedCategory));
        }

        // Show inline message when filter yields no results
        if (filteredList.isEmpty() && !"All".equals(selectedCategory)) {
            showResultBanner("No businesses match \"" + selectedCategory + "\" — try selecting \"All\"",
                    "result-banner-warning");
        } else {
            hideResultBanner();
        }
    }

    /**
     * Sorts businesses by rating in descending order (highest first).
     * 
     * <p>
     * <strong>Implementation Note:</strong> Sets the rating column as the sort
     * column
     * with descending order. The TableView's sort order is updated, which
     * automatically
     * updates the SortedList due to the binding established in
     * setupFilteringAndSorting().
     * </p>
     */
    @FXML
    private void handleSortByRating() {
        ratingColumn.setSortType(TableColumn.SortType.DESCENDING);
        businessTable.getSortOrder().clear();
        businessTable.getSortOrder().add(ratingColumn);
    }

    /**
     * Displays the deals for the selected business in the deals pane.
     * Also fetches persistent reviews from Supabase in the background.
     * 
     * @param business The selected business
     */
    private void displayBusinessDeals(Business business) {
        selectedBusinessLabel.setText(business.getName() + " - Details");
        String desc = business.getDescription();
        if (desc == null || desc.trim().isEmpty()) {
            businessDescriptionLabel.setText("");
        } else {
            businessDescriptionLabel.setText(desc);
        }

        // Show local deals first
        dealsPane.setItems(business.getDeals());

        // Fetch reviews from Supabase in background so UI doesn't freeze
        javafx.concurrent.Task<java.util.List<Review>> fetchReviewsTask = new javafx.concurrent.Task<>() {
            @Override
            protected java.util.List<Review> call() throws Exception {
                return SupabaseClient.getReviewsForBusiness(business.getName());
            }
        };

        fetchReviewsTask.setOnSucceeded(e -> {
            java.util.List<Review> reviews = fetchReviewsTask.getValue();
            if (reviews != null && !reviews.isEmpty()) {
                // If it's still the selected business, append reviews to the deals pane
                // visually
                Platform.runLater(() -> {
                    if (businessTable.getSelectionModel().getSelectedItem() == business) {
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
     * Clears the deals pane when no business is selected.
     */
    private void clearDealsPane() {
        selectedBusinessLabel.setText("Select a business to view details");
        businessDescriptionLabel.setText("");
        dealsPane.setItems(FXCollections.observableArrayList());
    }

    /**
     * Handles fetching businesses by Zip Code from an external API using a
     * background Task.
     */
    @FXML
    private void handleZipSearch() {
        String zipCode = zipCodeField.getText();
        if (zipCode == null || zipCode.trim().isEmpty()) {
            showResultBanner("⚠ Please enter a zip code.", "result-banner-warning");
            return;
        }

        // Disable table during search
        businessTable.setDisable(true);
        hideResultBanner();

        String currentUser = com.fbla.businessboost.util.SessionManager.getCurrentUser();
        String trimmedZip = zipCode.trim();

        javafx.concurrent.Task<ObservableList<Business>> fetchTask = new javafx.concurrent.Task<>() {
            @Override
            protected ObservableList<Business> call() throws Exception {
                return com.fbla.businessboost.util.BusinessFetcher.fetchBusinessesByZipCode(trimmedZip);
            }
        };

        fetchTask.setOnSucceeded(e -> {
            businessTable.setDisable(false);
            ObservableList<Business> results = fetchTask.getValue();
            if (results.isEmpty()) {
                showResultBanner("No businesses found near " + trimmedZip + " — try a different zip code.",
                        "result-banner-warning");
            } else {
                new Thread(() -> {
                    java.util.List<String> userFavoritesList = currentUser != null
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
            businessTable.setDisable(false);
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

    private void processZipResults(ObservableList<Business> results, java.util.List<String> userFavoritesList) {
        allBusinesses.clear();

        // Only add the new search results to the table — favorites stay in the sidebar
        // independently
        for (Business newBiz : results) {
            if (userFavoritesList.contains(newBiz.getName())) {
                newBiz.setFavorite(true);
            }
            attachFavoriteListener(newBiz);
            allBusinesses.add(newBiz);
        }

        categoryFilter.setValue("All"); // Reset filter
    }

    /**
     * Opens the review submission dialog for the selected business.
     * 
     * <p>
     * <strong>Modal Dialog:</strong> Uses Modality.APPLICATION_MODAL to block
     * interaction
     * with the main window until the review dialog is closed, ensuring users
     * complete or
     * cancel the review before continuing.
     * </p>
     */
    @FXML
    private void handleAddReview() {
        Business selectedBusiness = businessTable.getSelectionModel().getSelectedItem();

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

            // Refresh table to show updated rating
            businessTable.refresh();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to load review dialog: " + e.getMessage());
        }
    }

    /**
     * Generates a support report for all favorited businesses.
     * Delegates to ReportGenerator utility class.
     */
    @FXML
    private void handleGenerateReport() {
        // Use the separate favorites list (contains all favorites across searches)
        ReportGenerator.generateReport(FXCollections.observableArrayList(favoritedBusinesses));
    }

    // ==================== Inline Result Banner ====================

    /**
     * Shows the inline result banner below the search bar.
     *
     * @param message    Text to display
     * @param styleClass One of "result-banner-success", "result-banner-warning",
     *                   "result-banner-error"
     */
    private void showResultBanner(String message, String styleClass) {
        resultBannerLabel.setText(message);
        resultBanner.getStyleClass().removeAll(
                "result-banner-success", "result-banner-warning", "result-banner-error");
        resultBanner.getStyleClass().add(styleClass);
        resultBanner.setVisible(true);
        resultBanner.setManaged(true);
    }

    /** Hides the inline result banner. Called by the ✕ button in FXML. */
    @FXML
    private void hideResultBanner() {
        resultBanner.setVisible(false);
        resultBanner.setManaged(false);
    }

    /**
     * Displays an alert dialog to the user.
     * 
     * @param type    Alert type (INFORMATION, WARNING, ERROR)
     * @param title   Dialog title
     * @param message Dialog message
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
