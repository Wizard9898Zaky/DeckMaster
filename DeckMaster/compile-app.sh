#!/usr/bin/env bash

### Prerequisites

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

# Accept licenses and install the platform + build tools (API 36, matching compileSdk):
yes | sdkmanager --licenses
yes | sdkmanager "platforms;android-36" "build-tools;36.0.0"

### Step 3: Ensure Gradle 9.5+ or manual install
a=($(gradle -v | sed -n /Gradle/p | awk '{ print $2 }' | awk -F. '{ print $1, $2, $3 }'))
[ "$a" -gt 9 ] ||
[[ "$a" -eq 9 && "${a[1]}" -ge 5 ]] || {
    wget -O ~/gradle-9.5-bin.zip https://services.gradle.org/distributions/gradle-9.5-bin.zip
    mkdir -p ~/gradle
    unzip -d ~/gradle ~/gradle-9.5-bin.zip
    mv ~/gradle/gradle-9.5 ~/gradle/gradle
    rm ~/gradle-9.5-bin.zip
}

# Add Gradle to your PATH
cat >> ~/.bashrc << 'EOF'
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/build-tools/36.0.0:$ANDROID_HOME/cmdline-tools/latest/bin:~/gradle/gradle/bin
EOF
source ~/.bashrc

### Step 4: Set AAPT2_PATH to aapt2
AAPT2_PATH=$ANDROID_HOME/build-tools/36.0.0/aapt2

# Create local.properties pointing to the SDK:
echo "sdk.dir=$ANDROID_HOME" > local.properties
sed -i '/gradle-version:/s/8.9/9.5/' ../.github/workflows/build.yml

# Build the release APK:
chmod +x gradlew
./gradlew assembleRelease

# The APK will be at:
# app/build/outputs/apk/release/app-release.apk
