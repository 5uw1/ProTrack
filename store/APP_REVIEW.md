# App Review Information (Guideline 2.1 answers)

Paste the block below into **App Store Connect → App Review Information → Notes** and send the
same text as the reply in the Resolution Center. It answers the six points Apple asks for on a new
app submission. Keep it here so future submissions reuse the same wording.

Quick facts: no account, no login, no in-app purchases, no user-generated content shared with
anyone, no network access at all. Sign-in required: **No** – no demo account needed.

---

## Notes text (paste verbatim)

**Demo account:** none required. The app has no accounts and no login; every feature is available
immediately after launch.

**1. Screen recording**

A screen recording made on an iPhone running the latest iOS is attached. It starts with the app
launch and shows the typical flow: clocking in, picking a project and task, switching tasks,
tagging a lunch break, clocking out, correcting a time afterwards, the day / week / month reports
with hours per project, exporting a CSV and the "SAP week" text, entering an absence, the settings
(work schedule, break rules, backup) and the home-screen widget.

The app has no account registration, no login and no account deletion flow, because it has no
accounts. It has no user-generated content that is shared with other users, so no reporting or
blocking mechanism is required. It has no paid content and no in-app purchases; the full app is
included in the download.

**2. Purpose and target audience**

WorkTracker is an offline time tracker for employees and freelancers who have to book their working
hours onto projects – engineers, consultants, technicians, anyone who fills in a timesheet at the
end of the week or month.

The problem it solves: people record their hours on paper or in a spreadsheet, then try to remember
weeks later how long they worked on which project. WorkTracker records it as it happens – one tap to
clock in, one tap to switch task – and produces the numbers the timesheet asks for: hours per
project (rounded to 0.25 h), hours per category, target hours, overtime balance, breaks and
absences. It also warns when a legal working-time rule is about to be broken (missing break, day
too long, week over the weekly maximum); the default values follow Swiss rules and are fully
configurable.

The audience is adults in working life (age rating 4+, no objectionable content). The app is sold to
the general public – it is not limited to one company's employees and needs no employer account,
company e-mail or internal server.

**3. Setting up and accessing the main features**

No credentials, no sample files and no configuration are needed. On first launch the app opens on
the Today tab with an empty day.

1. **Today → "Clock in"** – starts the working day. The timer runs and the day's totals update.
2. **Pick a project / task** – tap the activity row and choose a project and a category (the app
   ships with none; add a project with the "+" button on the Projects tab, or paste a list of
   project numbers under Settings → Import). "No project" is also allowed.
3. **Switch task** – choosing another project/task closes the running activity and starts the next
   one. The "···" menu tags a pause as lunch or break.
4. **Clock out** – ends the period. Times can be edited afterwards by tapping any entry.
5. **Reports tab** – day / week / month navigation, hours per project number, per category, target
   and overtime, daily chart. "Export" produces CSV files and the "SAP week" text block via the
   iOS share sheet.
6. **Absences** – Today tab → absence button: sick, holiday, public holiday, compensation,
   education or a custom reason.
7. **Settings** – work schedule and break rules, notifications, "Backup & transfer" (writes one
   JSON file, restores it on any device), optional automatic backup into a folder the user picks
   (for example iCloud Drive), and import from a CSV export of another app.
8. **Widget** – long-press the home screen → add the WorkTracker widget: today's clocked-in time,
   target progress, the running activity, a Clock in / Clock out button and one-tap task switching
   (buttons require iOS 17).

Data is deleted by deleting the individual records in the app, or by deleting the app, which removes
its database. Backup files the user exported stay where the user put them.

**4. External services, tools and platforms**

None. The app makes no network requests at all and contains no third-party SDK – no analytics, no
advertising, no tracking, no crash reporting, no authentication provider, no payment processor, no
AI service, no data provider, no backend of our own. Everything is computed on the device and
stored in an SQLite database in the app's private container.

The only components used are Apple system frameworks (SwiftUI/UIKit, WidgetKit, UserNotifications
for local reminders, the share sheet for exports, security-scoped bookmarks and the document picker
for the optional backup folder, App Groups to feed the widget) and open-source libraries compiled
into the binary (Kotlin Multiplatform / Compose Multiplatform for the UI, Room/SQLite for local
storage). If the user chooses iCloud Drive as the backup folder, the file is written through the
system document picker into the user's own iCloud Drive; the app has no account there and no
server component of its own.

The App Privacy answers are therefore "Data Not Collected" in every category, matching the
privacy manifest in the build.

**5. Regional differences**

There are none. The app behaves identically in every region and every country; nothing is gated by
region, and the app does not read the device location. The interface is localized into English,
German and French and follows the device language and the device's date, time and number formats.

The only region-related content is a default setting: the shipped working-time defaults (40 h week,
45 h weekly maximum, 30 min break after 5 h, 1 h after 9 h) follow Swiss rules, and every one of
those values is editable in Settings → Work schedule, so a user anywhere can enter their own
country's or employer's rules. No feature is unavailable anywhere.

**6. Regulated industry / third-party material**

The app is not part of a regulated industry. It is a personal productivity tool: it does not provide
payroll, banking, health, gambling or legal services, it does not transmit data to any employer or
authority, and it makes no claim to be an official or certified record of working time.

It contains no protected third-party material. All text, icons and graphics are our own. Timesheet
systems such as SAP are mentioned only descriptively, to say that the app's export produces plain
text/CSV the user can copy into the timesheet their employer uses. There is no connection to SAP or
any other vendor's system, no SAP software, SDK or data is used, and no third-party logo or brand
appears in the app or in the screenshots. If Apple would prefer the reference removed from the
metadata, we will remove it right away.

---

## Screen recording – what to capture

Record on a physical iPhone on the latest iOS (Settings → Control Centre → Screen Recording),
portrait, roughly 2–4 minutes, starting from the home screen with the app not running:

1. Tap the app icon – show the launch and the empty Today tab.
2. Projects tab → "+" → add a project (or Settings → Import → paste two lines) so the recording
   shows where projects come from.
3. Today → **Clock in** → pick the project and a category → let the timer run a few seconds.
4. Switch to another task; add a note.
5. "···" → tag a pause as **Lunch**, then resume.
6. **Clock out**, then tap an entry and correct its time to show editing.
7. Absence: add e.g. "Holiday" on another day.
8. Reports tab: day → week → month, hours per project, overtime; **Export** → CSV and
   **SAP week** → show the share sheet (cancel it).
9. Settings: work schedule, break rules, Backup & transfer (show the share sheet), automatic
   backup folder picker (cancel it).
10. Home screen: long-press → add the WorkTracker widget → tap **Clock in** on the widget → open
    the app to show the state matched.

Nothing in the app is behind a login or a purchase, so there is nothing else to unlock on camera.
Upload the file in the Resolution Center reply (App Store Connect accepts .mp4/.mov; if it is too
large, trim it or share an unlisted link).
