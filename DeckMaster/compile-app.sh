#!/usr/bin/env bash
set -e

### Prerequisites
export ANDROID_HOME=~/android-sdk

### Install packages — aapt2 is the Termux-native binary for your device's exact CPU
pkg update -y && pkg upgrade -y
pkg install openjdk-17 git wget unzip aapt2 -y

### Verify Java:
a=$(java --show-version | head -1 | awk '{ print $2 }')
[ "${a:0:2}" -ge 17 ] || {
    echo "Please upgrade java to version 17+..."
    exit 1
}

### Install the Android SDK
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
    if [ ! -d "$ANDROID_HOME/platforms/android-36" ]; then
        EXTRACTED=$(find "$ANDROID_HOME/platforms/" -maxdepth 1 -type d -name "*36*" | head -1)
        if [ -n "$EXTRACTED" ] && [ "$EXTRACTED" != "$ANDROID_HOME/platforms/android-36" ]; then
            mv "$EXTRACTED" "$ANDROID_HOME/platforms/android-36"
        else
            echo "ERROR: Could not install Android SDK Platform 36"
            exit 1
        fi
    fi
fi

### Find a working aapt2 — prefer Termux pkg (exact device arch) over SDK build-tools
AAPT2_PATH=""
if [ -x /data/data/com.termux/files/usr/bin/aapt2 ]; then
    AAPT2_PATH=/data/data/com.termux/files/usr/bin/aapt2
elif [ -x "$ANDROID_HOME/build-tools/36.0.0/aapt2" ]; then
    AAPT2_PATH=$ANDROID_HOME/build-tools/36.0.0/aapt2
else
    echo "ERROR: No aapt2 found. Run: pkg install aapt2"
    exit 1
fi
echo "Using aapt2: $AAPT2_PATH"

### Write aapt2 override to user-level gradle.properties
mkdir -p ~/.gradle
sed -i '/android.aapt2FromMavenOverride/d' ~/.gradle/gradle.properties 2>/dev/null || true
echo "android.aapt2FromMavenOverride=$AAPT2_PATH" >> ~/.gradle/gradle.properties

### Build
echo "sdk.dir=$ANDROID_HOME" > local.properties
chmod +x gradlew
./gradlew assembleRelease \
    -Dandroid.aapt2FromMavenOverride="$AAPT2_PATH" \
    -Pandroid.aapt2FromMavenOverride="$AAPT2_PATH"

# APK at: app/build/outputs/apk/release/app-release.apk
