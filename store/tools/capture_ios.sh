#!/usr/bin/env bash
# App Store screenshots from the iOS simulator (Debug build, demo data).
# Usage: store/tools/capture_ios.sh   -> store/screenshots/ios/<device>/<lang>/NN-<tab>.png
# Builds first: xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
#   -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 17 Pro Max' -derivedDataPath build/ios-sim CODE_SIGNING_ALLOWED=NO build
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
APP="$ROOT/build/ios-sim/Build/Products/Debug-iphonesimulator/WorkTracker.app"
BUNDLE=com.suw1labs.worktracker
OUT="$ROOT/store/screenshots/ios"
DEMO_TIME=15:20

capture_device() {
  local device="$1" folder="$2"
  local udid
  udid=$(xcrun simctl list devices available -j | python3 -c "import sys,json;d=json.load(sys.stdin);print(next(x['udid'] for v in d['devices'].values() for x in v if x['name']=='$device'))")
  xcrun simctl boot "$udid" 2>/dev/null || true
  xcrun simctl bootstatus "$udid" -b >/dev/null
  xcrun simctl install "$udid" "$APP"
  for lang in en de fr; do
    mkdir -p "$OUT/$folder/$lang"
    # System language = screenshot language (status bar date, system dialogs); written on the booted
    # device, applied by a reboot.
    xcrun simctl boot "$udid" >/dev/null 2>&1 || true
    xcrun simctl bootstatus "$udid" -b >/dev/null
    xcrun simctl spawn "$udid" defaults write "Apple Global Domain" AppleLanguages -array "$lang" >/dev/null
    locale=en_US; [[ "$lang" == de ]] && locale=de_CH; [[ "$lang" == fr ]] && locale=fr_CH
    xcrun simctl spawn "$udid" defaults write "Apple Global Domain" AppleLocale -string "$locale" >/dev/null
    xcrun simctl spawn "$udid" defaults write "Apple Global Domain" AppleICUForce24HourTime -bool true >/dev/null
    xcrun simctl shutdown "$udid" >/dev/null 2>&1 || true
    xcrun simctl boot "$udid"
    xcrun simctl bootstatus "$udid" -b >/dev/null
    xcrun simctl status_bar "$udid" override --time "$DEMO_TIME" --batteryState charged --batteryLevel 100 --wifiBars 3 --cellularBars 4 --operatorName ""
    n=1
    for tab in today reports tasks settings; do
      xcrun simctl terminate "$udid" "$BUNDLE" 2>/dev/null || true
      xcrun simctl launch "$udid" "$BUNDLE" -demo -tab "$tab" -lang "$lang" -time "$DEMO_TIME" >/dev/null
      sleep 6
      xcrun simctl io "$udid" screenshot "$OUT/$folder/$lang/0$n-$tab.png" >/dev/null
      echo "captured $folder/$lang/$tab"
      n=$((n+1))
    done
  done
  xcrun simctl terminate "$udid" "$BUNDLE" 2>/dev/null || true
  xcrun simctl status_bar "$udid" clear
}

capture_device "iPhone 17 Pro Max" "iphone-6.9"
capture_device "iPad Pro 13-inch (M5)" "ipad-13"
