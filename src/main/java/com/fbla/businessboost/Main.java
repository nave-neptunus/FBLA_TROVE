package com.fbla.businessboost;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main application entry point for FBLA Business Boost.
 * 
 * <p>
 * This class extends {@code javafx.application.Application} and serves as the
 * JavaFX
 * application launcher. It loads the main FXML view, applies styling, and
 * displays the
 * primary stage (main window).
 * </p>
 * 
 * <p>
 * <strong>JavaFX Application Lifecycle:</strong>
 * </p>
 * <ol>
 * <li><strong>init():</strong> Called before start(), used for initialization
 * (not overridden here)</li>
 * <li><strong>start(Stage):</strong> Main entry point where the UI is
 * constructed and displayed</li>
 * <li><strong>stop():</strong> Called when application is closing (not
 * overridden here)</li>
 * </ol>
 * 
 * <p>
 * <strong>Design Decisions:</strong>
 * </p>
 * <ul>
 * <li><strong>Window Size:</strong> 1200x700 provides optimal space for the
 * dashboard layout
 * with sidebar, main table, and deals pane without overwhelming smaller
 * screens.</li>
 * <li><strong>Minimum Size:</strong> Set to 900x600 to prevent layout collapse
 * when window
 * is resized, ensuring all UI elements remain usable.</li>
 * <li><strong>FXML Loading:</strong> Uses FXMLLoader to separate UI structure
 * (FXML) from
 * application logic (Java), following MVC best practices.</li>
 * </ul>
 * 
 * @author FBLA Development Team
 * @version 1.0
 */
public class Main extends Application {

    /**
     * Starts the JavaFX application and displays the main window.
     * 
     * <p>
     * This method is called by the JavaFX runtime after the application is
     * initialized.
     * It loads the MainView.fxml file, creates a Scene with the loaded content,
     * applies
     * the CSS stylesheet, and configures the primary Stage.
     * </p>
     * 
     * @param primaryStage The primary stage (main window) provided by JavaFX
     *                     runtime
     * @throws IOException If the FXML file cannot be loaded
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        // Load FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MainView.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 700);

        // Configure primary stage
        primaryStage.setTitle("FBLA Business Boost - Discover Local Businesses");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        // Display the window
        primaryStage.show();
    }

    /**
     * Application entry point.
     * 
     * <p>
     * This method is called when the application is launched from the command line
     * or IDE. It delegates to {@code Application.launch()}, which initializes the
     * JavaFX runtime and calls the {@code start()} method.
     * </p>
     * 
     * @param args Command line arguments (not used in this application)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
