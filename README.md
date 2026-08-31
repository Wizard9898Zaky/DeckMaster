# DeckMaster

DeckMaster is an Android cartomancy app built with Jetpack Compose.

## Building on Termux

You can compile DeckMaster into an installable APK directly on your phone using [Termux](https://termux.dev/). No root access required.

### Prerequisites

- Termux installed from [F-Droid](https://f-droid.org/en/packages/com.termux/) (the Play Store version is outdated)
- ~2 GB free storage for the Android SDK + build tools
- An ARM64 device (most modern Android phones)

### Step 1: Install packages

```bash
pkg update -y && pkg upgrade -y
pkg install openjdk-17 git wget unzip -y
```

Verify Java:

```bash
java -version
# Should show version 17
```

### Step 2: Install the Android SDK

```bash
# Download the Termux SDK installer
wget -O ~/install-android-sdk.sh https://raw.githubusercontent.com/Sohil876/termux-sdk-installer/main/installer.sh
chmod +x ~/install-android-sdk.sh
bash ~/install-android-sdk.sh -i
```

Accept licenses and install the platform + build tools (API 35, matching `compileSdk`):

```bash
yes | sdkmanager --licenses
yes | sdkmanager "platforms;android-36" "build-tools;36.0.0"
```

### Step 3: Install Gradle 9.5+ (manual install)

The Termux `pkg install gradle` version is too old for the Android Gradle Plugin (AGP 9.3.0). Install Gradle 9.5+ manually to match the CI:

```bash
wget -O ~/gradle-9.5.0-bin.zip https://services.gradle.org/distributions/gradle-9.5.0-bin.zip
mkdir -p ~/gradle
unzip -d ~/gradle ~/gradle-9.5.0-bin.zip
mv ~/gradle/gradle-9.5.0 ~/gradle/gradle
rm ~/gradle-9.5.0-bin.zip
```

Add Gradle to your PATH:

```bash
cat >> ~/.bashrc << 'EOF'
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/build-tools/36.0.0:$ANDROID_HOME/cmdline-tools/latest/bin:~/gradle/gradle/bin
EOF
source ~/.bashrc
```

Verify Gradle:

```bash
gradle -v
# Should show Gradle 9.5+
```

### Step 4: Fix the aapt2 symlink

Termux ships its own `aapt2` in a different location than the SDK expects. Link it:

```bash
AAPT2_PATH=$(which aapt2)
ln -sf "$AAPT2_PATH" $ANDROID_HOME/build-tools/36.0.0/aapt2
```

### Step 5: Clone and build

```bash
git clone https://github.com/Wizard9898Zaky/DeckMaster.git
cd DeckMaster/DeckMaster
```

Create `local.properties` pointing to the SDK:

```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

Build the release APK:

```bash
chmod +x gradlew
./gradlew assembleRelease
```

The `gradlew` script delegates to the system-installed Gradle via `which gradle`, so it picks up Gradle 9.5+ from Step 3 automatically.

The APK will be at:

```
app/build/outputs/apk/release/app-release.apk
```

### Step 6: Install on your phone

```bash
termux-open app/build/outputs/apk/release/app-release.apk
```

This opens the Android installer to install the APK directly.

### Troubleshooting

| Problem | Fix |
|---|---|
| **Plugin not found / AGP resolve failure** | You need Gradle 9.5+ (Step 3). Run `gradle -v` to check. The `pkg install gradle` version is too old. |
| **Out of memory** | The `gradle.properties` is already tuned for low-RAM devices (`-Xmx768m`, single worker, no daemon). If it still OOMs, try closing other apps first. |
| **`gradlew` permission denied** | Run `chmod +x gradlew` |
| **SDK location not found** | Create `local.properties` in the `DeckMaster/` directory with `sdk.dir=/data/data/com.termux/files/home/android-sdk` |
| **aapt2 crashes** | This is the most common Termux build issue. Run `pkg install aapt2` and re-create the symlink from Step 4. |
| **`build-tools;36.0.0` not found** | Run `yes \| sdkmanager "build-tools;36.0.0"` again — it may have failed silently during license acceptance. |
| **Daemon environment warning** | Harmless on Termux. The "no native integration" message is expected — it won't affect the build. |

## Building with GitHub Actions

This repo includes a CI workflow (`.github/workflows/build.yml`) that automatically builds a release APK on every push to `main`. You can download the APK from the Actions tab → latest run → Artifacts.

## Project structure

```
DeckMaster/
├── app/                    # App module
│   ├── build.gradle.kts    # App-level Gradle config (Compose, SDK versions)
│   └── src/                # Kotlin source code
├── build.gradle.kts        # Root Gradle config
├── settings.gradle.kts     # Gradle settings
├── gradle.properties       # JVM/worker tuning (low-memory friendly)
└── gradlew                 # Delegates to system-installed Gradle
```

## Tech stack

- **Language:** Kotlin
- **UI:** Jetpack Compose with Material 3
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 35 (Android 15)
- **Build:** Gradle Kotlin DSL
