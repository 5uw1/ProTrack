# Store listing kit

Everything needed for Google Play Console and App Store Connect. Screenshots come from the
debug / simulator build with demo data (`-demo`), see `tools/`.

## Contents

| Path | What | Where it goes |
|---|---|---|
| `icons/play-icon-512.png` | 512×512 PNG, no alpha, full-bleed | Play Console → Main store listing → App icon |
| `icons/appstore-icon-1024.png` | 1024×1024 PNG, no alpha, square (Apple masks the corners) | App Store Connect is fed from `iosApp/Assets.xcassets/AppIcon`; this copy is for the listing / marketing |
| `graphics/play-feature-graphic-1024x500.png` | Feature graphic | Play Console → Main store listing → Feature graphic (required) |
| `screenshots/android/phone/<lang>/` | 1080×2400 raw phone screenshots, 4 per language (Today, Reports, Projects, Settings) | Reference / re-framing |
| `screenshots/android/phone/<lang>/framed/` | 1080×1920 (9:16) marketing versions with a caption | Play Console → Phone screenshots (2–8) |
| `screenshots/ios/iphone-6.9/<lang>/` and `…/framed/` | 1320×2868 iPhone 17 Pro Max, raw and captioned | App Store Connect → iPhone 6.9" display |
| `screenshots/ios/iphone-6.5/<lang>/framed/` | 1284×2778 captioned (same shots re-framed) | App Store Connect → iPhone 6.5" display (what the listing page asks for on this app record) |
| `screenshots/ios/ipad-13/<lang>/` and `…/framed/` | 2064×2752 iPad Pro 13", raw and captioned | App Store Connect → iPad 13" display (required because the app supports iPad) |
| `metadata/<locale>/` | title, subtitle, short description, full description, keywords, promotional text, release notes | See the table below |
| `APP_REVIEW.md` | Answers for App Review (purpose, feature walk-through, services, regions) and the screen-recording shot list | App Store Connect → App Review Information → Notes, and Resolution Center replies |
| `../PRIVACY.md` | Privacy policy text | Public URL: https://github.com/5uw1/ProTrack/blob/main/PRIVACY.md – paste it into both consoles |

## Text fields per store

| File | Google Play | App Store Connect |
|---|---|---|
| `title.txt` (≤30) | App name | Name |
| `subtitle.txt` (≤30) | – | Subtitle |
| `short_description.txt` (≤80) | Short description | – |
| `full_description.txt` (≤4000) | Full description | Description |
| `keywords.txt` (≤100) | – | Keywords |
| `promotional_text.txt` (≤170) | – | Promotional text |
| `release_notes.txt` | Release notes ("What's new") | What's New in This Version |

Locales: `en-US` (default), `de-DE`, `fr-FR`. In Play Console add German and French as listing
translations; in App Store Connect add German and French localizations.

## Google Play – other required answers

* **Category**: Productivity (or Business). **Tags**: Time tracking, Productivity.
* **Contact e-mail**: suwijakza@gmail.com. **Privacy policy URL**: https://github.com/5uw1/ProTrack/blob/main/PRIVACY.md.
* **App access**: all functionality available without login.
* **Ads**: no. **Content rating**: fill the IARC questionnaire – no violence, no user interaction, no
  data sharing → "Everyone".
* **Target audience**: 18 and over (business tool). **News app**: no. **COVID-19**: no.
* **Data safety**: "Does your app collect or share user data?" → **No**. All data stays on the
  device; exports and backups are user-initiated to the user's own storage. Data is not encrypted
  in transit because none is transmitted. Users can delete data by uninstalling or by clearing
  the app storage.
* **Government apps / financial features / health**: no.
* **Release**: the CI uploads the AAB to the *internal testing* track on every tag. Promote the
  tested build to Production from Play Console (Release → Internal testing → Promote release).

## App Store Connect – other required answers

* **Bundle ID**: `com.suw1labs.worktracker`. **SKU**: `worktracker`. **Primary category**:
  Productivity; secondary: Business.
* **Age rating**: none of the listed content → 4+.
* **App Privacy** (nutrition label): "Data Not Collected" for every category – the app has no
  analytics and no network access. `iosApp/PrivacyInfo.xcprivacy` must match (no tracking, no
  required-reason APIs beyond UserDefaults / file timestamps).
* **Support URL**: the GitHub repository. **Privacy Policy URL**: https://github.com/5uw1/ProTrack/blob/main/PRIVACY.md.
* **Export compliance**: uses no encryption beyond the OS (answer "No" / exempt).
* **Sign-in**: none required → no demo account needed for review.
* **Review notes**: paste the notes block from [APP_REVIEW.md](APP_REVIEW.md) – purpose, target
  audience, feature walk-through, external services (none), regional differences (none), regulated
  industry / third-party material. Apple asks for all of this on a new app submission
  (Guideline 2.1) and it must also sit in the App Review Information → Notes field.
* **Screen recording**: required with the first submission – a device recording of the typical
  flow, see the shot list in [APP_REVIEW.md](APP_REVIEW.md).
* **Build**: archive with Xcode (`iosApp.xcodeproj`, scheme `iosApp`) after setting `TEAM_ID` in
  `iosApp/Configuration/Config.xcconfig`, then upload with Xcode Organizer or Transporter. The
  App Group `group.com.suw1labs.worktracker` must exist in the developer account for the widget.

## Regenerating

```bash
python3 store/tools/make_graphics.py                     # icons + feature graphic
./gradlew :androidApp:assembleDebug && store/tools/capture_android.sh   # needs a running emulator (debug build)
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro Max' -derivedDataPath build/ios-sim CODE_SIGNING_ALLOWED=NO build
store/tools/capture_ios.sh
python3 store/tools/frame_screenshots.py                  # captioned marketing versions
```

The demo data uses fictional projects, clients and numbers (Aurora / Helix / Kestrel). A widget
screenshot is not captured automatically: widgets render with the real clock, so they would not
match the demo time; add one by hand if wanted.

Demo flags (debug / simulator builds only): `demo` seeds sample data, `tab` = today | reports |
tasks | settings, `lang` = en | de | fr, `time` = HH:MM shifts the app clock so screenshots and the
status bar agree.
