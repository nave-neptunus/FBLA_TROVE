package com.fbla.businessboost.controller;

import com.fbla.businessboost.model.Business;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.Random;

/**
 * Controller for the review submission dialog with bot verification.
 * 
 * <p>
 * This controller implements a simple arithmetic challenge to prevent automated
 * bot
 * submissions while remaining accessible to human users. The challenge
 * generates a random
 * addition problem (e.g., "What is 7 + 5?") that must be answered correctly
 * before the
 * review can be submitted.
 * </p>
 * 
 * <p>
 * <strong>Bot Verification Design:</strong>
 * </p>
 * <ul>
 * <li><strong>Random Generation:</strong> Uses {@code java.util.Random} to
 * generate two
 * numbers between 1 and 20, ensuring variety and preventing memorization.</li>
 * <li><strong>Simple Arithmetic:</strong> Addition only, keeping the challenge
 * accessible
 * to all users including those with cognitive disabilities (accessibility
 * requirement).</li>
 * <li><strong>Validation Timing:</strong> Answer is checked only on submit, not
 * on input,
 * to avoid frustrating users with premature error messages.</li>
 * </ul>
 * 
 * <p>
 * <strong>Rating Update Logic:</strong> When a review is submitted
 * successfully, the new
 * rating is averaged with the existing rating using a weighted approach. This
 * prevents a
 * single review from drastically changing the overall rating while still
 * reflecting new
 * feedback.
 * </p>
 * 
 * @author FBLA Development Team
 * @version 1.0
 */
public class ReviewDialogController {

    // ==================== FXML Injected Components ====================

    @FXML
    private Slider ratingSlider;
    @FXML
    private Label ratingValueLabel;
    @FXML
    private TextArea reviewTextArea;
    @FXML
    private Label botQuestionLabel;
    @FXML
    private TextField botAnswerField;

    // ==================== Instance Variables ====================

    /** The business being reviewed */
    private Business business;

    /** Random number generator for bot verification */
    private final Random random = new Random();

    /** First number in the arithmetic challenge */
    private int num1;

    /** Second number in the arithmetic challenge */
    private int num2;

    /** Correct answer to the arithmetic challenge */
    private int correctAnswer;

    /**
     * Initializes the dialog controller.
     * Called automatically by JavaFX after FXML loading.
     * 
     * <p>
     * Sets up the rating slider with a listener that updates the display label
     * in real-time as the user drags the slider.
     * </p>
     */
    @FXML
    public void initialize() {
        // Configure rating slider (1.0 to 5.0, default 3.0)
        ratingSlider.setMin(1.0);
        ratingSlider.setMax(5.0);
        ratingSlider.setValue(3.0);
        ratingSlider.setMajorTickUnit(1.0);
        ratingSlider.setMinorTickCount(0);
        ratingSlider.setShowTickLabels(true);
        ratingSlider.setShowTickMarks(true);
        ratingSlider.setSnapToTicks(true);

        // Update label when slider value changes
        ratingSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            ratingValueLabel.setText(String.format("%.0f Stars", newValue.doubleValue()));
        });

        // Initialize label
        ratingValueLabel.setText("3 Stars");

        // Generate bot verification question
        generateBotQuestion();
    }

    /**
     * Sets the business being reviewed and updates the dialog title context.
     * Must be called before showing the dialog.
     * 
     * @param business The business to review
     */
    public void setBusiness(Business business) {
        this.business = business;
    }

    /**
     * Generates a random arithmetic challenge for bot verification.
     * 
     * <p>
     * <strong>Algorithm:</strong> Generates two random integers between 1 and 20,
     * calculates their sum, and displays the question to the user. The range 1-20
     * is chosen to be simple enough for mental math while providing sufficient
     * variety.
     * </p>
     * 
     * <p>
     * <strong>Accessibility Consideration:</strong> Uses addition rather than
     * multiplication or division to ensure the challenge is solvable by users with
     * varying mathematical abilities.
     * </p>
     */
    private void generateBotQuestion() {
        num1 = random.nextInt(20) + 1; // Random number 1-20
        num2 = random.nextInt(20) + 1; // Random number 1-20
        correctAnswer = num1 + num2;

        botQuestionLabel.setText(String.format("Bot Verification: What is %d + %d?", num1, num2));
    }

    /**
     * Handles the submit button click.
     * 
     * <p>
     * <strong>Validation Steps:</strong>
     * </p>
     * <ol>
     * <li>Verify bot answer is correct</li>
     * <li>Validate review text is not empty</li>
     * <li>Update business rating with weighted average</li>
     * <li>Close dialog</li>
     * </ol>
     * 
     * <p>
     * <strong>Rating Calculation:</strong> Uses a weighted average where the
     * existing
     * rating has weight 5 (representing previous reviews) and the new rating has
     * weight 1.
     * Formula: newRating = (oldRating * 5 + userRating) / 6
     * </p>
     * 
     * <p>
     * This approach prevents rating volatility while still incorporating new
     * feedback.
     * </p>
     */
    @FXML
    private void handleSubmit() {
        // Validate bot answer
        String userAnswer = botAnswerField.getText().trim();

        if (userAnswer.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Verification Required",
                    "Please answer the bot verification question.");
            return;
        }

        try {
            int answer = Integer.parseInt(userAnswer);
            if (answer != correctAnswer) {
                showAlert(Alert.AlertType.ERROR, "Incorrect Answer",
                        String.format("The answer to %d + %d is not %d. Please try again.",
                                num1, num2, answer));
                botAnswerField.clear();
                botAnswerField.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input",
                    "Please enter a valid number for the verification question.");
            botAnswerField.clear();
            botAnswerField.requestFocus();
            return;
        }

        // Validate review text
        String reviewText = reviewTextArea.getText().trim();
        if (reviewText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Review Required",
                    "Please enter a review before submitting.");
            return;
        }

        // Update business rating with weighted average
        // Formula: (oldRating * 5 + newRating * 1) / 6
        // This gives more weight to existing rating to prevent volatility
        double currentRating = business.getRating();
        double newRating = ratingSlider.getValue();
        double updatedRating = (currentRating * 5 + newRating) / 6.0;

        business.setRating(updatedRating);

        // Show success message
        showAlert(Alert.AlertType.INFORMATION, "Review Submitted",
                String.format("Thank you for your review!\n\n" +
                        "Your rating: %.0f stars\n" +
                        "Updated business rating: %.1f stars",
                        newRating, updatedRating));

        // Close dialog
        closeDialog();
    }

    /**
     * Handles the cancel button click.
     * Closes the dialog without saving changes.
     */
    @FXML
    private void handleCancel() {
        closeDialog();
    }

    /**
     * Closes the review dialog.
     */
    private void closeDialog() {
        Stage stage = (Stage) ratingSlider.getScene().getWindow();
        stage.close();
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
