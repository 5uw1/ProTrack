# WorkTracker

Personal working-time tracker for a full-time software engineer whose hours are booked into
an ERP timesheet per project at the end of the month. Built with **Kotlin Multiplatform + Compose
Multiplatform** and runs on **Android, iOS and desktop (Windows, macOS, Linux)** from one
codebase. The app never talks to company systems: projects are pasted in, reports are exported
as files.

## What it does

* **Clock in / clock out** – morning, lunch, breaks, leaving for a few hours, going home. Only
  clocked-in time counts; every clock-in period can be corrected later.
* **Activities** – while clocked in, pick the project (or "no project"), a category
  (PLC, High Level Language, Meeting, or unproductive ones such as coffee / smoke break,
  informal meeting, uncategorized) and a note. Switching starts a new entry, clocking out stops it.
  Entries can be edited, re-assigned or added by hand at any time.
* **Today** – clocked-in total, project vs. unproductive vs. unallocated time, breaks, target for the
  day, overtime today and the running overtime balance.
* **Reports** – day / week / month navigation with hours per project number (rounded to 0.25 h),
  hours per category, target and overtime, daily chart.
* **Swiss working-time rules (canton of Bern)** – configurable schedule (default 40 h/week at
  100 %, legal maximum 45 h/week). Adjustable break rules (default: more than 5 h needs 30 min,
  more than 9 h needs 1 h; lunch and breaks count together). A break that was not taken is deducted
  from the counted time automatically (9 h 30 clocked in with 30 min break counts as 9 h; can be
  switched off), and the day is flagged. Warnings also when a week exceeds 45 h or a clock-out was
  forgotten.
* **Lunch and breaks** – clock out with one tap, or tag the pause as lunch or break from the "···"
  menu. Lunch time is summed per day / week / month. Companies that credit short breaks (e.g. 10
  minutes of coffee / smoke breaks a day) set "Paid break per day" in the work schedule: breaks
  tagged as Break then count as working time up to that allowance (7 h 50 + 2 × 5 min = 8 h).
* **Absences** – sick, holiday (own vacation), public holiday (paid by company), compensation
  (taken from overtime), education, or any custom reason. Paid absences count towards the target.
* **Looks like the platform it runs on** – iOS 26 gets a real Liquid Glass tab bar
  (`UIGlassEffect`) with the content scrolling underneath it and no title bar at all; Android and
  desktop use Material 3 Expressive navigation. Both come from the same screens – see
  [App shell](#app-shell--following-each-platforms-design).
* **Home-screen widgets (Android & iOS)** – clocked-in time today, target progress, the running
  activity, a Clock in / Clock out button and one-tap task switching (running task first, then the
  most recently used ones). Android uses Jetpack Glance and works on the shared database directly.
  iOS uses WidgetKit (home screen, lock screen; buttons need iOS 17): the app publishes a
  `WidgetSnapshot` into the App Group `group.com.suw1labs.worktracker`, the widget shows a live
  timer, and its buttons queue `{type, at, taskId}` actions in the App Group which the app replays
  with their original timestamps as soon as it becomes active. The App Group must be enabled for
  both the app and the `WorkTrackerWidget` extension in the Apple developer account.
* **Backup & automatic backup** – "Backup & transfer" saves or shares everything as one JSON
  file and restores it on any device. "Automatic backup" mirrors the same file into a folder you
  choose – Google Drive, iCloud Drive or a local folder – a few seconds after every change
  (`worktracker-backup.json`, only rewritten when the data changed). The database itself stays in
  the app's private storage: SQLite must not live on a cloud-synced folder. On Android the folder
  is a Storage Access Framework tree, on iOS a security-scoped bookmark, on desktop a path; the
  choice is kept outside the database so it never travels inside a backup.
* **Import from the iOS app WORK** – Settings → "Import from other apps" reads WORK's CSV export
  (Export → CSV): every "DAYS IN DETAIL" row becomes an activity, runs of rows become clock-in
  periods with lunch / break tags from the pause rows, absences become day records; projects and
  tasks are created by name; rows already present are skipped so re-importing is safe.
* **Weekly sheet (copy & paste)** – Reports → Export → "Weekly sheet": tab-separated lines per calendar
  week exactly as a weekly time sheet expects them (`INTERNAL  1000  0.50 …`,
  `PROJECT  P-1234.5  3.00 5.00 …`), hours with two decimals, Monday first, empty cells for
  nothing to book; time without a project is listed with a leading `!` so it is not pasted by
  mistake. Activity types and the unproductive cost object are set in Settings → Timesheet booking.
* **Import / export** – paste a project list (from a spreadsheet, an ERP or CSV; tab, `;` or `,` separated)
  to import project numbers. Export the summary per project, a daily timesheet or an
  attendance/overtime report as plain CSV or spreadsheet-friendly CSV (semicolon + UTF-8 BOM).

## Project layout

| Module / folder | Purpose |
| --- | --- |
| `composeApp/` | Shared Kotlin Multiplatform module: all UI (Compose), view models, Room database, export generation. Also contains the desktop entry point (`desktopMain`) and the iOS framework sources (`iosMain`). |
| `androidApp/` | Thin Android application (activity, manifest, notifications via AlarmManager, share sheet, Firebase). |
| `iosApp/` | Xcode project that embeds the `ComposeApp` framework produced by `composeApp`, plus the `WorkTrackerWidget` WidgetKit extension (Swift, reads the App Group). |

### App shell – following each platform's design

Design languages move: Material 3 became expressive, iOS 26 became Liquid Glass, and both will
change again. Everything that follows such a fashion – title bar, navigation, snackbars – lives
behind `AppShell` in `composeApp/src/commonMain/kotlin/com/suw1labs/worktracker/ui/shell/`, so a
new look is a new implementation instead of an edit through the whole app. Screens, view models
and navigation state know nothing about it.

| Shell | Chrome | Used by |
| --- | --- | --- |
| `MaterialShell` | Material 3 Expressive: `ShortNavigationBar` on phones, `WideNavigationRail` on tablets and desktop, content between the bars | Android, desktop |
| `LiquidGlassShell` | iOS 26: a `UIGlassEffect` tab bar as an interop overlay, no title bar (the heading is part of the content), content fills the window | iOS (phones; wide windows fall back to `MaterialShell`) |
| `FloatingShell` | The floating bar drawn with Compose, no native glass | fallback, and to see that look on any platform |

`AppShell.Chrome(state, wide) { modifier -> … }` draws the chrome and hands the screen the
modifier it should use. A shell that lays content out between its bars passes the padding in that
modifier; a shell that draws over the content passes `Modifier.fillMaxSize()` and publishes the
space to keep free as `LocalScreenInsets`, which every scrolling screen adds as `contentPadding`.

To add a design: implement `AppShell`, then either return it from `platformAppShell()` for a
platform or provide it locally, e.g. for a screenshot test or a setting:

```kotlin
CompositionLocalProvider(LocalAppShell provides FloatingShell()) { App(container) }
```

Platform-specific behaviour is isolated behind small interfaces in `composeApp/src/commonMain/kotlin/com/example/platform/`:

* `ReminderScheduler` – AlarmManager + notifications (Android), `UNUserNotificationCenter` (iOS), system-tray notifications (desktop).
* `FileExporter` – share sheet (Android/iOS) or native save dialog (desktop).
* `NotificationPermissionEffect` – runtime notification permission where the platform needs it.
* `WidgetBridge` – re-renders the Glance widget (Android) or writes the App Group defaults and reloads WidgetKit (iOS); no-op on desktop.

## Building & running

Android (requires Android SDK, compileSdk 37):

```bash
./gradlew :androidApp:assembleDebug
```

Desktop – run directly:

```bash
./gradlew :composeApp:run
```

Desktop – native installers (`.msi`/`.exe` must be built on Windows, `.dmg` on macOS, `.deb` on Linux; requires JDK 17+ with `jpackage`):

```bash
./gradlew :composeApp:packageDistributionForCurrentOS
```

The installer is written to `composeApp/build/compose/binaries/main/<format>/`.

### Windows

On a Windows machine (JDK 25 and [WiX Toolset 3.x](https://wixtoolset.org/) installed):

```bash
gradlew.bat :composeApp:packageMsi :composeApp:packageExe
```

Without a Windows machine, push to `main` (or run the workflow manually): the
[Build](.github/workflows/build.yml) GitHub Actions workflow builds the Windows `.msi`/`.exe`,
the macOS `.dmg`, the Linux `.deb` and the Android APK/AAB and attaches them as downloadable artifacts
to the workflow run. The app stores its data under `%APPDATA%\WorkTracker` on Windows.

## Releases (CI)

Push a tag to publish a release:

```bash
git tag v1.0.0 && git push origin v1.0.0
```

The workflow stamps that version into every platform, runs the tests, builds all installers and
creates a GitHub Release with auto-generated notes (`v1.0.0-rc1` etc. become pre-releases).
Builds from `main` are versioned `1.0.0-dev.<run number>`.

Signing and store upload are optional and switch on automatically once these repository secrets exist
(`ci_scripts/set-play-secrets.sh <service-account.json>` sets all of the Android ones in one go):

| Secret | Purpose |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` | Sign the release APK/AAB with the Play upload key (`base64 -i upload.jks`). |
| `PLAY_SERVICE_ACCOUNT_JSON` | Upload the AAB to the Google Play internal testing track on every tag. |
| `MACOS_CERT_P12`, `MACOS_CERT_PASSWORD`, `MACOS_SIGNING_IDENTITY` | Developer ID signing of the macOS app. |
| `APPLE_ID`, `APPLE_APP_PASSWORD`, `APPLE_TEAM_ID` | Notarize the DMG with Apple. |

Privacy policy for the stores: [PRIVACY.md](PRIVACY.md).

iOS (requires macOS + Xcode; the app targets **iOS 26 and later**, because the glass chrome uses
`UIGlassEffect`): open `iosApp/iosApp.xcodeproj` in Xcode and run the `iosApp`
scheme, or from the command line:

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build
```

Set your `TEAM_ID` in `iosApp/Configuration/Config.xcconfig` for device builds.

### TestFlight via Xcode Cloud

GitHub Actions does not build iOS (no Apple signing there). TestFlight builds come from **Xcode
Cloud**, which watches the GitHub repository on its own: App Store Connect → Xcode Cloud → a workflow
with an *Archive – iOS* action and *TestFlight (Internal Testing)* as post-action, started on
pushes to `main` or on tags. `iosApp/ci_scripts/ci_post_clone.sh` prepares the runner: it installs
a JDK and a minimal Android SDK (the Gradle build configures the Android module too) and writes
`local.properties`. Prerequisites on the Apple side: the app record with bundle id
`com.suw1labs.worktracker`, the widget id `com.suw1labs.worktracker.widget`, and the App Group
`group.com.suw1labs.worktracker` enabled on both; automatic signing uses the team in the project.

## Tests

```bash
./gradlew :composeApp:desktopTest :androidApp:testDebugUnitTest
```

Common tests live in `composeApp/src/commonTest` and run on every target; Robolectric /
Roborazzi screenshot tests live in `androidApp/src/test`.
