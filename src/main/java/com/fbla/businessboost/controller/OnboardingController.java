package com.fbla.businessboost.controller;

import com.fbla.businessboost.util.SessionManager;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the first-launch onboarding wizard.
 *
 * <p>
 * The wizard shows three steps sequentially:
 * <ol>
 *   <li><strong>Welcome</strong> — brand introduction</li>
 *   <li><strong>ZIP Code</strong> — pre-fills the main search bar for instant results</li>
 *   <li><strong>Category Preferences</strong> — bootstraps the recommendation engine</li>
 * </ol>
 * Preferences are persisted to {@link SessionManager} so they survive app restarts.
 * </p>
 *
 * <p>
 * Page transitions use a 250ms {@link FadeTransition} on the incoming step for
 * a polished feel.
 * </p>
 *
 * @author FBLA Development Team
 * @version 1.0
 */
public class OnboardingController {

    // ==================== FXML Injected ====================

    @FXML private VBox step1;
    @FXML private VBox step2;
    @FXML private VBox step3;

    @FXML private TextField zipCodeInput;
    @FXML private Label zipErrorLabel;

    @FXML private ToggleButton toggleFood;
    @FXML private ToggleButton toggleRetail;
    @FXML private ToggleButton toggleServices;

    // ==================== Event Handlers ====================

    /**
     * Advances from the Welcome step to the ZIP code step.
     */
    @FXML
    private void handleStep1Next() {
        transitionTo(step1, step2);
    }

    /**
     * Validates the ZIP code and advances to the category step.
     * Shows an inline error if the ZIP is invalid.
     */
    @FXML
    private void handleStep2Next() {
        String zip = zipCodeInput.getText().trim();
        if (zip.isEmpty() || !zip.matches("\\d{5}")) {
            zipErrorLabel.setText("Please enter a valid 5-digit ZIP code.");
            zipErrorLabel.setVisible(true);
            zipErrorLabel.setManaged(true);
            return;
        }
        // Save the preferred ZIP
        SessionManager.setPreferredZip(zip);
        zipErrorLabel.setVisible(false);
        zipErrorLabel.setManaged(false);
        transitionTo(step2, step3);
    }

    /**
     * Saves category preferences and launches the main dashboard.
     */
    @FXML
    private void handleFinish() {
        savePreferencesAndLaunch();
    }

    /**
     * Skips the wizard entirely and goes directly to the main dashboard.
     * The wizard will not be shown again (onboarding is marked complete).
     */
    @FXML
    private void handleSkip() {
        SessionManager.markOnboardingComplete();
        loadMainApp();
    }

    // ==================== Helpers ====================

    /**
     * Fades out the current step and fades in the next step.
     *
     * @param from the currently visible step VBox
     * @param to   the step VBox to show next
     */
    private void transitionTo(VBox from, VBox to) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(180), from);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            from.setVisible(false);
            from.setManaged(false);
            to.setOpacity(0.0);
            to.setVisible(true);
            to.setManaged(true);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(250), to);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    /**
     * Saves selected category preferences to SessionManager, marks onboarding
     * complete, and loads the main dashboard.
     */
    private void savePreferencesAndLaunch() {
        List<String> selected = new ArrayList<>();
        if (toggleFood.isSelected())     selected.add("Food");
        if (toggleRetail.isSelected())   selected.add("Retail");
        if (toggleServices.isSelected()) selected.add("Services");
        SessionManager.setPreferredCategories(String.join("|", selected));
        SessionManager.markOnboardingComplete();
        loadMainApp();
    }

    /**
     * Loads and displays the main dashboard scene.
     */
    private void loadMainApp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MainView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) step1.getScene().getWindow();
            stage.setTitle("Commercio — Logged in as: " + SessionManager.getCurrentUser());
            stage.setScene(new Scene(root, 1200, 750));
            stage.setMinWidth(900);
            stage.setMinHeight(600);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
