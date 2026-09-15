# WorkTracker

Personal working-time tracker for a full-time software engineer whose hours are booked into
SAP per project at the end of the month. Built with **Kotlin Multiplatform + Compose
Multiplatform** and runs on **Android, iOS and desktop (Windows, macOS, Linux)** from one
codebase. The app never talks to company systems: projects are pasted in, reports are exported
as files.

## What it does

* **Clock in / clock out** – morning, lunch, breaks, leaving for a few hours, going home. Only
  clocked-in time counts; every clock-in period can be corrected later.
* **Activities** – while clocked in, pick the SAP project (or "no project"), a category
  (PLC, High Level Language, Meeting, or unproductive ones such as coffee / smoke break,
  informal meeting, uncategorized) and a note. Switching starts a new entry, clocking out stops it.
  Entries can be edited, re-assigned or added by hand at any time.
* **Today** – clocked-in total, project vs. unproductive vs. unallocated time, breaks, target for the
  day, overtime today and the running overtime balance.
* **Reports** – day / week / month navigation with hours per SAP project number (rounded to 0.25 h),
  hours per category, target and overtime, daily chart.
* **Swiss working-time rules (canton of Bern)** – configurable schedule (default 40 h/week at
  100 %, legal maximum 45 h/week). Warnings when more than 5 h were worked with less than 30 min
  break, more than 9 h with less than 1 h break, a week exceeds 45 h, or a clock-out was forgotten.
* **Absences** – sick, holiday (own vacation), public holiday (paid by company), compensation
  (taken from overtime), education, or any custom reason. Paid absences count towards the target.
* **Import / export** – paste a project list (from Excel, SAP or CSV; tab, `;` or `,` separated)
  to import project numbers. Export the summary per project, a daily timesheet or an
  attendance/overtime report as plain CSV or Excel-friendly CSV (semicolon + UTF-8 BOM).

## Project layout

| Module / folder | Purpose |
| --- | --- |
| `composeApp/` | Shared Kotlin Multiplatform module: all UI (Compose), view models, Room database, export generation. Also contains the desktop entry point (`desktopMain`) and the iOS framework sources (`iosMain`). |
| `androidApp/` | Thin Android application (activity, manifest, notifications via AlarmManager, share sheet, Firebase). |
| `iosApp/` | Xcode project that embeds the `ComposeApp` framework produced by `composeApp`. |

Platform-specific behaviour is isolated behind small interfaces in `composeApp/src/commonMain/kotlin/com/example/platform/`:

* `ReminderScheduler` – AlarmManager + notifications (Android), `UNUserNotificationCenter` (iOS), system-tray notifications (desktop).
* `FileExporter` – share sheet (Android/iOS) or native save dialog (desktop).
* `NotificationPermissionEffect` – runtime notification permission where the platform needs it.

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
[Build installers](.github/workflows/build.yml) GitHub Actions workflow builds the Windows `.msi`/`.exe`,
the macOS `.dmg`, the Linux `.deb` and the Android APK and attaches them as downloadable artifacts
to the workflow run. The app stores its data under `%APPDATA%\WorkTracker` on Windows.

iOS (requires macOS + Xcode): open `iosApp/iosApp.xcodeproj` in Xcode and run the `iosApp`
scheme, or from the command line:

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build
```

Set your `TEAM_ID` in `iosApp/Configuration/Config.xcconfig` for device builds.

## Tests

```bash
./gradlew :composeApp:desktopTest :androidApp:testDebugUnitTest
```

Common tests live in `composeApp/src/commonTest` and run on every target; Robolectric /
Roborazzi screenshot tests live in `androidApp/src/test`.
