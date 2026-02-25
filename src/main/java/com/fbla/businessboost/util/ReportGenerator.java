package com.fbla.businessboost.util;

import com.fbla.businessboost.model.Business;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates professional HTML-formatted reports for favorited businesses.
 *
 * <p>
 * <strong>Asynchronous File I/O:</strong> Report generation can be invoked from
 * the
 * UI thread; the file writing itself is handled efficiently by
 * {@code BufferedWriter}.
 * After generation, the report opens automatically in the user's default
 * browser.
 * </p>
 *
 * @author FBLA Development Team
 * @version 2.0
 */
public class ReportGenerator {

    /** Default filename for the generated report */
    private static final String REPORT_FILENAME = "SupportReport.html";

    /** Date/time format for report timestamp */
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Generates a professional HTML report for all favorited businesses.
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
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

            // Category stats
            Map<String, Long> catCount = favorites.stream()
                    .collect(Collectors.groupingBy(Business::getCategory, Collectors.counting()));
            double avgRating = favorites.stream().mapToDouble(Business::getRating).average().orElse(0);

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
            html.append("<meta charset=\"UTF-8\">\n");
            html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("<title>Commercio - Support Report</title>\n");
            html.append("<style>\n");
            html.append(getReportCSS());
            html.append("</style>\n</head>\n<body>\n");

            // Header
            html.append("<div class=\"header\">\n");
            html.append("  <h1>🚀 Commercio</h1>\n");
            html.append("  <p class=\"subtitle\">Support Report</p>\n");
            html.append("</div>\n");

            // Summary
            html.append("<div class=\"summary\">\n");
            html.append("  <div class=\"stat-card\">\n");
            html.append("    <div class=\"stat-number\">").append(favorites.size()).append("</div>\n");
            html.append("    <div class=\"stat-label\">Businesses Supported</div>\n");
            html.append("  </div>\n");
            html.append("  <div class=\"stat-card\">\n");
            html.append("    <div class=\"stat-number\">").append(String.format("%.1f", avgRating)).append("</div>\n");
            html.append("    <div class=\"stat-label\">Avg Rating</div>\n");
            html.append("  </div>\n");
            html.append("  <div class=\"stat-card\">\n");
            html.append("    <div class=\"stat-number\">").append(catCount.size()).append("</div>\n");
            html.append("    <div class=\"stat-label\">Categories</div>\n");
            html.append("  </div>\n");
            html.append("</div>\n");

            // Table
            html.append("<table>\n");
            html.append(
                    "<thead><tr><th>#</th><th>Business</th><th>Category</th><th>Rating</th><th>Price</th><th>Deals</th></tr></thead>\n");
            html.append("<tbody>\n");

            for (int i = 0; i < favorites.size(); i++) {
                Business b = favorites.get(i);
                html.append("<tr>\n");
                html.append("  <td>").append(i + 1).append("</td>\n");
                html.append("  <td><strong>").append(escapeHtml(b.getName())).append("</strong><br><small>")
                        .append(escapeHtml(b.getAddress())).append("</small></td>\n");
                html.append("  <td><span class=\"badge badge-").append(b.getCategory().toLowerCase()).append("\">")
                        .append(b.getCategory()).append("</span></td>\n");
                html.append("  <td>").append(String.format("%.1f ⭐", b.getRating())).append("</td>\n");
                html.append("  <td>").append(escapeHtml(b.getPrice())).append("</td>\n");
                html.append("  <td>");
                if (!b.getDeals().isEmpty()) {
                    for (String deal : b.getDeals()) {
                        html.append("• ").append(escapeHtml(deal)).append("<br>");
                    }
                } else {
                    html.append("<em>No current deals</em>");
                }
                html.append("</td>\n");
                html.append("</tr>\n");
            }

            html.append("</tbody>\n</table>\n");

            // Category breakdown
            html.append("<div class=\"category-breakdown\">\n");
            html.append("<h2>Category Breakdown</h2>\n");
            catCount.forEach((cat, count) -> {
                int pct = (int) ((count * 100.0) / favorites.size());
                html.append("<div class=\"bar-row\">\n");
                html.append("  <span class=\"bar-label\">").append(cat).append(" (").append(count).append(")</span>\n");
                html.append("  <div class=\"bar-track\"><div class=\"bar-fill\" style=\"width: ").append(pct)
                        .append("%\"></div></div>\n");
                html.append("</div>\n");
            });
            html.append("</div>\n");

            // Footer
            html.append("<div class=\"footer\">\n");
            html.append("  <p>Generated by Commercio on ").append(timestamp).append("</p>\n");
            html.append("  <p>Supporting local businesses, one favorite at a time.</p>\n");
            html.append("</div>\n");

            html.append("</body>\n</html>");

            writer.write(html.toString());

            // Try to open in default browser
            try {
                File reportFile = new File(REPORT_FILENAME);
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(reportFile.toURI());
                }
            } catch (Exception e) {
                // Browser opening is a bonus, not critical
                System.err.println("Could not open browser: " + e.getMessage());
            }

            showAlert(Alert.AlertType.INFORMATION, "Report Generated",
                    String.format("HTML report created!\n\nFile: %s\nTotal businesses: %d\n\n" +
                            "The report has been opened in your browser.",
                            REPORT_FILENAME, favorites.size()));

            return true;

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Report Generation Failed",
                    "An error occurred while writing the report:\n\n" + e.getMessage());
            return false;
        }
    }

    /**
     * Returns the CSS for the HTML report.
     */
    private static String getReportCSS() {
        return "* { margin: 0; padding: 0; box-sizing: border-box; }\n" +
                "body { font-family: 'Segoe UI', Arial, sans-serif; background: #F8F7FA; color: #3D3555; padding: 20px; }\n"
                +
                ".header { background: linear-gradient(135deg, #5BA08C, #4A9079); color: white; padding: 30px; border-radius: 12px; text-align: center; margin-bottom: 25px; }\n"
                +
                ".header h1 { font-size: 28px; margin-bottom: 5px; }\n" +
                ".subtitle { opacity: 0.85; font-size: 14px; }\n" +
                ".summary { display: flex; gap: 20px; margin-bottom: 25px; }\n" +
                ".stat-card { flex: 1; background: white; border-radius: 10px; padding: 20px; text-align: center; box-shadow: 0 2px 8px rgba(91,160,140,0.1); }\n"
                +
                ".stat-number { font-size: 32px; font-weight: bold; color: #5BA08C; }\n" +
                ".stat-label { font-size: 13px; color: #888; margin-top: 4px; }\n" +
                "table { width: 100%; border-collapse: collapse; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 8px rgba(91,160,140,0.1); margin-bottom: 25px; }\n"
                +
                "thead { background: #5BA08C; color: white; }\n" +
                "th { padding: 12px 15px; text-align: left; font-size: 13px; }\n" +
                "td { padding: 12px 15px; border-bottom: 1px solid #EBE8F0; font-size: 13px; vertical-align: top; }\n" +
                "tr:hover { background: #F0FAF6; }\n" +
                ".badge { padding: 3px 10px; border-radius: 12px; font-size: 11px; font-weight: bold; color: white; }\n"
                +
                ".badge-food { background: #5BA08C; }\n" +
                ".badge-retail { background: #8B7BB8; }\n" +
                ".badge-services { background: #6BAF9C; }\n" +
                ".category-breakdown { background: white; border-radius: 10px; padding: 25px; box-shadow: 0 2px 8px rgba(91,160,140,0.1); margin-bottom: 25px; }\n"
                +
                ".category-breakdown h2 { font-size: 18px; margin-bottom: 15px; }\n" +
                ".bar-row { display: flex; align-items: center; margin-bottom: 10px; }\n" +
                ".bar-label { width: 120px; font-size: 13px; font-weight: 600; }\n" +
                ".bar-track { flex: 1; height: 24px; background: #EBE8F0; border-radius: 12px; overflow: hidden; }\n" +
                ".bar-fill { height: 100%; background: linear-gradient(90deg, #5BA08C, #6BAF9C); border-radius: 12px; transition: width 0.5s; }\n"
                +
                ".footer { text-align: center; padding: 20px; color: #999; font-size: 12px; }\n" +
                "small { color: #999; }\n";
    }

    /**
     * Escapes HTML special characters.
     */
    private static String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private ReportGenerator() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}
