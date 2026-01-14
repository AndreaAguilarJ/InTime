#!/bin/bash
# Script to build, install and launch the debug app

# Paths
ADB="/Users/hermes/Library/Android/sdk/platform-tools/adb"
PROJECT_DIR="/Users/hermes/Library/CloudStorage/GoogleDrive-al345735@edu.uaa.mx/My Drive/InTime"

echo "Building and installing app..."
cd "$PROJECT_DIR"
./gradlew installDebug

if [ $? -eq 0 ]; then
    echo "Launching app..."
    $ADB shell am start -n com.momentummm.app.debug/com.momentummm.app.MainActivity
    echo "App launched successfully!"
else
    echo "Build failed. Fix errors and try again."
    exit 1
fi
