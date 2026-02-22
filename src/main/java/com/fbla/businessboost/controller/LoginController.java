package com.fbla.businessboost.controller;

import com.fbla.businessboost.util.SessionManager;
import com.fbla.businessboost.util.SupabaseClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Random;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label botQuestionLabel;

    @FXML
    private TextField botAnswerField;

    @FXML
    private Label statusLabel;

    private int num1;
    private int num2;
    private int correctAnswer;

    @FXML
    public void initialize() {
        generateNewBotQuestion();
    }

    private void generateNewBotQuestion() {
        Random random = new Random();
        num1 = random.nextInt(10) + 1; // 1 to 10
        num2 = random.nextInt(10) + 1; // 1 to 10
        correctAnswer = num1 + num2;
        botQuestionLabel.setText(String.format("What is %d + %d?", num1, num2));
        botAnswerField.clear();
    }

    private boolean verifyBot() {
        String answerStr = botAnswerField.getText().trim();
        if (answerStr.isEmpty()) {
            statusLabel.setText("Please answer the bot verification.");
            return false;
        }

        try {
            int answer = Integer.parseInt(answerStr);
            if (answer == correctAnswer) {
                return true;
            } else {
                statusLabel.setText("Incorrect math answer. Please try again.");
                generateNewBotQuestion();
                return false;
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("Please enter a valid number.");
            return false;
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Username and password cannot be empty.");
            return;
        }

        if (!verifyBot()) {
            return;
        }

        statusLabel.setText("Authenticating...");

        // Run authentication in background
        new Thread(() -> {
            boolean success = SupabaseClient.authenticateUser(username, password);
            Platform.runLater(() -> {
                if (success) {
                    SessionManager.setCurrentUser(username);
                    loadMainApp();
                } else {
                    statusLabel.setText("Invalid username or password.");
                    generateNewBotQuestion();
                }
            });
        }).start();
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Username and password cannot be empty.");
            return;
        }

        if (!verifyBot()) {
            return;
        }

        statusLabel.setText("Registering...");

        // Run registration in background
        new Thread(() -> {
            boolean success = SupabaseClient.registerUser(username, password);
            Platform.runLater(() -> {
                if (success) {
                    SessionManager.setCurrentUser(username);
                    loadMainApp();
                } else {
                    statusLabel.setText("Registration failed. Username may already exist.");
                    generateNewBotQuestion();
                }
            });
        }).start();
    }

    private void loadMainApp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MainView.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle("FBLA Business Boost - Logged in as: " + SessionManager.getCurrentUser());
            stage.setScene(new Scene(root, 1000, 700));
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading main application window.");
        }
    }
}
