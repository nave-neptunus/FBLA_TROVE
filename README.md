# FBLA Business Boost

A professional JavaFX desktop application for discovering local businesses, submitting reviews, and managing favorites.

## 🎯 Project Overview

This application was developed for the FBLA "Byte-Sized Business Boost" competition, demonstrating:
- **MVC Architecture** with clear separation of concerns
- **Professional UI/UX** with modern design principles
- **Data Analysis** capabilities with report generation
- **Bot Verification** to prevent automated spam
- **Accessibility** features for all users

## ✨ Features

### Core Functionality
- **Business Discovery**: Browse 10 local businesses across Food, Retail, and Services categories
- **Smart Filtering**: Filter businesses by category with real-time updates
- **Rating Sort**: Sort businesses by customer rating (highest to lowest)
- **Favorites Management**: Bookmark favorite businesses with automatic sidebar updates
- **Special Deals**: View current promotions and coupons for each business

### Review System
- **User Reviews**: Submit ratings (1-5 stars) with written feedback
- **Bot Verification**: Simple arithmetic challenge prevents automated submissions
- **Rating Updates**: Weighted average algorithm maintains rating stability

### Data Analysis
- **Report Generation**: Create formatted reports of favorited businesses
- **Export to Text**: Saves report to `SupportReport.txt` with business details and ratings

## 🛠️ Technical Stack

- **Language**: Java 21
- **Framework**: JavaFX 21.0.1
- **Build Tool**: Maven 3.x
- **Architecture**: Model-View-Controller (MVC)

## 📋 Requirements

- **Java Development Kit (JDK)**: Version 21 or higher
- **Maven**: Version 3.6 or higher (for building)

## 🚀 Running the Application

### Option 1: Using Maven (Recommended)

```bash
# Navigate to project directory
cd fbla-business-boost

# Compile the project
mvn clean compile

# Run the application
mvn javafx:run
```

### Option 2: Building a JAR

```bash
# Package the application
mvn clean package

# Run the JAR (requires JavaFX runtime)
java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -jar target/business-boost-1.0-SNAPSHOT.jar
```

## 📁 Project Structure

```
fbla-business-boost/
├── pom.xml                          # Maven configuration
├── src/
│   └── main/
│       ├── java/com/fbla/businessboost/
│       │   ├── Main.java            # Application entry point
│       │   ├── model/
│       │   │   └── Business.java    # Business entity with JavaFX properties
│       │   ├── controller/
│       │   │   ├── MainController.java         # Dashboard controller
│       │   │   └── ReviewDialogController.java # Review dialog controller
│       │   └── util/
│       │       ├── DataManager.java       # Mock data initialization
│       │       └── ReportGenerator.java   # Report generation utility
│       └── resources/
│           ├── styles.css           # Application styling
│           └── view/
│               ├── MainView.fxml    # Main dashboard layout
│               └── ReviewDialog.fxml # Review submission dialog
└── README.md
```

## 🎨 Design Decisions

### Data Structures

**JavaFX Properties**: Used throughout the Business model to enable automatic UI updates when data changes. This eliminates the need for manual refresh logic.

**ObservableList**: Chosen for business collections to provide automatic TableView synchronization. When businesses are added, removed, or modified, the UI updates automatically.

**FilteredList & SortedList**: Layered wrappers that enable independent filtering and sorting operations without modifying the original data.

### Bot Verification

The review system uses a simple arithmetic challenge (addition of two numbers 1-20) to prevent automated bot submissions while remaining accessible to all users, including those with cognitive disabilities.

### Rating Algorithm

New ratings are incorporated using a weighted average:
```
newRating = (oldRating × 5 + userRating × 1) / 6
```

This prevents single reviews from drastically changing overall ratings while still reflecting new feedback.

## 📊 FBLA Rubric Alignment

### User Experience (15 points)
- Professional JavaFX interface with modern gradient design
- Intuitive navigation with clear visual hierarchy
- Responsive layout with proper spacing and alignment

### Functionality (15 points)
- Category filtering with ComboBox
- Rating-based sorting
- Favorites management with automatic updates
- Review submission with validation
- Report generation with file I/O

### Logic & Algorithms (15 points)
- Bot verification with random arithmetic generation
- Weighted rating calculation algorithm
- Filtered and sorted list implementations
- Comprehensive error handling

### Data Analysis (15 points)
- Report generation iterates through favorites
- Formatted output with business details and ratings
- File writing with proper error handling
- Timestamp and summary statistics

### Code Quality (15 points)
- Comprehensive Javadoc comments on all classes and methods
- Explanation of data type choices and variable scope
- Modular architecture following MVC pattern
- Proper exception handling throughout

### Accessibility (10 points)
- Simple bot verification accessible to all users
- Clear labels and instructions
- Keyboard navigation support
- High contrast color scheme

## 📝 Usage Guide

### Browsing Businesses
1. Launch the application
2. View all businesses in the main table
3. Use the category filter to narrow results
4. Click "Sort by Rating" to order by highest rated

### Managing Favorites
1. Check the "Favorite" checkbox for any business
2. The business automatically appears in the left sidebar
3. Uncheck to remove from favorites

### Viewing Deals
1. Click on any business row in the table
2. Current deals appear in the right pane
3. Multiple deals are listed for each business

### Submitting Reviews
1. Select a business from the table
2. Click "Add Review" button
3. Set your rating using the slider (1-5 stars)
4. Write your review in the text area
5. Solve the arithmetic problem for verification
6. Click "Submit Review"

### Generating Reports
1. Mark several businesses as favorites
2. Click "Generate Report" button
3. Find the report in `SupportReport.txt` in the project directory

## 🏆 Key Achievements

✅ **Full MVC Architecture**: Clean separation between model, view, and controller layers  
✅ **Comprehensive Documentation**: Every class and method includes detailed Javadoc  
✅ **Professional UI**: Modern design with gradient header and polished styling  
✅ **Advanced Features**: Bot verification, weighted ratings, automatic UI updates  
✅ **Data Analysis**: Report generation with formatted output  
✅ **Standalone Execution**: Fully self-contained desktop application  

## 👥 Author

FBLA Development Team - 2026

## 📄 License

This project was created for educational purposes as part of the FBLA competition.
