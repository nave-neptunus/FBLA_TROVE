package com.fbla.businessboost.util;

import com.fbla.businessboost.model.Business;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generates formatted reports for favorited businesses and writes them to a
 * text file.
 * 
 * <p>
 * This class fulfills the "Data Analysis" requirement by iterating through a
 * filtered
 * list of favorite businesses and creating a structured summary report. The
 * report includes
 * business names, categories, and average ratings, formatted for easy
 * readability.
 * </p>
 * 
 * <p>
 * <strong>File I/O Choice:</strong> Uses {@code BufferedWriter} wrapped around
 * {@code FileWriter}
 * for efficient text file writing. BufferedWriter reduces the number of I/O
 * operations by batching
 * writes, improving performance when generating reports with many businesses.
 * </p>
 * 
 * <p>
 * <strong>Error Handling:</strong> All file operations are wrapped in try-catch
 * blocks to handle
 * {@code IOException}. Errors are presented to the user via JavaFX Alert
 * dialogs rather than
 * console output, maintaining a professional desktop application experience.
 * </p>
 * 
 * @author FBLA Development Team
 * @version 1.0
 */
public class ReportGenerator {

    /** Default filename for the generated report */
    private static final String REPORT_FILENAME = "SupportReport.txt";

    /** Date/time format for report timestamp */
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Generates a support report for all favorited businesses and writes it to
     * SupportReport.txt.
     * 
     * <p>
     * The report includes:
     * </p>
     * <ul>
     * <li>Timestamp of report generation</li>
     * <li>Total count of favorited businesses</li>
     * <li>For each business: name, category, and rating</li>
     * </ul>
     * 
     * <p>
     * <strong>Algorithm:</strong> Iterates through the provided ObservableList
     * (which should
     * be pre-filtered to contain only favorites) and formats each business into a
     * structured
     * text block separated by horizontal rules.
     * </p>
     * 
     * @param favorites ObservableList of Business objects marked as favorites
     * @return true if report was successfully generated, false otherwise
     */
    public static boolean generateReport(ObservableList<Business> favorites) {
        // Validate input
        if (favorites == null || favorites.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Favorites",
                    "You haven't marked any businesses as favorites yet. " +
                            "Add some favorites before generating a report.");
            return false;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(REPORT_FILENAME))) {
            // Write report header
            writer.write("═══════════════════════════════════════════════════════\n");
            writer.write("        FBLA BUSINESS BOOST - SUPPORT REPORT\n");
            writer.write("═══════════════════════════════════════════════════════\n");
            writer.write("\n");

            // Write timestamp
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            writer.write("Generated: " + timestamp + "\n");
            writer.write("Total Favorites: " + favorites.size() + "\n");
            writer.write("\n");
            writer.write("───────────────────────────────────────────────────────\n");
            writer.write("\n");

            // Iterate through favorites and write formatted entries
            for (int i = 0; i < favorites.size(); i++) {
                Business business = favorites.get(i);

                writer.write(String.format("Business #%d\n", i + 1));
                writer.write(String.format("Name: %s\n", business.getName()));
                writer.write(String.format("Category: %s\n", business.getCategory()));
                writer.write(String.format("Average Rating: %.1f/5.0\n", business.getRating()));

                // Include deals if available
                if (!business.getDeals().isEmpty()) {
                    writer.write("Current Deals:\n");
                    for (String deal : business.getDeals()) {
                        writer.write("  • " + deal + "\n");
                    }
                }

                writer.write("\n");
                writer.write("───────────────────────────────────────────────────────\n");
                writer.write("\n");
            }

            // Write footer
            writer.write("End of Report\n");
            writer.write("═══════════════════════════════════════════════════════\n");

            // Show success message
            showAlert(Alert.AlertType.INFORMATION, "Report Generated",
                    String.format("Support report successfully created!\n\n" +
                            "File: %s\n" +
                            "Total businesses: %d",
                            REPORT_FILENAME, favorites.size()));

            return true;

        } catch (IOException e) {
            // Handle file writing errors
            showAlert(Alert.AlertType.ERROR, "Report Generation Failed",
                    "An error occurred while writing the report file:\n\n" +
                            e.getMessage());
            return false;
        }
    }

    /**
     * Displays a JavaFX Alert dialog to the user.
     * 
     * <p>
     * <strong>UI Design Choice:</strong> Uses Alert dialogs instead of console
     * output
     * to maintain consistency with desktop application standards and ensure users
     * see
     * important messages even when running without a terminal.
     * </p>
     * 
     * @param type    The type of alert (INFORMATION, WARNING, ERROR)
     * @param title   The alert dialog title
     * @param message The alert message content
     */
    private static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private ReportGenerator() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}
