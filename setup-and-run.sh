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

# Download org.json if it doesn't exist
if [ ! -f "lib/json-20240303.jar" ]; then
    echo "📦 Downloading org.json..."
    curl -s -L -o lib/json-20240303.jar https://repo1.maven.org/maven2/org/json/json/20240303/json-20240303.jar
fi

# Download OpenPDF if it doesn't exist
if [ ! -f "lib/openpdf-1.3.30.jar" ]; then
    echo "📦 Downloading OpenPDF..."
    curl -s -L -o lib/openpdf-1.3.30.jar https://repo1.maven.org/maven2/com/github/librepdf/openpdf/1.3.30/openpdf-1.3.30.jar
fi

# Check if JavaFX is already downloaded
if [ ! -d "lib/javafx-sdk-21.0.1" ]; then
    echo "📦 Downloading JavaFX SDK 21.0.1..."
    
    # Detect OS and Architecture
    OS="$(uname -s)"
    ARCH="$(uname -m)"
    
    case "${OS}" in
        Darwin*)
            if [ "$ARCH" = "arm64" ]; then
                JAVAFX_OS="osx-aarch64"
            else
                JAVAFX_OS="osx-x64"
            fi
            ;;
        Linux*)
            if [ "$ARCH" = "aarch64" ]; then
                JAVAFX_OS="linux-aarch64"
            else
                JAVAFX_OS="linux-x64"
            fi
            ;;
        *)          JAVAFX_OS="unknown";;
    esac
    
    if [ "$JAVAFX_OS" = "unknown" ]; then
        echo "❌ Unsupported operating system or architecture: ${OS} ${ARCH}"
        exit 1
    fi
    
    JAVAFX_URL="https://download2.gluonhq.com/openjfx/21.0.1/openjfx-21.0.1_${JAVAFX_OS}_bin-sdk.zip"
    
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
      --add-modules javafx.controls,javafx.fxml,javafx.web \
      -cp lib/json-20240303.jar:lib/openpdf-1.3.30.jar \
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

# Load environment variables if .env exists
if [ -f ".env" ]; then
    echo "🔒 Loading environment variables from .env"
    export $(grep -v '^#' .env | xargs)
fi

# Run the application
java --module-path lib/javafx-sdk-21.0.1/lib \
     --add-modules javafx.controls,javafx.fxml,javafx.web \
     -cp out/production/classes:lib/json-20240303.jar:lib/openpdf-1.3.30.jar \
     com.fbla.businessboost.Main
