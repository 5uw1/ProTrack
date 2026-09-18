#!/usr/bin/env bash
# Phone screenshots for Google Play from the running emulator (debug build, demo data).
# Usage: store/tools/capture_android.sh [apk]   -> store/screenshots/android/phone/<lang>/NN-<tab>.png
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
# Always the emulator, even when a phone is plugged in (override with ANDROID_SERIAL).
export ANDROID_SERIAL="${ANDROID_SERIAL:-$("$ADB" devices | awk '/^emulator-/{print $1; exit}')}"
APK="${1:-$ROOT/androidApp/build/outputs/apk/debug/androidApp-debug.apk}"
PKG=com.suw1labs.worktracker
OUT="$ROOT/store/screenshots/android/phone"
DEMO_TIME=15:20
HHMM=1520

"$ADB" install -r -t "$APK" >/dev/null
"$ADB" shell pm grant $PKG android.permission.POST_NOTIFICATIONS
# Permission granted (no system dialog) but posting blocked, so no heads-up notification lands in a screenshot.
"$ADB" shell appops set $PKG POST_NOTIFICATION ignore
"$ADB" shell cmd notification set_dnd on >/dev/null 2>&1 || true
"$ADB" shell settings put system time_12_24 24
# Clean status bar (demo mode): fixed clock, full battery, wifi, no notifications.
"$ADB" shell settings put global sysui_demo_allowed 1
"$ADB" shell am broadcast -a com.android.systemui.demo -e command enter >/dev/null
"$ADB" shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm "$HHMM" >/dev/null
"$ADB" shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false >/dev/null
"$ADB" shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e mobile show -e datatype none -e level 4 >/dev/null
"$ADB" shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false >/dev/null
"$ADB" shell settings put global window_animation_scale 0
"$ADB" shell settings put global transition_animation_scale 0
"$ADB" shell settings put global animator_duration_scale 0

i=0
for lang in en de fr; do
  mkdir -p "$OUT/$lang"
  n=1
  for tab in today reports tasks settings; do
    "$ADB" shell am start -S -W -n "$PKG/.MainActivity" --ez demo true --es tab "$tab" --es lang "$lang" --es time "$DEMO_TIME" >/dev/null
    sleep 6
    "$ADB" exec-out screencap -p > "$OUT/$lang/0$n-$tab.png"
    echo "captured $lang/$tab"
    n=$((n+1))
  done
done
"$ADB" shell am broadcast -a com.android.systemui.demo -e command exit >/dev/null
"$ADB" shell cmd notification set_dnd off >/dev/null 2>&1 || true
