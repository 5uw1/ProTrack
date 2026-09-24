# Release checklist

Copy this list into the release issue (or tick it here) and go through it before every tag. It is
written from what has actually gone wrong: the screenshots that no longer matched the app, the
employer's codes that shipped as defaults, the language that was added without its store listing.

## 1. Code

- [ ] `./gradlew :composeApp:desktopTest :androidApp:testDebugUnitTest` – green, and run with
      `--rerun-tasks` so nothing is served from the cache.
- [ ] Builds for every target: `./gradlew :composeApp:compileKotlinDesktop :androidApp:assembleDebug`
      and the iOS build (`xcodebuild … -scheme iosApp`).
- [ ] Working tree clean, everything committed on `main`.

## 2. Data – the part that cannot be fixed afterwards

- [ ] Database migration from the **previously released** version, not just from the last commit:
      `MigrationTest` covers every intermediate version; it must pass with the new
      `DATABASE_VERSION` and an exported schema in `composeApp/schemas/`.
- [ ] Restore a backup file written by the released version into the new build.
- [ ] Nothing employer- or person-specific ships as a default (booking codes, project numbers,
      folder paths).

## 3. Languages

- [ ] `StringsCompletenessTest` passes – every language sets every text.
- [ ] New texts read naturally in all of them, not machine-passed through English.
- [ ] Look at the longest (German) and the tallest (Thai) on a phone: nothing clipped, nothing
      pushed off screen.

## 4. Screens

Run the app on a **physical** iPhone and Android phone – the simulator renders Liquid Glass flat
and will not show a layout that only breaks on device:

- [ ] Clock in, switch task, tag a lunch break, clock out, correct a time afterwards.
- [ ] Reports: day / week / month, export CSV and the weekly sheet.
- [ ] Settings: work schedule, language, backup & restore, open source licenses.
- [ ] A dialog over the glass bars (they step aside), and the keyboard in a dialog.
- [ ] Dark mode and light mode.
- [ ] Widgets: every size on the home screen and both lock screen ones – each has to be able to
      clock in and out, and the buttons must reach the app.

## 5. Store listings

- [ ] Screenshots regenerated if anything on screen changed:
      ```bash
      ./gradlew :androidApp:assembleDebug && store/tools/capture_android.sh
      xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
        -sdk iphonesimulator -destination 'name=iPhone 17 Pro Max' -derivedDataPath build/ios-sim \
        CODE_SIGNING_ALLOWED=NO build && store/tools/capture_ios.sh
      python3 store/tools/frame_screenshots.py
      ```
- [ ] Release notes written in every store locale (`store/metadata/<locale>/release_notes.txt`).
- [ ] Keywords still ≤ 100 characters per locale, and free of other companies' trademarks.
- [ ] A language the app now speaks but the listing does not is a decision, not an oversight –
      either add the locale or note why not.

## 6. Legal and review

- [ ] No third-party product names in the interface or the listing beyond plain compatibility
      statements.
- [ ] `Settings → Open source licenses` lists every dependency that ships, after any change to
      `gradle/libs.versions.toml`.
- [ ] [PRIVACY.md](PRIVACY.md) still describes what the app does (no network, no accounts, no
      analytics) – and the App Privacy answers in App Store Connect match it.
- [ ] [store/APP_REVIEW.md](store/APP_REVIEW.md) still true; paste it into App Review Information
      if anything changed.
- [ ] A screen recording only if Apple asks, or if the release adds accounts, purchases or
      user-generated content (see the shot list in `store/APP_REVIEW.md`).

## 7. Ship

- [ ] Tag: `git tag vX.Y.Z && git push origin vX.Y.Z` – CI stamps the version into every platform,
      builds the installers and creates the GitHub release.
- [ ] Android: the AAB lands on the Play internal testing track; test it there, then promote.
- [ ] iOS: Xcode Cloud builds to TestFlight; install that build, walk section 4 again on it, then
      submit.
- [ ] After approval: check the listing renders in every locale on the store itself.
