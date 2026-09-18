#!/bin/sh
# Xcode Cloud runs this after cloning (it lives next to iosApp.xcodeproj). The Kotlin framework
# build phase runs Gradle, which configures the whole project – including the Android module – so
# the runner needs a JDK and a minimal Android SDK (AGP downloads the rest itself).
set -e

echo "=== JDK ==="
brew install openjdk@21
mkdir -p "$HOME/Library/Java/JavaVirtualMachines"
ln -sfn "$(brew --prefix openjdk@21)/libexec/openjdk.jdk" "$HOME/Library/Java/JavaVirtualMachines/openjdk-21.jdk"
export JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
java -version

echo "=== Android SDK (command-line tools + compile platform) ==="
export ANDROID_HOME="$HOME/Library/Android/sdk"
TOOLS_ZIP="$TMPDIR/cmdline-tools.zip"
mkdir -p "$ANDROID_HOME/cmdline-tools"
curl -fsSL -o "$TOOLS_ZIP" "https://dl.google.com/android/repository/commandlinetools-mac-13114758_latest.zip"
rm -rf "$ANDROID_HOME/cmdline-tools/latest"
unzip -q -o "$TOOLS_ZIP" -d "$ANDROID_HOME/cmdline-tools"
mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
yes | "$SDKMANAGER" --licenses >/dev/null || true
"$SDKMANAGER" --install "platforms;android-37.0" "platform-tools" >/dev/null

# Gradle reads the SDK location from here (settings.gradle.kts only writes it when the folder exists).
REPO="$(cd "$(dirname "$0")/../.." && pwd)"
echo "sdk.dir=$ANDROID_HOME" > "$REPO/local.properties"
echo "wrote $REPO/local.properties"

echo "=== Warm up Gradle (downloads wrapper + dependencies, fails early if something is off) ==="
cd "$REPO"
./gradlew --version
