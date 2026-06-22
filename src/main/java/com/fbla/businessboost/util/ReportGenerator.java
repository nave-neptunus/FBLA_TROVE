package com.fbla.businessboost.util;

import com.fbla.businessboost.model.Business;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;

import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates professional PDF-formatted reports for favorited businesses.
 *
 * <p>
 * <strong>Report structure (v4.0):</strong>
 * <ol>
 *   <li>Cover page — branded with app icon, title, username, and date</li>
 *   <li>Summary stats cards — favorites count, avg rating, categories explored</li>
 *   <li>Economic impact section — estimated local jobs supported</li>
 *   <li>Business table — ranked list of all favorited businesses</li>
 *   <li>Category breakdown — aggregated category distribution</li>
 *   <li>Footer — brand tagline</li>
 * </ol>
 * </p>
 *
 * @author FBLA Development Team
 * @version 4.0
 */
public class ReportGenerator {

    /** Default filename for the generated report */
    private static final String REPORT_FILENAME = "SupportReport.pdf";

    /** Date/time format for report timestamp */
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Colors
    private static final Color PRIMARY_COLOR = new Color(91, 160, 140); // #5BA08C
    private static final Color TEXT_DARK = new Color(45, 38, 64); // #2D2640
    private static final Color BORDER_GRAY = new Color(235, 232, 240); // #EBE8F0
    private static final Color HEADER_BG = new Color(240, 250, 246); // #F0FAF6

    /**
     * Generates a professional PDF report for all favorited businesses.
     *
     * @param username  The username of the user generating the report
     * @param favorites ObservableList of Business objects marked as favorites
     * @return true if report was successfully generated, false otherwise
     */
    public static boolean generateReport(String username, ObservableList<Business> favorites) {
        // Validate input
        if (favorites == null || favorites.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Favorites",
                    "You haven't marked any businesses as favorites yet. " +
                            "Add some favorites before generating a report.");
            return false;
        }

        Document document = new Document(PageSize.A4, 36, 36, 54, 54);

        try {
            PdfWriter.getInstance(document, new FileOutputStream(REPORT_FILENAME));
            document.open();

            // Setup Fonts
            Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, PRIMARY_COLOR);
            Font subtitleFont = new Font(Font.HELVETICA, 12, Font.NORMAL, Color.GRAY);
            Font sectionFont = new Font(Font.HELVETICA, 16, Font.BOLD, TEXT_DARK);
            Font tableHeaderFont = new Font(Font.HELVETICA, 10, Font.BOLD, PRIMARY_COLOR);
            Font tableBodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXT_DARK);
            Font smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);

            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

            // ================= Cover Page =================
            Paragraph appName = new Paragraph("Trove", titleFont);
            appName.setAlignment(Paragraph.ALIGN_CENTER);
            appName.setSpacingAfter(4);
            document.add(appName);

            Paragraph tagline = new Paragraph("Local Business Discovery — Impact Report",
                    new Font(Font.HELVETICA, 14, Font.ITALIC, Color.GRAY));
            tagline.setAlignment(Paragraph.ALIGN_CENTER);
            tagline.setSpacingAfter(20);
            document.add(tagline);

            PdfPTable coverRule = new PdfPTable(1);
            coverRule.setWidthPercentage(100);
            PdfPCell ruleCell = new PdfPCell(new Phrase(""));
            ruleCell.setBackgroundColor(PRIMARY_COLOR);
            ruleCell.setFixedHeight(3);
            ruleCell.setBorder(PdfPCell.NO_BORDER);
            coverRule.addCell(ruleCell);
            document.add(coverRule);

            Paragraph coverMeta = new Paragraph(
                    "\nGenerated for: " + (username != null ? username : "Guest") +
                    "\nDate: " + timestamp +
                    "\nPowered by Trove — FBLA Business Boost",
                    subtitleFont);
            coverMeta.setAlignment(Paragraph.ALIGN_CENTER);
            coverMeta.setSpacingAfter(30);
            document.add(coverMeta);

            Map<String, Long> catCount = favorites.stream()
                    .collect(Collectors.groupingBy(Business::getCategory, Collectors.counting()));
            double avgRating = favorites.stream().mapToDouble(Business::getRating).average().orElse(0);

            PdfPTable statsTable = new PdfPTable(3);
            statsTable.setWidthPercentage(100);
            statsTable.setSpacingAfter(20);

            statsTable.addCell(createStatCell("Businesses Supported", String.valueOf(favorites.size()), tableBodyFont,
                    sectionFont));
            statsTable.addCell(
                    createStatCell("Average Rating", String.format("%.1f ★", avgRating), tableBodyFont, sectionFont));
            statsTable.addCell(
                    createStatCell("Categories Explored", String.valueOf(catCount.size()), tableBodyFont, sectionFont));

            document.add(statsTable);

            // ================= Economic Impact Section =================
            int estimatedJobs = (int) Math.round(favorites.size() * 4.7);

            PdfPTable impactTable = new PdfPTable(1);
            impactTable.setWidthPercentage(100);
            impactTable.setSpacingBefore(5);
            impactTable.setSpacingAfter(20);

            PdfPCell impactCell = new PdfPCell();
            impactCell.setBackgroundColor(HEADER_BG);
            impactCell.setBorderColor(PRIMARY_COLOR);
            impactCell.setBorderWidth(2);
            impactCell.setPadding(16);

            Font impactNumFont  = new Font(Font.HELVETICA, 24, Font.BOLD, PRIMARY_COLOR);
            Font impactTextFont = new Font(Font.HELVETICA, 11, Font.NORMAL, TEXT_DARK);
            Font impactSmallFont = new Font(Font.HELVETICA, 9, Font.ITALIC, Color.GRAY);

            Paragraph impactHeading = new Paragraph("🏪 Economic Impact",
                    new Font(Font.HELVETICA, 14, Font.BOLD, TEXT_DARK));
            impactHeading.setSpacingAfter(6);

            Paragraph impactLine = new Paragraph(
                    "Your " + favorites.size() + " favorited businesses support an estimated  ", impactTextFont);
            impactLine.add(new Phrase(estimatedJobs + " local jobs", impactNumFont));
            impactLine.setSpacingAfter(4);

            Paragraph impactSource = new Paragraph(
                    "Estimate based on U.S. Small Business Administration average of 4.7 employees per small business.",
                    impactSmallFont);

            impactCell.addElement(impactHeading);
            impactCell.addElement(impactLine);
            impactCell.addElement(impactSource);
            impactTable.addCell(impactCell);
            document.add(impactTable);

            // ================= Business Table =================
            Paragraph tableTitle = new Paragraph("Favorited Businesses", sectionFont);
            tableTitle.setSpacingAfter(10);
            document.add(tableTitle);

            PdfPTable table = new PdfPTable(new float[] { 1, 4, 3, 2, 2 });
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setSpacingAfter(20);

            // Header Row
            String[] headers = { "#", "Business Name & Address", "Category", "Rating", "Price" };
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, tableHeaderFont));
                cell.setBackgroundColor(HEADER_BG);
                cell.setBorderColor(BORDER_GRAY);
                cell.setPadding(8);
                cell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
                table.addCell(cell);
            }

            // Body Rows
            for (int i = 0; i < favorites.size(); i++) {
                Business b = favorites.get(i);

                table.addCell(createCell(String.valueOf(i + 1), tableBodyFont));

                String nameAndAddress = b.getName() + "\n" + b.getAddress();
                PdfPCell nameCell = createCell(nameAndAddress, tableBodyFont);
                table.addCell(nameCell);

                table.addCell(createCell(b.getCategory(), tableBodyFont));
                table.addCell(createCell(String.format("%.1f", b.getRating()) + " Stars", tableBodyFont));
                table.addCell(createCell(b.getPrice(), tableBodyFont));
            }

            document.add(table);

            // ================= Category Breakdown =================
            Paragraph categoryTitle = new Paragraph("Category Breakdown", sectionFont);
            categoryTitle.setSpacingAfter(10);
            document.add(categoryTitle);

            PdfPTable catTable = new PdfPTable(2);
            catTable.setWidthPercentage(50);
            catTable.setHorizontalAlignment(PdfPTable.ALIGN_LEFT);

            for (Map.Entry<String, Long> entry : catCount.entrySet()) {
                PdfPCell catCell = new PdfPCell(new Phrase(entry.getKey(), tableBodyFont));
                catCell.setBorder(PdfPCell.NO_BORDER);
                catCell.setPaddingBottom(5);
                catTable.addCell(catCell);

                PdfPCell countCell = new PdfPCell(new Phrase(String.valueOf(entry.getValue()), tableBodyFont));
                countCell.setBorder(PdfPCell.NO_BORDER);
                countCell.setPaddingBottom(5);
                countCell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
                catTable.addCell(countCell);
            }
            document.add(catTable);

            // ================= Footer =================
            Paragraph footer = new Paragraph("\nSupporting local businesses, one favorite at a time.", smallFont);
            footer.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(footer);

            document.close();

            // Try to open the generated PDF
            try {
                File reportFile = new File(REPORT_FILENAME);
                if (Desktop.isDesktopSupported() && reportFile.exists()) {
                    Desktop.getDesktop().open(reportFile);
                }
            } catch (Exception e) {
                System.err.println("Could not open PDF: " + e.getMessage());
            }

            showAlert(Alert.AlertType.INFORMATION, "Report Generated",
                    String.format("PDF report created!\n\nFile: %s\nTotal businesses: %d\n\n" +
                            "The report has been opened in your PDF viewer.",
                            REPORT_FILENAME, favorites.size()));

            // Save report metadata to Supabase
            if (username != null) {
                String categoriesStr = String.join(", ", catCount.keySet());
                new Thread(() -> {
                    SupabaseClient.saveReportMetadata(username, favorites.size(), avgRating, categoriesStr);
                }).start();
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Report Generation Failed",
                    "An error occurred while generating the PDF report:\n\n" + e.getMessage());
            return false;
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private static PdfPCell createCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorderColor(BORDER_GRAY);
        cell.setPadding(8);
        return cell;
    }

    private static PdfPCell createStatCell(String title, String value, Font titleFont, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.BOX);
        cell.setBorderColor(BORDER_GRAY);
        cell.setPadding(15);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);

        Paragraph v = new Paragraph(value, valueFont);
        v.setAlignment(Paragraph.ALIGN_CENTER);
        Paragraph t = new Paragraph(title, titleFont);
        t.setAlignment(Paragraph.ALIGN_CENTER);

        cell.addElement(v);
        cell.addElement(t);
        return cell;
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
