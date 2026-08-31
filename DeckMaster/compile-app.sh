#!/usr/bin/env bash
set -e

### Prerequisites

# Set ANDROID_HOME explicitly so it's available immediately
export ANDROID_HOME=~/android-sdk

### Step 1: Install packages
pkg update -y && pkg upgrade -y
pkg install openjdk-17 git wget unzip gradle -y

### Verify Java:
a=$(java --show-version | head -1 | awk '{ print $2 }')
[ "${a:0:2}" -ge 17 ] || {
    echo "Please upgrade java to version 17+..."
    exit 1
}

### Step 2: Install the Android SDK
wget -O ~/install-android-sdk.sh https://raw.githubusercontent.com/Sohil876/termux-sdk-installer/main/installer.sh
chmod +x ~/install-android-sdk.sh
bash ~/install-android-sdk.sh -i

export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/build-tools/36.0.0

yes | sdkmanager --licenses
yes | sdkmanager "build-tools;36.0.0"

echo "Installing Android SDK Platform 36..."
if ! yes | sdkmanager "platforms;android-36" 2>/dev/null; then
    echo "sdkmanager failed — falling back to manual download..."
    mkdir -p "$ANDROID_HOME/platforms"
    wget -O /tmp/android-36.zip "https://dl.google.com/android/repository/platform-36_r02.zip" || \
    wget -O /tmp/android-36.zip "https://dl.google.com/android/repository/platform-36_r01.zip"
    unzip -o /tmp/android-36.zip -d "$ANDROID_HOME/platforms/"
    rm /tmp/android-36.zip
    if [ -d "$ANDROID_HOME/platforms/android-36" ]; then
        echo "Platform 36 installed manually ✓"
    else
        EXTRACTED=$(find "$ANDROID_HOME/platforms/" -maxdepth 1 -type d -name "*36*" | head -1)
        if [ -n "$EXTRACTED" ] && [ "$EXTRACTED" != "$ANDROID_HOME/platforms/android-36" ]; then
            mv "$EXTRACTED" "$ANDROID_HOME/platforms/android-36"
            echo "Platform 36 installed manually (renamed) ✓"
        else
            echo "ERROR: Could not install Android SDK Platform 36"
            exit 1
        fi
    fi
fi

# Verify aapt2 exists and is ARM-compatible (critical for Termux)
AAPT2_PATH=$ANDROID_HOME/build-tools/36.0.0/aapt2
if [ ! -f "$AAPT2_PATH" ]; then
    echo "⚠ aapt2 not found at $AAPT2_PATH — searching..."
    FOUND_AAPT2=$(find "$ANDROID_HOME" -name "aapt2" -type f 2>/dev/null | head -1)
    if [ -n "$FOUND_AAPT2" ]; then
        ln -sf "$FOUND_AAPT2" "$AAPT2_PATH"
        echo "Symlinked aapt2 from $FOUND_AAPT2 ✓"
    else
        echo "ERROR: No ARM-compatible aapt2 found."
        exit 1
    fi
fi
echo "Using aapt2: $AAPT2_PATH"

### Step 3: Ensure Gradle 9.5+
a=($(gradle -v | sed -n /Gradle/p | awk '{ print $2 }' | awk -F. '{ print $1, $2, $3 }'))
[ "$a" -gt 9 ] ||
[[ "$a" -eq 9 && "${a[1]}" -ge 5 ]] || {
    echo "Installing Gradle 9.5..."
    wget -O ~/gradle-9.5-bin.zip https://services.gradle.org/distributions/gradle-9.5-bin.zip
    mkdir -p ~/gradle
    unzip -d ~/gradle ~/gradle-9.5-bin.zip
    mv ~/gradle/gradle-9.5 ~/gradle/gradle
    rm ~/gradle-9.5-bin.zip
}

grep -q 'gradle/gradle/bin' ~/.bashrc || cat >> ~/.bashrc << 'EOF'
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/build-tools/36.0.0:$ANDROID_HOME/cmdline-tools/latest/bin:~/gradle/gradle/bin
EOF

export PATH=$PATH:~/gradle/gradle/bin

### Step 4: Build
echo "sdk.dir=$ANDROID_HOME" > local.properties

echo "Starting build..."
chmod +x gradlew
./gradlew assembleRelease \
    -Dandroid.aapt2FromMavenOverride="$AAPT2_PATH"

# APK at: app/build/outputs/apk/release/app-release.apk
