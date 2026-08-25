#!/usr/bin/env bash
set -e

### Prerequisites

# Set ANDROID_HOME explicitly so it's available immediately (not just after .bashrc reload)
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
# Download the Termux SDK installer
wget -O ~/install-android-sdk.sh https://raw.githubusercontent.com/Sohil876/termux-sdk-installer/main/installer.sh
chmod +x ~/install-android-sdk.sh
bash ~/install-android-sdk.sh -i

# Add cmdline-tools to PATH immediately (sdkmanager lives here)
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/build-tools/36.0.0

# Accept licenses
yes | sdkmanager --licenses

# Install build-tools 36.0.0
yes | sdkmanager "build-tools;36.0.0"

# Install platform 36 — sdkmanager often fails with Zip errors on Termux,
# so try it first, then fall back to manual download + extract
echo "Installing Android SDK Platform 36..."
if ! yes | sdkmanager "platforms;android-36" 2>/dev/null; then
    echo "sdkmanager failed — falling back to manual download..."
    mkdir -p "$ANDROID_HOME/platforms"
    # Download the platform ZIP directly from Google's mirror
    wget -O /tmp/android-36.zip "https://dl.google.com/android/repository/platform-36_r02.zip" || \
    wget -O /tmp/android-36.zip "https://dl.google.com/android/repository/platform-36_r01.zip"
    unzip -o /tmp/android-36.zip -d "$ANDROID_HOME/platforms/"
    rm /tmp/android-36.zip
    # The extracted folder should be named "android-36"
    if [ -d "$ANDROID_HOME/platforms/android-36" ]; then
        echo "Platform 36 installed manually ✓"
    else
        # Try to find and rename the extracted folder
        EXTRACTED=$(find "$ANDROID_HOME/platforms/" -maxdepth 1 -type d -name "*36*" | head -1)
        if [ -n "$EXTRACTED" ] && [ "$EXTRACTED" != "$ANDROID_HOME/platforms/android-36" ]; then
            mv "$EXTRACTED" "$ANDROID_HOME/platforms/android-36"
            echo "Platform 36 installed manually (renamed) ✓"
        else
            echo "ERROR: Could not install Android SDK Platform 36"
            echo "Try manually: sdkmanager \"platforms;android-36\""
            exit 1
        fi
    fi
fi

### Step 3: Ensure Gradle 9.5+ or manual install
a=($(gradle -v | sed -n /Gradle/p | awk '{ print $2 }' | awk -F. '{ print $1, $2, $3 }'))
[ "$a" -gt 9 ] ||
[[ "$a" -eq 9 && "${a[1]}" -ge 5 ]] || {
    echo "Installing Gradle 9.5 (your version is too old)..."
    wget -O ~/gradle-9.5-bin.zip https://services.gradle.org/distributions/gradle-9.5-bin.zip
    mkdir -p ~/gradle
    unzip -d ~/gradle ~/gradle-9.5-bin.zip
    mv ~/gradle/gradle-9.5 ~/gradle/gradle
    rm ~/gradle-9.5-bin.zip
}

# Add Gradle + SDK to PATH (idempotent — checks before appending)
grep -q 'gradle/gradle/bin' ~/.bashrc || cat >> ~/.bashrc << 'EOF'
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/build-tools/36.0.0:$ANDROID_HOME/cmdline-tools/latest/bin:~/gradle/gradle/bin
EOF

export PATH=$PATH:~/gradle/gradle/bin

### Step 4: Set up the build

# Symlink aapt2 if missing (Termux sometimes needs this)
AAPT2_PATH=$ANDROID_HOME/build-tools/36.0.0/aapt2
if [ ! -f "$AAPT2_PATH" ]; then
    ln -sf "$(which aapt2)" "$AAPT2_PATH" 2>/dev/null || true
fi

# Write local.properties with the resolved ANDROID_HOME path (not empty!)
echo "sdk.dir=$ANDROID_HOME" > local.properties

# Patch CI workflow Gradle version if needed
sed -i '/gradle-version:/s/8.9/9.5/' ../.github/workflows/build.yml 2>/dev/null || true

# Build the release APK:
echo "Starting build..."
chmod +x gradlew
./gradlew assembleRelease

# The APK will be at:
# app/build/outputs/apk/release/app-release.apk
