#!/bin/bash
set -e

echo "=== Gradle Java Compatibility Fix Script ==="
echo ""

# Step 1: Stop all Gradle daemons
echo "Step 1: Stopping Gradle daemons..."
cd /Users/hermes/Downloads/InTime
./gradlew --stop 2>/dev/null || echo "No daemons running"
echo "✓ Gradle daemons stopped"
echo ""

# Step 2: Clean corrupted Gradle cache
echo "Step 2: Cleaning corrupted Gradle 8.5 cache..."
if [ -d "$HOME/.gradle/caches/8.5" ]; then
    rm -rf "$HOME/.gradle/caches/8.5"
    echo "✓ Removed corrupted cache at ~/.gradle/caches/8.5"
else
    echo "✓ Cache directory not found (already clean)"
fi
echo ""

# Step 3: Check available Java versions
echo "Step 3: Checking available Java versions..."
echo "Available Java installations:"
/usr/libexec/java_home -V 2>&1 | grep -E "^\s+[0-9]" || true
echo ""

# Step 4: Find compatible Java version (17, 19, 20, or 21)
echo "Step 4: Looking for compatible Java version (17-21)..."
JAVA_17=$(/usr/libexec/java_home -v 17 2>/dev/null || echo "")
JAVA_21=$(/usr/libexec/java_home -v 21 2>/dev/null || echo "")
JAVA_20=$(/usr/libexec/java_home -v 20 2>/dev/null || echo "")
JAVA_19=$(/usr/libexec/java_home -v 19 2>/dev/null || echo "")

COMPATIBLE_JAVA=""
if [ -n "$JAVA_21" ]; then
    COMPATIBLE_JAVA="$JAVA_21"
    echo "✓ Found Java 21: $JAVA_21"
elif [ -n "$JAVA_20" ]; then
    COMPATIBLE_JAVA="$JAVA_20"
    echo "✓ Found Java 20: $JAVA_20"
elif [ -n "$JAVA_19" ]; then
    COMPATIBLE_JAVA="$JAVA_19"
    echo "✓ Found Java 19: $JAVA_19"
elif [ -n "$JAVA_17" ]; then
    COMPATIBLE_JAVA="$JAVA_17"
    echo "✓ Found Java 17: $JAVA_17"
fi

if [ -z "$COMPATIBLE_JAVA" ]; then
    echo "✗ No compatible Java version (17-21) found!"
    echo ""
    echo "SOLUTION: Install Java 17 or 21:"
    echo "  Option 1: brew install openjdk@17"
    echo "  Option 2: brew install openjdk@21"
    echo "  Option 3: Download from https://adoptium.net"
    echo ""
    echo "After installing, run this script again."
    exit 1
fi
echo ""

# Step 5: Create gradle.properties with Java home
echo "Step 5: Configuring project to use compatible Java..."
GRADLE_PROPS="/Users/hermes/Downloads/InTime/gradle.properties"
if grep -q "org.gradle.java.home" "$GRADLE_PROPS" 2>/dev/null; then
    # Update existing entry
    sed -i.bak "s|^org.gradle.java.home=.*|org.gradle.java.home=$COMPATIBLE_JAVA|" "$GRADLE_PROPS"
    rm -f "$GRADLE_PROPS.bak"
    echo "✓ Updated org.gradle.java.home in gradle.properties"
else
    # Add new entry
    echo "" >> "$GRADLE_PROPS"
    echo "# Gradle Java home (compatible version for Gradle 8.5)" >> "$GRADLE_PROPS"
    echo "org.gradle.java.home=$COMPATIBLE_JAVA" >> "$GRADLE_PROPS"
    echo "✓ Added org.gradle.java.home to gradle.properties"
fi
echo ""

# Step 6: Verify the fix
echo "Step 6: Testing Gradle with compatible Java..."
export JAVA_HOME="$COMPATIBLE_JAVA"
./gradlew --version
echo ""

echo "=== Fix Complete! ==="
echo ""
echo "Next steps:"
echo "1. In your IDE, go to Settings → Build, Execution, Deployment → Build Tools → Gradle"
echo "2. Set 'Gradle JVM' to: $COMPATIBLE_JAVA"
echo "3. Click 'Apply' and 'OK'"
echo "4. Sync your project (File → Sync Project with Gradle Files)"
echo ""
echo "Gradle is now configured to use Java at: $COMPATIBLE_JAVA"


