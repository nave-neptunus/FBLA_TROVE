#!/bin/bash

# FBLA Business Boost - Run with Software Rendering
# This forces JavaFX to use software rendering instead of hardware acceleration

echo "=========================================="
echo "Running with Software Rendering..."
echo "=========================================="
echo ""

# Run with Prism software renderer
java -Dprism.order=sw \
     -Dprism.verbose=true \
     --module-path lib/javafx-sdk-21.0.1/lib \
     --add-modules javafx.controls,javafx.fxml \
     -cp out/production/classes:lib/json-20240303.jar \
     com.fbla.businessboost.Main
