#!/usr/bin/env bash
set -e

### Prerequisites
export ANDROID_HOME=~/android-sdk

### Step 1: Install packages
pkg update -y && pkg upgrade -y
pkg install openjdk-17 git wget unzip gradle aapt2 -y

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

### Find a working ARM aapt2
# Priority: Termux pkg (native ARM) > SDK build-tools
AAPT2_PATH=""
# Check Termux-native aapt2 first (installed via pkg install aapt2)
if [ -x /data/data/com.termux/files/usr/bin/aapt2 ]; then
    AAPT2_PATH=/data/data/com.termux/files/usr/bin/aapt2
    echo "Using Termux-native aapt2: $AAPT2_PATH"
fi
# Fallback: SDK build-tools aapt2
if [ -z "$AAPT2_PATH" ]; then
    BUILT_AAPT2=$ANDROID_HOME/build-tools/36.0.0/aapt2
    if [ -x "$BUILT_AAPT2" ]; then
        AAPT2_PATH=$BUILT_AAPT2
        echo "Using SDK build-tools aapt2: $AAPT2_PATH"
    fi
fi
if [ -z "$AAPT2_PATH" ]; then
    echo "ERROR: No ARM-compatible aapt2 found. Run: pkg install aapt2"
    exit 1
fi

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

### Step 4: Write aapt2 override to user-level gradle.properties
# This is read for ALL Gradle builds and isn't tracked by git
mkdir -p ~/.gradle
# Remove any existing override to avoid duplicates
sed -i '/android.aapt2FromMavenOverride/d' ~/.gradle/gradle.properties 2>/dev/null || true
echo "android.aapt2FromMavenOverride=$AAPT2_PATH" >> ~/.gradle/gradle.properties
echo "✅ Wrote aapt2 override to ~/.gradle/gradle.properties"

### Step 5: Build
echo "sdk.dir=$ANDROID_HOME" > local.properties
sed -i '/gradle-version:/s/8.9/9.5/' ../.github/workflows/build.yml 2>/dev/null || true

echo "Starting build..."
chmod +x gradlew
./gradlew assembleRelease \
    -Dandroid.aapt2FromMavenOverride="$AAPT2_PATH" \
    -Pandroid.aapt2FromMavenOverride="$AAPT2_PATH"

# APK at: app/build/outputs/apk/release/app-release.apk
