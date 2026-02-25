package com.fbla.businessboost.controller;

import com.fbla.businessboost.util.CaptchaServer;
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
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller for the login/registration screen.
 * Features:
 * - Google reCAPTCHA v2 verification (only required for registration)
 * - Social login (Google, Facebook, Yahoo) via OAuth popups
 * - Username/password auth via Supabase
 *
 * @author FBLA Development Team
 * @version 4.0
 */
public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label captchaStatusLabel;
    @FXML
    private Label statusLabel;

    /** Whether reCAPTCHA has been completed (only needed for Register) */
    private boolean captchaVerified = false;

    @FXML
    public void initialize() {
        // Nothing to load at startup — captcha opens on-demand for registration only
    }

    // ==================== Login (no captcha) ====================

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Username and password cannot be empty.");
            return;
        }

        statusLabel.setText("Signing in...");

        new Thread(() -> {
            boolean success = SupabaseClient.authenticateUser(username, password);
            Platform.runLater(() -> {
                if (success) {
                    SessionManager.setCurrentUser(username);
                    loadMainApp();
                } else {
                    statusLabel.setText("Invalid username or password.");
                }
            });
        }).start();
    }

    // ==================== Register (captcha required) ====================

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Username and password cannot be empty.");
            return;
        }

        // If not yet verified, open captcha popup first
        if (!captchaVerified) {
            statusLabel.setText("Please complete the reCAPTCHA to register.");
            openCaptchaPopup(() -> {
                // After captcha is verified, proceed with registration
                doRegister(username, password);
            });
            return;
        }

        doRegister(username, password);
    }

    private void doRegister(String username, String password) {
        statusLabel.setText("Creating account...");

        new Thread(() -> {
            boolean success = SupabaseClient.registerUser(username, password);
            Platform.runLater(() -> {
                if (success) {
                    SessionManager.setCurrentUser(username);
                    loadMainApp();
                } else {
                    statusLabel.setText("Registration failed. Username may already exist.");
                }
            });
        }).start();
    }

    // ==================== reCAPTCHA Popup ====================

    /**
     * Opens a popup window with Google reCAPTCHA v2. Calls onVerified when
     * complete.
     */
    private void openCaptchaPopup(Runnable onVerified) {
        Stage captchaStage = new Stage();
        captchaStage.initModality(Modality.APPLICATION_MODAL);
        captchaStage.initOwner(usernameField.getScene().getWindow());
        captchaStage.setTitle("🛡 Verify You're Human");
        captchaStage.setResizable(false);

        WebView webView = new WebView();
        webView.setPrefSize(340, 200);
        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        String captchaUrl = CaptchaServer.getInstance().getCaptchaUrl();
        engine.load(captchaUrl);

        engine.titleProperty().addListener((obs, oldTitle, newTitle) -> {
            if (newTitle != null && newTitle.startsWith("CAPTCHA_OK:")) {
                captchaVerified = true;
                Platform.runLater(() -> {
                    captchaStage.close();
                    statusLabel.setText("✅ Verified! Creating account...");
                    onVerified.run();
                });
            }
        });

        Scene scene = new Scene(webView, 340, 200);
        captchaStage.setScene(scene);
        captchaStage.showAndWait();
    }

    // ==================== Social Login (OAuth Popups) ====================

    @FXML
    private void handleGoogleLogin() {
        // Load Google's real OAuth page with account picker
        String googleUrl = CaptchaServer.getInstance().getGoogleOAuthUrl();
        openOAuthWebView(googleUrl, "Sign in with Google", 500, 650);
    }

    @FXML
    private void handleFacebookLogin() {
        String url = CaptchaServer.getInstance().getOAuthUrl("facebook");
        openOAuthWebView(url, "Sign in with Facebook", 420, 480);
    }

    @FXML
    private void handleYahooLogin() {
        String url = CaptchaServer.getInstance().getOAuthUrl("yahoo");
        openOAuthWebView(url, "Sign in with Yahoo", 420, 480);
    }

    /**
     * Opens a WebView popup with the given OAuth URL.
     * Listens for OAUTH_OK title signal to capture user info.
     */
    private void openOAuthWebView(String url, String title, int width, int height) {
        Stage oauthStage = new Stage();
        oauthStage.initModality(Modality.APPLICATION_MODAL);
        oauthStage.initOwner(usernameField.getScene().getWindow());
        oauthStage.setTitle(title);
        oauthStage.setResizable(true);

        WebView webView = new WebView();
        webView.setPrefSize(width, height);
        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        engine.load(url);

        // Listen for the OAUTH_OK signal from callback page
        engine.titleProperty().addListener((obs, oldTitle, newTitle) -> {
            if (newTitle != null && newTitle.startsWith("OAUTH_OK:")) {
                try {
                    String[] parts = newTitle.split(":", 4);
                    String name = java.net.URLDecoder.decode(parts[1], "UTF-8");

                    Platform.runLater(() -> {
                        SessionManager.setCurrentUser(name);
                        oauthStage.close();
                        loadMainApp();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> statusLabel.setText("Sign-in failed."));
                }
            }
        });

        Scene scene = new Scene(webView, width, height);
        oauthStage.setScene(scene);
        oauthStage.showAndWait();
    }

    // ==================== Navigation ====================

    private void loadMainApp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MainView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle("Commercio - Logged in as: " + SessionManager.getCurrentUser());
            stage.setScene(new Scene(root, 1200, 750));
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading main application.");
        }
    }
}
