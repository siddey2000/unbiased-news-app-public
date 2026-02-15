#!/bin/bash

# Gradle wrapper script - downloads and uses Gradle directly
# This is a fallback wrapper when the standard gradle-wrapper.jar is unavailable

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GRADLE_VERSION="8.5"
GRADLE_HOME="${GRADLE_HOME:-$HOME/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}-bin}"
GRADLE_CACHE="/tmp/gradle-${GRADLE_VERSION}"

# Find or download Gradle
find_gradle() {
    # Check common locations
    for dir in \
        "$GRADLE_CACHE" \
        "$HOME/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}-bin" \
        "/tmp/gradle-${GRADLE_VERSION}"
    do
        if [ -x "$dir/bin/gradle" ]; then
            echo "$dir/bin/gradle"
            return 0
        fi
        # Check for hash subdirectory structure
        for subdir in "$dir"/*/; do
            if [ -x "$subdir/gradle-${GRADLE_VERSION}/bin/gradle" ]; then
                echo "$subdir/gradle-${GRADLE_VERSION}/bin/gradle"
                return 0
            fi
        done
    done
    return 1
}

download_gradle() {
    echo "Downloading Gradle ${GRADLE_VERSION}..."
    local zip_file="/tmp/gradle-${GRADLE_VERSION}-bin.zip"

    if [ ! -f "$zip_file" ]; then
        curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "$zip_file"
    fi

    echo "Extracting Gradle..."
    unzip -q -o "$zip_file" -d "/tmp"

    echo "$GRADLE_CACHE/bin/gradle"
}

# Get gradle executable
GRADLE_EXE=$(find_gradle) || GRADLE_EXE=$(download_gradle)

# Verify gradle is executable
if [ ! -x "$GRADLE_EXE" ]; then
    chmod +x "$GRADLE_EXE"
fi

# Execute Gradle from project directory
cd "$SCRIPT_DIR"
exec "$GRADLE_EXE" "$@"
