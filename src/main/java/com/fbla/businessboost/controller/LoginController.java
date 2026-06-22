package com.fbla.businessboost.controller;

import com.fbla.businessboost.util.CaptchaServer;
import com.fbla.businessboost.util.SessionManager;
import com.fbla.businessboost.util.SupabaseClient;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Controller for the redesigned premium login/registration screen.
 * Features:
 * - Dynamic mode switching (Sign In vs Sign Up)
 * - Auto-sliding branding text carousel with dot indicators
 * - Password visibility toggle (eye icon)
 * - Input validation & terms check
 * - Google reCAPTCHA v2 verification (registration only)
 * - macOS Biometric Authentication (Touch ID / Face ID) for Google/Apple button clicks
 * - Social login (Google, Apple, Facebook, Yahoo) via OAuth WebView popups (fallback)
 * - Credentials verification via Supabase
 */
public class LoginController {

    // Redesigned Fields
    @FXML
    private Label titleLabel;
    @FXML
    private Label subtitleText;
    @FXML
    private Button subtitleLink;
    @FXML
    private HBox nameFieldsBox;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField passwordTextField;
    @FXML
    private Button passwordToggleBtn;
    @FXML
    private CheckBox termsCheckbox;
    @FXML
    private HBox termsContainer;
    @FXML
    private Button actionButton;
    @FXML
    private Label dividerLabel;
    @FXML
    private Label captchaStatusLabel;
    @FXML
    private Label statusLabel;

    // Carousel Fields
    @FXML
    private Label carouselText;
    @FXML
    private Rectangle indicator0;
    @FXML
    private Rectangle indicator1;
    @FXML
    private Rectangle indicator2;

    private Rectangle[] indicators;
    private int currentSlideIndex = 0;
    private Timeline carouselTimeline;
    private final String[] carouselSlides = {
        "Discover Local Treasures, Boost Your Impact",
        "Empowering Businesses, Growing Communities",
        "Navigate Premium Insights with Trove"
    };

    /** True if in Sign Up (registration) mode, false for Sign In */
    private boolean isSignUpMode = true;

    /** Whether reCAPTCHA has been completed (only needed for Sign Up) */
    private boolean captchaVerified = false;

    @FXML
    public void initialize() {
        // Bidirectional binding for show/hide password text fields
        passwordField.textProperty().bindBidirectional(passwordTextField.textProperty());

        // Initialize carousel dot array
        indicators = new Rectangle[]{indicator0, indicator1, indicator2};

        // Start text carousel auto-rotation
        startCarousel();

        // Start with Sign Up view (mockup default)
        setMode(true);
    }

    // ==================== Carousel Timeline ====================

    private void startCarousel() {
        carouselTimeline = new Timeline(new KeyFrame(Duration.seconds(4), event -> {
            currentSlideIndex = (currentSlideIndex + 1) % carouselSlides.length;
            updateCarousel();
        }));
        carouselTimeline.setCycleCount(Timeline.INDEFINITE);
        carouselTimeline.play();
    }

    private void updateCarousel() {
        carouselText.setText(carouselSlides[currentSlideIndex]);
        for (int i = 0; i < indicators.length; i++) {
            if (i == currentSlideIndex) {
                indicators[i].getStyleClass().clear();
                indicators[i].getStyleClass().add("carousel-dot-active");
                indicators[i].setWidth(24.0);
            } else {
                indicators[i].getStyleClass().clear();
                indicators[i].getStyleClass().add("carousel-dot");
                indicators[i].setWidth(8.0);
            }
        }
    }

    // ==================== Mode Toggling ====================

    @FXML
    private void handleToggleMode() {
        setMode(!isSignUpMode);
    }

    private void setMode(boolean signUpMode) {
        this.isSignUpMode = signUpMode;
        statusLabel.setText("");

        if (signUpMode) {
            titleLabel.setText("Create an account");
            subtitleText.setText("Already have an account?");
            subtitleLink.setText("Log in");
            
            nameFieldsBox.setVisible(true);
            nameFieldsBox.setManaged(true);
            termsContainer.setVisible(true);
            termsContainer.setManaged(true);
            
            actionButton.setText("Create account");
            dividerLabel.setText("Or register with");
        } else {
            titleLabel.setText("Log in to your account");
            subtitleText.setText("Don't have an account?");
            subtitleLink.setText("Sign up");
            
            nameFieldsBox.setVisible(false);
            nameFieldsBox.setManaged(false);
            termsContainer.setVisible(false);
            termsContainer.setManaged(false);
            
            actionButton.setText("Log In");
            dividerLabel.setText("Or log in with");
        }
    }

    // ==================== Password Toggle ====================

    @FXML
    private void togglePasswordVisibility() {
        if (passwordField.isVisible()) {
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
            passwordToggleBtn.setText("🙈");
        } else {
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordToggleBtn.setText("👁");
        }
    }

    // ==================== Actions Dispatcher ====================

    @FXML
    private void handleMainAction() {
        if (isSignUpMode) {
            handleRegister();
        } else {
            handleLogin();
        }
    }

    // ==================== Login Flow ====================

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Email and password cannot be empty.");
            return;
        }

        statusLabel.setText("Signing in...");

        new Thread(() -> {
            boolean success = SupabaseClient.authenticateUser(username, password);
            Platform.runLater(() -> {
                if (success) {
                    SessionManager.setCurrentUser(username);
                    stopCarouselTimeline();
                    loadMainApp();
                } else {
                    statusLabel.setText("Invalid credentials.");
                }
            });
        }).start();
    }

    // ==================== Registration Flow ====================

    private void handleRegister() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (firstName.isEmpty() || lastName.isEmpty()) {
            statusLabel.setText("First name and last name cannot be empty.");
            return;
        }

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Email and password cannot be empty.");
            return;
        }

        if (!termsCheckbox.isSelected()) {
            statusLabel.setText("You must agree to the Terms & Conditions.");
            return;
        }

        // If not yet verified, open captcha popup first
        if (!captchaVerified) {
            statusLabel.setText("Please complete the reCAPTCHA to register.");
            openCaptchaPopup(() -> {
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
                    stopCarouselTimeline();
                    loadMainApp();
                } else {
                    statusLabel.setText("Registration failed. Email may already exist.");
                }
            });
        }).start();
    }

    // ==================== reCAPTCHA Popup ====================

    private void openCaptchaPopup(Runnable onVerified) {
        Stage captchaStage = new Stage();
        captchaStage.initModality(Modality.APPLICATION_MODAL);
        captchaStage.initOwner(usernameField.getScene().getWindow());
        captchaStage.setTitle("Verify You're Human");
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
                    statusLabel.setText("Verified! Creating account...");
                    onVerified.run();
                });
            }
        });

        Scene scene = new Scene(webView, 340, 200);
        captchaStage.setScene(scene);
        captchaStage.showAndWait();
    }

    // ==================== Social & Biometric Logins ====================

    @FXML
    private void handleGoogleLogin() {
        String email = java.util.prefs.Preferences.userNodeForPackage(LoginController.class).get("google_remembered_email", null);
        if (email != null) {
            statusLabel.setText("Checking biometric authentication...");
            new Thread(() -> {
                boolean biometricSuccess = runBiometricAuth("Google", email);
                if (!biometricSuccess) {
                    Platform.runLater(() -> {
                        statusLabel.setText("");
                        String googleUrl = CaptchaServer.getInstance().getGoogleOAuthUrl();
                        openOAuthWebView(googleUrl, "Sign in with Google", 500, 650, "Google");
                    });
                }
            }).start();
        } else {
            statusLabel.setText("");
            String googleUrl = CaptchaServer.getInstance().getGoogleOAuthUrl();
            openOAuthWebView(googleUrl, "Sign in with Google", 500, 650, "Google");
        }
    }

    @FXML
    private void handleAppleLogin() {
        String email = java.util.prefs.Preferences.userNodeForPackage(LoginController.class).get("apple_remembered_email", null);
        if (email != null) {
            statusLabel.setText("Checking biometric authentication...");
            new Thread(() -> {
                boolean biometricSuccess = runBiometricAuth("Apple", email);
                if (!biometricSuccess) {
                    Platform.runLater(() -> {
                        statusLabel.setText("");
                        String appleUrl = CaptchaServer.getInstance().getOAuthUrl("apple");
                        openOAuthWebView(appleUrl, "Sign in with Apple", 420, 500, "Apple");
                    });
                }
            }).start();
        } else {
            statusLabel.setText("");
            String appleUrl = CaptchaServer.getInstance().getOAuthUrl("apple");
            openOAuthWebView(appleUrl, "Sign in with Apple", 420, 500, "Apple");
        }
    }

    /**
     * Executes the compiled macOS biometric helper script.
     * Returns true if Touch ID succeeded.
     */
    private boolean runBiometricAuth(String provider, String email) {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("mac")) {
            return false;
        }

        try {
            java.io.File helper = new java.io.File("bin/BiometricHelper");
            if (!helper.exists()) {
                System.out.println("BiometricHelper binary not found. Falling back to web view.");
                return false;
            }

            Process process = new ProcessBuilder("bin/BiometricHelper").start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                Platform.runLater(() -> handleBiometricLogin(provider, email));
                return true;
            } else {
                System.out.println("Biometric authentication returned code: " + exitCode);
            }
        } catch (Exception e) {
            System.err.println("Biometric error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Authenticates and registers a biometric credential against the backend.
     */
    private void handleBiometricLogin(String provider, String email) {
        String username = email;
        String password = "biometric_secure_pass_123!"; // Fixed secure password for biometric users
        
        statusLabel.setText("Syncing biometric credentials with backend...");

        new Thread(() -> {
            boolean success = SupabaseClient.authenticateUser(username, password);
            if (!success) {
                boolean registered = SupabaseClient.registerUser(username, password);
                if (registered) {
                    success = SupabaseClient.authenticateUser(username, password);
                }
            }
            final boolean loginOk = success;
            Platform.runLater(() -> {
                if (loginOk) {
                    SessionManager.setCurrentUser(username);
                    stopCarouselTimeline();
                    loadMainApp();
                } else {
                    statusLabel.setText("Biometric backend synchronization failed.");
                }
            });
        }).start();
    }

    private void handleOAuthSuccess(String provider, String email, String name) {
        // Save the email to local preferences to remember it for biometrics next time
        String prefKey = provider.toLowerCase() + "_remembered_email";
        java.util.prefs.Preferences.userNodeForPackage(LoginController.class).put(prefKey, email);

        statusLabel.setText("Syncing account with backend...");

        new Thread(() -> {
            String username = email;
            String password = "biometric_secure_pass_123!";
            boolean success = SupabaseClient.authenticateUser(username, password);
            if (!success) {
                boolean registered = SupabaseClient.registerUser(username, password);
                if (registered) {
                    success = SupabaseClient.authenticateUser(username, password);
                }
            }
            final boolean loginOk = success;
            Platform.runLater(() -> {
                if (loginOk) {
                    SessionManager.setCurrentUser(username);
                    stopCarouselTimeline();
                    loadMainApp();
                } else {
                    statusLabel.setText("Backend synchronization failed.");
                }
            });
        }).start();
    }

    private void openOAuthWebView(String url, String title, int width, int height, String provider) {
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

        engine.titleProperty().addListener((obs, oldTitle, newTitle) -> {
            if (newTitle != null && newTitle.startsWith("OAUTH_OK:")) {
                try {
                    String[] parts = newTitle.split(":", 4);
                    String name = java.net.URLDecoder.decode(parts[1], "UTF-8");
                    String email = java.net.URLDecoder.decode(parts[2], "UTF-8");

                    Platform.runLater(() -> {
                        oauthStage.close();
                        handleOAuthSuccess(provider, email, name);
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

    // ==================== Extra Redirection / Dialog Actions ====================

    @FXML
    private void handleBackToWebsite() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.initOwner(usernameField.getScene().getWindow());
        alert.setTitle("Website Redirection");
        alert.setHeaderText(null);
        alert.setContentText("Redirecting you back to the main Trove portal...");
        alert.showAndWait();
    }

    @FXML
    private void handleShowTerms() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.initOwner(usernameField.getScene().getWindow());
        alert.setTitle("Terms & Conditions");
        alert.setHeaderText("Trove Terms of Service");
        alert.setContentText("Welcome to Trove!\n\nBy using our platform, you agree to: \n"
                + "1. Provide accurate registration details.\n"
                + "2. Maintain account confidentiality.\n"
                + "3. Adhere to community standards for business reviews.\n\n"
                + "Your data security and privacy are protected in accordance with FBLA submission standards.");
        alert.showAndWait();
    }

    private void stopCarouselTimeline() {
        if (carouselTimeline != null) {
            carouselTimeline.stop();
        }
    }

    // ==================== Navigation ====================

    private void loadMainApp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MainView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle("Trove - Logged in as: " + SessionManager.getCurrentUser());
            stage.setScene(new Scene(root, 1200, 750));
            stage.setMinWidth(900);
            stage.setMinHeight(600);
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading main application.");
        }
    }
}
