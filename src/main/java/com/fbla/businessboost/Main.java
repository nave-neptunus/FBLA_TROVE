package com.fbla.businessboost;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Main application entry point for FBLA Business Boost — Trove.
 *
 * <p>
 * <strong>Startup flow:</strong>
 * <ol>
 *   <li>Load the app icon for the window/taskbar.</li>
 *   <li>Check for a persisted login session ({@link com.fbla.businessboost.util.SessionManager}).</li>
 *   <li>If logged in, check whether the onboarding wizard has been completed.</li>
 *   <li>If onboarding is pending, show {@code OnboardingView.fxml}.</li>
 *   <li>Otherwise, go directly to {@code MainView.fxml}.</li>
 *   <li>If not logged in, show {@code LoginView.fxml}.</li>
 * </ol>
 * </p>
 *
 * <p>
 * <strong>Design decisions:</strong>
 * <ul>
 *   <li><strong>Window size 1200×750:</strong> Optimal for the dashboard layout without
 *       overwhelming 1080p screens.</li>
 *   <li><strong>Minimum size 900×600:</strong> Prevents layout collapse on resize.</li>
 *   <li><strong>App icon:</strong> Bundled as a resource to avoid external file dependencies.</li>
 * </ul>
 * </p>
 *
 * @author FBLA Development Team
 * @version 2.0
 */
public class Main extends Application {

    /**
     * Starts the JavaFX application and displays the appropriate first screen.
     *
     * @param primaryStage The primary stage provided by the JavaFX runtime
     * @throws IOException If any FXML file cannot be loaded
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        // Set branded app icon
        try (InputStream iconStream = getClass().getResourceAsStream("/icons/app_icon.png")) {
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception e) {
            System.err.println("Could not load app icon: " + e.getMessage());
        }

        if (com.fbla.businessboost.util.SessionManager.isLoggedIn()) {
            String user = com.fbla.businessboost.util.SessionManager.getCurrentUser();

            if (!com.fbla.businessboost.util.SessionManager.isOnboardingComplete()) {
                // First login — show onboarding wizard
                java.net.URL resource = getClass().getResource("/view/OnboardingView.fxml");
                if (resource == null) throw new IOException("Cannot find /view/OnboardingView.fxml");
                FXMLLoader loader = new FXMLLoader(resource);
                Scene scene = new Scene(loader.load(), 600, 500);
                primaryStage.setTitle("Welcome to Trove!");
                primaryStage.setScene(scene);
                primaryStage.setMinWidth(500);
                primaryStage.setMinHeight(460);
            } else {
                // Returning user — go straight to the dashboard
                java.net.URL resource = getClass().getResource("/view/MainView.fxml");
                if (resource == null) throw new IOException("Cannot find /view/MainView.fxml");
                FXMLLoader loader = new FXMLLoader(resource);
                Scene scene = new Scene(loader.load(), 1200, 750);
                primaryStage.setTitle("Trove — Logged in as: " + user);
                primaryStage.setScene(scene);
                primaryStage.setMinWidth(900);
                primaryStage.setMinHeight(600);
            }
        } else {
            // Not logged in — show login screen
            java.net.URL resource = getClass().getResource("/view/LoginView.fxml");
            if (resource == null) throw new IOException("Cannot find /view/LoginView.fxml");
            FXMLLoader loader = new FXMLLoader(resource);
            Scene scene = new Scene(loader.load(), 850, 550);
            primaryStage.setTitle("Trove — Sign In");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(850);
            primaryStage.setMinHeight(550);
        }

        primaryStage.show();
    }

    /**
     * Application entry point — delegates to {@link Application#launch(String...)}.
     *
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
