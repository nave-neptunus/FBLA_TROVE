package com.fbla.businessboost.controller;

import com.fbla.businessboost.model.Business;
import com.fbla.businessboost.util.SupabaseClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.util.*;

/**
 * Controller for the review submission dialog.
 * Features:
 * - Google reCAPTCHA v2 verification via embedded WebView
 * - Sentiment validation (warns if low rating contradicts positive text)
 * - Weighted average rating updates
 *
 * @author FBLA Development Team
 * @version 3.0
 */
public class ReviewDialogController {

    @FXML
    private Slider ratingSlider;
    @FXML
    private Label ratingValueLabel;
    @FXML
    private TextArea reviewTextArea;
    @FXML
    private WebView recaptchaWebView;
    @FXML
    private Label captchaStatusLabel;
    @FXML
    private HBox sentimentWarningBox;
    @FXML
    private Label sentimentWarningLabel;
    @FXML
    private TextField honeypotField;
    @FXML
    private Label mathChallengeLabel;
    @FXML
    private TextField mathAnswerField;

    private Business business;
    private int expectedMathAnswer;

    /** Whether the reCAPTCHA has been successfully completed */
    private boolean captchaVerified = false;

    /** Positive sentiment keywords for mismatch detection */
    private static final String[] POSITIVE_WORDS = {
            "great", "amazing", "excellent", "wonderful", "fantastic", "love",
            "awesome", "outstanding", "perfect", "incredible", "best", "superb",
            "delicious", "friendly", "recommend", "impressed", "enjoyed"
    };

    /** Negative sentiment keywords */
    private static final String[] NEGATIVE_WORDS = {
            "terrible", "awful", "horrible", "worst", "disgusting", "rude",
            "never", "avoid", "hate", "disappointing", "waste", "unacceptable"
    };

    @FXML
    public void initialize() {
        // Configure rating slider (1 to 5, step 1)
        ratingSlider.setMin(1);
        ratingSlider.setMax(5);
        ratingSlider.setValue(3);
        ratingSlider.setBlockIncrement(1);
        ratingSlider.setMajorTickUnit(1);
        ratingSlider.setMinorTickCount(0);
        ratingSlider.setSnapToTicks(true);
        ratingSlider.setShowTickLabels(true);

        // Update display label as slider moves
        ratingSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int stars = newVal.intValue();
            String starStr = "★".repeat(stars) + "☆".repeat(5 - stars);
            ratingValueLabel.setText(starStr + " " + stars);
            checkSentiment();
        });

        // Set initial display
        ratingValueLabel.setText("★★★☆☆ 3");

        // Add sentiment check on text change too
        reviewTextArea.textProperty().addListener((obs, oldVal, newVal) -> checkSentiment());

        // Setup Proof of Work Math Challenge
        int[] pow = com.fbla.businessboost.util.ReviewTrustEngine.generateProofOfWork();
        mathChallengeLabel.setText("What is " + pow[0] + " + " + pow[1] + "?");
        expectedMathAnswer = pow[2];

        // Load reCAPTCHA
        loadRecaptcha();
    }

    /**
     * Loads Google reCAPTCHA v2 from the local CaptchaServer into the WebView.
     */
    private void loadRecaptcha() {
        WebEngine engine = recaptchaWebView.getEngine();
        engine.setJavaScriptEnabled(true);

        // Load from localhost to satisfy reCAPTCHA domain requirement
        String captchaUrl = com.fbla.businessboost.util.CaptchaServer.getInstance().getCaptchaUrl();
        engine.load(captchaUrl);

        // Listen for the title change as the callback signal
        engine.titleProperty().addListener((obs, oldTitle, newTitle) -> {
            if (newTitle != null && newTitle.startsWith("CAPTCHA_OK:")) {
                captchaVerified = true;
                Platform.runLater(() -> {
                    captchaStatusLabel.setText("Verification complete");
                    captchaStatusLabel.getStyleClass().add("caption-success");
                });
            }
        });
    }

    /**
     * Checks for sentiment mismatch between rating and review text.
     */
    private void checkSentiment() {
        String text = reviewTextArea.getText();
        int rating = (int) ratingSlider.getValue();

        if (text == null || text.trim().isEmpty()) {
            hideSentimentWarning();
            return;
        }

        String lowerText = text.toLowerCase();
        boolean hasPositive = Arrays.stream(POSITIVE_WORDS).anyMatch(lowerText::contains);
        boolean hasNegative = Arrays.stream(NEGATIVE_WORDS).anyMatch(lowerText::contains);

        if (rating <= 2 && hasPositive && !hasNegative) {
            showSentimentWarning("Warning: Your rating seems low for such a positive review. Is this intentional?");
        } else if (rating >= 4 && hasNegative && !hasPositive) {
            showSentimentWarning("Warning: Your rating seems high given the negative feedback. Is this intentional?");
        } else {
            hideSentimentWarning();
        }
    }

    private void showSentimentWarning(String message) {
        sentimentWarningLabel.setText(message);
        sentimentWarningBox.setVisible(true);
        sentimentWarningBox.setManaged(true);
    }

    private void hideSentimentWarning() {
        sentimentWarningBox.setVisible(false);
        sentimentWarningBox.setManaged(false);
    }

    /**
     * Sets the business being reviewed.
     */
    public void setBusiness(Business business) {
        this.business = business;
    }

    /**
     * Handles review submission.
     */
    @FXML
    private void handleSubmit() {
        // Step 0: Rate Limiting
        String username = com.fbla.businessboost.util.SessionManager.getCurrentUser();
        if (com.fbla.businessboost.util.ReviewTrustEngine.isRateLimited(username)) {
            showError("You are submitting reviews too quickly. Please wait 10 seconds before trying again.");
            return;
        }

        // Step 1: Layered Bot Defenses
        if (!captchaVerified) {
            showError("Please complete the reCAPTCHA verification to prove you're human.");
            return;
        }
        
        // Honeypot check (bots fill hidden fields)
        if (honeypotField != null && !honeypotField.getText().isEmpty()) {
            showError("Automated bot submission detected (honeypot triggered).");
            return;
        }
        
        // Proof of Work check
        try {
            int answer = Integer.parseInt(mathAnswerField.getText().trim());
            if (answer != expectedMathAnswer) {
                showError("Incorrect math answer. Please try again.");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Please enter a valid number for the math challenge.");
            return;
        }

        // Step 2: Validate review text
        String reviewText = reviewTextArea.getText();
        if (reviewText == null || reviewText.trim().length() < 10) {
            showError("Please write a review with at least 10 characters.");
            return;
        }

        // Step 3: Update business rating with TRULY weighted average using TrustEngine
        double currentRating = business.getRating();
        double newRating = ratingSlider.getValue();
        // Assume base business has 5 reviews for weighting math
        final double updatedRating = com.fbla.businessboost.util.ReviewTrustEngine.weightRating(username, newRating, currentRating, 5);
        business.setRating(updatedRating);

        // Step 4: Save review to Supabase and track locally
        com.fbla.businessboost.util.SessionManager.incrementReviewCount();
        new Thread(() -> {
            SupabaseClient.saveReview(business.getName(), newRating, reviewText.trim());
            Platform.runLater(() -> {
                showSuccess("Review submitted successfully! Rating updated to "
                        + String.format("%.1f", updatedRating) + " ★");
            });
        }).start();

        closeDialog();
    }

    @FXML
    private void handleSkipCaptcha() {
        this.captchaVerified = true;
        Platform.runLater(() -> {
            captchaStatusLabel.setText("Presentation Mode: Captcha Skipped");
            captchaStatusLabel.getStyleClass().add("caption-success");
        });
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) ratingSlider.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
