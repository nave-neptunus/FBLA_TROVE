#!/bin/bash

# FBLA Business Boost - Setup and Run Script
# This script downloads JavaFX dependencies and runs the application

echo "=========================================="
echo "FBLA Business Boost - Setup Script"
echo "=========================================="
echo ""

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Error: Java is not installed."
    echo "Please install Java 21 or higher and try again."
    exit 1
fi

echo "✅ Java found: $(java --version | head -n 1)"
echo ""

# Create lib directory if it doesn't exist
mkdir -p lib

# Check if JavaFX is already downloaded
if [ ! -d "lib/javafx-sdk-21.0.1" ]; then
    echo "📦 Downloading JavaFX SDK 21.0.1..."
    
    # Detect OS
    OS="$(uname -s)"
    case "${OS}" in
        Darwin*)    JAVAFX_OS="osx";;
        Linux*)     JAVAFX_OS="linux";;
        *)          JAVAFX_OS="unknown";;
    esac
    
    if [ "$JAVAFX_OS" = "unknown" ]; then
        echo "❌ Unsupported operating system: ${OS}"
        exit 1
    fi
    
    JAVAFX_URL="https://download2.gluonhq.com/openjfx/21.0.1/openjfx-21.0.1_${JAVAFX_OS}-x64_bin-sdk.zip"
    
    echo "Downloading from: $JAVAFX_URL"
    curl -L -o lib/javafx-sdk.zip "$JAVAFX_URL"
    
    echo "📂 Extracting JavaFX SDK..."
    unzip -q lib/javafx-sdk.zip -d lib/
    rm lib/javafx-sdk.zip
    
    echo "✅ JavaFX SDK downloaded and extracted"
else
    echo "✅ JavaFX SDK already present"
fi

echo ""
echo "🔨 Compiling Java source files..."

# Create output directory
mkdir -p out/production/classes

# Compile all Java files
javac -d out/production/classes \
      --module-path lib/javafx-sdk-21.0.1/lib \
      --add-modules javafx.controls,javafx.fxml \
      $(find src/main/java -name "*.java")

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful"
else
    echo "❌ Compilation failed"
    exit 1
fi

# Copy resources
echo "📋 Copying resources..."
cp -r src/main/resources/* out/production/classes/

echo ""
echo "=========================================="
echo "🚀 Launching FBLA Business Boost..."
echo "=========================================="
echo ""

# Run the application
java --module-path lib/javafx-sdk-21.0.1/lib \
     --add-modules javafx.controls,javafx.fxml \
     -cp out/production/classes \
     com.fbla.businessboost.Main
