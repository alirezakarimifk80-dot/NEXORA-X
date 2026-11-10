#!/bin/sh
# Gradle Start Up script for UN*X — NEXORA X
# Supports:
#   1. Committed gradle/wrapper/gradle-wrapper.jar
#   2. System 'gradle' binary (e.g. gradle/actions/setup-gradle@v4 on GitHub Actions)
#   3. Automatic download of Gradle 8.9 distribution when java + curl + unzip are available

APP_HOME=$( cd "${0%/*}" && pwd -P ) || exit
GRADLE_VERSION="8.9"

if [ -f "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" ] && command -v java >/dev/null 2>&1; then
    exec java -Xmx64m -Xms64m -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
elif command -v gradle >/dev/null 2>&1 && command -v java >/dev/null 2>&1; then
    exec gradle "$@"
elif command -v java >/dev/null 2>&1 && command -v curl >/dev/null 2>&1 && command -v unzip >/dev/null 2>&1; then
    GRADLE_USER_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}"
    DIST_DIR="$GRADLE_USER_DIR/wrapper/dists/gradle-${GRADLE_VERSION}-bin-auto"
    GRADLE_BIN="$DIST_DIR/gradle-${GRADLE_VERSION}/bin/gradle"
    if [ ! -x "$GRADLE_BIN" ]; then
        echo "Downloading Gradle ${GRADLE_VERSION} distribution from services.gradle.org..."
        mkdir -p "$DIST_DIR"
        ZIP_FILE="$DIST_DIR/gradle-${GRADLE_VERSION}-bin.zip"
        curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "$ZIP_FILE"
        unzip -q -o "$ZIP_FILE" -d "$DIST_DIR"
        rm -f "$ZIP_FILE"
        chmod +x "$GRADLE_BIN"
    fi
    exec "$GRADLE_BIN" "$@"
else
    echo "ERROR: Android SDK / JDK 17 / Gradle runtime is not installed in this container." >&2
    echo "To build locally on your workstation or via GitHub Actions:" >&2
    echo "  1. Push to 'main' branch on GitHub (.github/workflows/android-apk.yml runs automatically)" >&2
    echo "  2. Or open in Android Studio Ladybug (2024.2.1+) with JDK 17 & Android SDK 35" >&2
    exit 127
fi
