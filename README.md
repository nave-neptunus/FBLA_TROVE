# 🚀 Trove

A professional JavaFX desktop application for discovering local businesses, submitting reviews, managing favorites, and generating impact reports.

## 🎯 Project Overview

Built for the FBLA "Byte-Sized Business Boost" competition, Trove demonstrates:
- **MVC Architecture** with clear separation of concerns
- **Modern Tabbed Interface** (Discover, My Impact, Smart Search)
- **Intelligent Recommendation Engine** ("The Local Matchmaker")
- **Data Analysis** with live charts and HTML report export
- **Visual Bot Verification** (icon-grid challenge)
- **Semantic Validation** for review sentiment
- **Accessibility** with Dark Mode / High Contrast theme toggle

## ✨ Features

### 🏪 Discover Tab
- **Business Discovery**: Browse local businesses across Food, Retail, and Services
- **DoorDash-Style Cards**: Image, name, category badge, price, rating stars, and address
- **Quick Favorite**: Heart icon overlay on each card for instant favoriting
- **Live Search**: Search by zip code with real-time API results (Overpass API)
- **Category Filter & Rating Sort**: Narrow results by category or highest rating
- **Recommendations**: "You might also like..." section powered by tag matching

### 📊 My Impact Tab
- **Live Dashboard**: Stats cards showing total favorites, average rating, and top category
- **Bar Chart**: Visualizes how many favorites you have in each category
- **Pie Chart**: Shows the rating distribution of your favorites
- **HTML Report Export**: Generates a styled HTML report that opens in your browser

### 🔍 Smart Search Tab
- **Fuzzy Search**: Find businesses by name, category, description, or tags
- **Tag Cloud**: Clickable tags like `#budget`, `#date-night`, `#outdoor` for instant filtering
- **Visual Results**: Search results displayed as the same beautiful business cards

### Review System
- **Visual Bot Verification**: 3×3 emoji icon grid — identify food icons to prove you're human
- **Sentiment Validation**: Real-time warning if your rating contradicts your review text
- **Star Rating**: Slider with instant visual feedback (★★★★☆)
- **Weighted Average**: New ratings are blended using `(old × 5 + new) / 6` for stability

### 🌙 Accessibility
- **Dark Mode Toggle**: One-click switch between light and dark themes
- **High Contrast**: Dark theme uses deep navy (#1a1a2e) with bright accents
- **CSS Swapping**: Instant theme change via `Scene.getStylesheets()`

---

## 🧠 Logic & Algorithms

### Collaborative Filtering Lite (Recommendation Engine)
The "Local Matchmaker" uses a simplified version of collaborative filtering. Instead of tracking multiple users' preferences, it builds a tag frequency profile from the current user's favorites, then scores every non-favorited business by counting tag overlaps. The top 3 scoring businesses are returned as recommendations.

```
Algorithm:
1. Collect all tags from favorited businesses
2. Build frequency map: { #budget: 3, #cozy: 2, ... }
3. Score each non-favorited business by sum of matching tag frequencies
4. Return top 3 by score
```

### Asynchronous File I/O
Report generation uses `BufferedWriter` for efficient file writing. After generating the HTML report, the app uses `Desktop.browse()` to open the report in the user's default browser, all from a background-safe context.

### Observable Pattern
JavaFX properties (`SimpleStringProperty`, `SimpleBooleanProperty`, etc.) and `ObservableList` enable automatic UI updates throughout the app:
- Toggling a favorite instantly updates the sidebar, recommendation section, and impact charts
- Rating slider changes instantly update the star display label
- Review text changes trigger real-time sentiment validation

### Semantic Validation (Sentiment Analysis)
The review dialog performs lightweight sentiment detection by checking review text against positive ("great", "amazing", "love") and negative ("terrible", "awful", "hate") word lists. If a low rating (≤2) contains positive words, or a high rating (≥4) contains negative words, a non-blocking warning banner appears.

### Weighted Rating Formula
```
updatedRating = (currentRating × 5 + newRating × 1) / 6.0
```
This prevents a single review from causing dramatic rating swings while still reflecting new feedback.

---

## �️ Technical Stack

- **Language**: Java 21
- **Framework**: JavaFX 21.0.1 (Controls, FXML, Charts)
- **Build Tool**: Maven 3.x
- **Architecture**: Model-View-Controller (MVC)
- **Data**: Supabase (auth, reviews, favorites) + Overpass API (business discovery)
- **Reports**: HTML generation with `StringBuilder`

## 📁 Project Structure

```
trove/
├── pom.xml
├── src/
│   └── main/
│       ├── java/com/fbla/businessboost/
│       │   ├── Main.java
│       │   ├── model/
│       │   │   ├── Business.java            # Business entity with tags + properties
│       │   │   └── Review.java              # Review POJO
│       │   ├── controller/
│       │   │   ├── LoginController.java     # Auth + math bot verification
│       │   │   ├── MainController.java      # Tabbed dashboard controller
│       │   │   └── ReviewDialogController.java  # Icon-grid verification + sentiment
│       │   └── util/
│       │       ├── BusinessFetcher.java     # Overpass API + auto-tagging
│       │       ├── DataManager.java         # Mock data with tags
│       │       ├── RecommendationEngine.java # Tag-based recommendation engine
│       │       ├── ReportGenerator.java     # HTML report generation
│       │       ├── SessionManager.java      # Current user session
│       │       └── SupabaseClient.java      # Supabase REST client
│       └── resources/
│           ├── styles.css                   # Light theme
│           ├── styles-dark.css              # Dark theme
│           └── view/
│               ├── LoginView.fxml           # Login/register screen
│               ├── MainView.fxml            # 3-tab dashboard
│               └── ReviewDialog.fxml        # Review dialog with icon grid
└── README.md
```

## 🚀 Running the Application

```bash
# Clone the repository
git clone https://github.com/yourusername/fbla-buisness-boost2.git
cd fbla-buisness-boost2

# Compile
mvn clean compile

# Run
mvn javafx:run
```

## 📊 FBLA Rubric Alignment

| Category | Points | Evidence |
|---|---|---|
| **User Experience** | 15 | Modern tabbed interface, DoorDash-style cards, dark mode, micro-animations |
| **Functionality** | 15 | 3-tab navigation, zip search, favorites, reviews, recommendations |
| **Logic & Algorithms** | 15 | Collaborative filtering, sentiment analysis, weighted rating, fuzzy search |
| **Data Analysis** | 15 | Live BarChart + PieChart, HTML report with stat cards and category bars |
| **Code Quality** | 15 | Comprehensive Javadoc, MVC architecture, clean separation of concerns |
| **Accessibility** | 10 | Dark mode toggle, icon-grid verification, keyboard navigation, high contrast |

## 👥 Author

FBLA Development Team — 2026

## 📄 License

Created for educational purposes as part of the FBLA competition.
