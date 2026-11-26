import WidgetKit
import SwiftUI
import AppIntents

// MARK: - State shared with the app (written from Kotlin, IosWidgetBridge.kt)

struct QuickTask: Identifiable {
    let taskId: Int
    let projectId: Int
    let title: String
    let projectLabel: String
    let productive: Bool
    var id: Int { taskId }
}

/// Snapshot the app leaves in the App Group. Running totals are "fixed part + start date" so the
/// widget can show a live timer without being refreshed every second.
struct WidgetState {
    static let appGroup = "group.com.suw1labs.worktracker"

    var clockedIn = false
    var clockedInSince: Date? = nil
    var attendanceSecondsBefore: TimeInterval = 0
    var targetSeconds: TimeInterval = 0
    var runningProject: String? = nil
    var runningTask: String? = nil
    var runningSince: Date? = nil
    var runningTaskId: Int? = nil
    /// "LUNCH" / "BREAK" while clocked out for a tagged pause today.
    var pauseReason: String? = nil
    var pauseSince: Date? = nil
    var quickTasks: [QuickTask] = []
    var language = "en"
    var updatedAt: Date? = nil

    static var defaults: UserDefaults? { UserDefaults(suiteName: appGroup) }

    static func load() -> WidgetState {
        guard let d = defaults else { return WidgetState() }
        var s = WidgetState()
        s.clockedIn = d.bool(forKey: "widget.clockedIn")
        s.clockedInSince = date(d, "widget.clockedInSince")
        s.attendanceSecondsBefore = TimeInterval(d.integer(forKey: "widget.attendanceSecondsBefore"))
        s.targetSeconds = TimeInterval(d.integer(forKey: "widget.targetSeconds"))
        s.runningProject = d.string(forKey: "widget.runningProject")
        s.runningTask = d.string(forKey: "widget.runningTask")
        s.runningSince = date(d, "widget.runningSince")
        let runningId = d.integer(forKey: "widget.runningTaskId")
        s.runningTaskId = runningId >= 0 && d.object(forKey: "widget.runningTaskId") != nil ? runningId : nil
        s.pauseReason = d.string(forKey: "widget.pauseReason")
        s.pauseSince = date(d, "widget.pauseSince")
        s.quickTasks = (d.array(forKey: "widget.quickTasks") as? [[String: Any]] ?? []).compactMap { m in
            guard let taskId = m["taskId"] as? Int, let projectId = m["projectId"] as? Int, let title = m["title"] as? String else { return nil }
            return QuickTask(taskId: taskId, projectId: projectId, title: title,
                             projectLabel: m["projectLabel"] as? String ?? "", productive: m["productive"] as? Bool ?? true)
        }
        s.language = d.string(forKey: "widget.language") ?? "en"
        s.updatedAt = date(d, "widget.updatedAt")
        return s
    }

    /// Widget texts in the app's language (the widget cannot use the Kotlin translations).
    func text(_ key: String) -> String { WidgetTexts.text(key, language: language) }

    private static func date(_ d: UserDefaults, _ key: String) -> Date? {
        guard d.object(forKey: key) != nil else { return nil }
        return Date(timeIntervalSince1970: d.double(forKey: key))
    }

    /// Virtual start date such that "now - timerStart" equals today's total clocked-in time.
    var timerStart: Date? {
        guard clockedIn, let since = clockedInSince else { return nil }
        return since.addingTimeInterval(-attendanceSecondsBefore)
    }

    func attendanceSeconds(at now: Date) -> TimeInterval {
        guard let start = timerStart else { return attendanceSecondsBefore }
        return max(0, now.timeIntervalSince(start))
    }

    /// The stored totals belong to the day of [updatedAt]. Once the day has changed (the app was not
    /// opened since midnight), yesterday's closed time must not carry into today's total, and an
    /// open period only counts from midnight – the same rules WidgetSnapshots.compute applies.
    func asOf(_ date: Date, calendar: Calendar = .current) -> WidgetState {
        let dayStart = calendar.startOfDay(for: date)
        var s = self
        if let updatedAt, updatedAt < dayStart { s.attendanceSecondsBefore = 0 }
        if s.clockedIn, let since = s.clockedInSince, since < dayStart { s.clockedInSince = dayStart }
        // Only a pause of today is shown, as on the Today screen.
        if s.clockedIn || (s.pauseSince.map { $0 < dayStart } ?? true) {
            s.pauseReason = nil
            s.pauseSince = nil
        }
        return s
    }

    /// The pause going on right now, if any.
    var pause: (reason: String, since: Date)? {
        guard !clockedIn, let reason = pauseReason, let since = pauseSince else { return nil }
        return (reason, since)
    }

    static let preview: WidgetState = {
        var s = WidgetState()
        s.clockedIn = true
        s.clockedInSince = Date().addingTimeInterval(-47 * 60)
        s.attendanceSecondsBefore = 3 * 3600 + 20 * 60
        s.targetSeconds = 8 * 3600
        s.runningProject = "P-2026-0142 · Spindle retrofit"
        s.runningTask = "PLC safety logic"
        s.runningSince = Date().addingTimeInterval(-47 * 60)
        s.runningTaskId = 1
        s.quickTasks = [
            QuickTask(taskId: 1, projectId: 1, title: "PLC safety logic", projectLabel: "P-2026-0142 · Spindle retrofit", productive: true),
            QuickTask(taskId: 2, projectId: 1, title: "HMI screens", projectLabel: "P-2026-0142 · Spindle retrofit", productive: true),
            QuickTask(taskId: 3, projectId: 2, title: "Meeting", projectLabel: "Unproductive", productive: false),
        ]
        return s
    }()
}

/// Small table of the widget's own texts in the app languages.
enum WidgetTexts {
    private static let table: [String: [String: String]] = [
        "clockedIn": ["en": "CLOCKED IN", "de": "EINGESTEMPELT", "fr": "POINTÉ", "zh": "已打卡", "th": "เข้างาน"],
        "clockedOut": ["en": "CLOCKED OUT", "de": "AUSGESTEMPELT", "fr": "DÉPOINTÉ", "zh": "已下班打卡", "th": "ออกงานแล้ว"],
        "ofToday": ["en": "of %@ today", "de": "von %@ heute", "fr": "sur %@ aujourd'hui", "zh": "今日目标 %@", "th": "จาก %@ วันนี้"],
        "clockIn": ["en": "Clock in", "de": "Einstempeln", "fr": "Pointer", "zh": "上班打卡", "th": "เข้างาน"],
        "clockOut": ["en": "Clock out", "de": "Ausstempeln", "fr": "Dépointer", "zh": "下班打卡", "th": "ออกงาน"],
        "switchTo": ["en": "SWITCH TO", "de": "WECHSELN ZU", "fr": "PASSER À", "zh": "切换到", "th": "สลับไป"],
        "tasks": ["en": "TASKS", "de": "AUFGABEN", "fr": "TÂCHES", "zh": "任务", "th": "งาน"],
        "clockInFirst": ["en": "Clock in first, then pick a task.", "de": "Zuerst einstempeln, dann Aufgabe wählen.", "fr": "Pointe d'abord, puis choisis une tâche.", "zh": "先打卡，再选择任务。", "th": "เข้างานก่อน แล้วค่อยเลือกงาน"],
        "addTasks": ["en": "Add tasks in the app", "de": "Aufgaben in der App anlegen", "fr": "Ajoute des tâches dans l'app", "zh": "在应用中添加任务", "th": "เพิ่มงานในแอป"],
        "now": ["en": "NOW", "de": "JETZT", "fr": "EN COURS", "zh": "进行中", "th": "ตอนนี้"],
        // Pause reasons, as the app names them (Strings.reasonLunch / reasonBreak).
        "LUNCH": ["en": "LUNCH", "de": "MITTAG", "fr": "MIDI", "zh": "午休", "th": "พักเที่ยง"],
        "BREAK": ["en": "BREAK", "de": "PAUSE", "fr": "PAUSE", "zh": "休息", "th": "พัก"],
        "resume": ["en": "Back to work", "de": "Weiterarbeiten", "fr": "Reprendre", "zh": "继续工作", "th": "กลับมาทำงาน"],
    ]

    static func text(_ key: String, language: String) -> String {
        let row = table[key] ?? [:]
        return row[language] ?? row["en"] ?? key
    }
}

// MARK: - Actions queued for the app (the extension cannot open the database)

/// Appends an action to the App Group queue and updates the shown state optimistically. The app
/// replays the queue with these timestamps when it next becomes active (WidgetActions.kt).
enum WidgetActionStore {
    /// [reason]: "LUNCH" / "BREAK" for a clock-out from a pause button, nil for a plain one.
    static func perform(_ type: String, taskId: Int? = nil, projectId: Int? = nil, title: String? = nil, projectLabel: String? = nil, reason: String? = nil) {
        guard let d = WidgetState.defaults else { return }
        let now = Date()
        var queue = d.array(forKey: "widget.pendingActions") as? [[String: Any]] ?? []
        var record: [String: Any] = ["type": type, "at": now.timeIntervalSince1970]
        if let taskId { record["taskId"] = taskId }
        if let projectId { record["projectId"] = projectId }
        if let reason { record["reason"] = reason }
        queue.append(record)
        d.set(queue, forKey: "widget.pendingActions")

        // Optimistic state so the widget reflects the tap immediately. Roll the stored totals over
        // to today first, so a clock-in the morning after a widget clock-out starts from zero.
        let state = WidgetState.load().asOf(now)
        d.set(Int(state.attendanceSecondsBefore), forKey: "widget.attendanceSecondsBefore")
        if let since = state.clockedInSince { d.set(since.timeIntervalSince1970, forKey: "widget.clockedInSince") }
        d.set(now.timeIntervalSince1970, forKey: "widget.updatedAt")
        // Any tap ends a pause; a clock-out from a pause button starts a new one below.
        d.removeObject(forKey: "widget.pauseReason")
        d.removeObject(forKey: "widget.pauseSince")
        switch type {
        case "clockIn":
            if !state.clockedIn {
                d.set(true, forKey: "widget.clockedIn")
                d.set(now.timeIntervalSince1970, forKey: "widget.clockedInSince")
            }
        case "clockOut":
            if state.clockedIn {
                d.set(Int(state.attendanceSeconds(at: now)), forKey: "widget.attendanceSecondsBefore")
            }
            d.set(false, forKey: "widget.clockedIn")
            for key in ["widget.clockedInSince", "widget.runningProject", "widget.runningTask", "widget.runningSince"] {
                d.removeObject(forKey: key)
            }
            d.set(-1, forKey: "widget.runningTaskId")
            if let reason {
                d.set(reason, forKey: "widget.pauseReason")
                d.set(now.timeIntervalSince1970, forKey: "widget.pauseSince")
            }
        case "switchTask":
            if !state.clockedIn {
                d.set(true, forKey: "widget.clockedIn")
                d.set(now.timeIntervalSince1970, forKey: "widget.clockedInSince")
            }
            d.set(projectLabel ?? "", forKey: "widget.runningProject")
            d.set(title ?? "", forKey: "widget.runningTask")
            d.set(now.timeIntervalSince1970, forKey: "widget.runningSince")
            d.set(taskId ?? -1, forKey: "widget.runningTaskId")
        default:
            break
        }
        d.synchronize()
        WidgetCenter.shared.reloadAllTimelines()
    }
}

@available(iOS 17.0, *)
struct ClockInIntent: AppIntent {
    static var title: LocalizedStringResource = "Clock in"
    static var description = IntentDescription("Clocks in and continues the last activity of the day.")
    func perform() async throws -> some IntentResult {
        WidgetActionStore.perform("clockIn")
        return .result()
    }
}

@available(iOS 17.0, *)
struct ClockOutIntent: AppIntent {
    static var title: LocalizedStringResource = "Clock out"
    static var description = IntentDescription("Clocks out and stops the running activity.")
    func perform() async throws -> some IntentResult {
        WidgetActionStore.perform("clockOut")
        return .result()
    }
}

@available(iOS 17.0, *)
struct PauseIntent: AppIntent {
    static var title: LocalizedStringResource = "Take a break"
    static var description = IntentDescription("Clocks out for lunch or a short break; clocking in again continues the activity.")

    /// "LUNCH" or "BREAK" (ClockOutReason names).
    @Parameter(title: "Reason") var reason: String

    init() {}
    init(reason: String) { self.reason = reason }

    func perform() async throws -> some IntentResult {
        WidgetActionStore.perform("clockOut", reason: reason)
        return .result()
    }
}

@available(iOS 17.0, *)
struct SwitchTaskIntent: AppIntent {
    static var title: LocalizedStringResource = "Switch task"
    static var description = IntentDescription("Starts working on a task (clocks in if needed).")

    @Parameter(title: "Task id") var taskId: Int
    @Parameter(title: "Project id") var projectId: Int
    @Parameter(title: "Task") var taskTitle: String
    @Parameter(title: "Project") var projectLabel: String

    init() {}
    init(task: QuickTask) {
        taskId = task.taskId
        projectId = task.projectId
        taskTitle = task.title
        projectLabel = task.projectLabel
    }

    func perform() async throws -> some IntentResult {
        WidgetActionStore.perform("switchTask", taskId: taskId, projectId: projectId, title: taskTitle, projectLabel: projectLabel)
        return .result()
    }
}

// MARK: - Timeline

struct WorkTrackerEntry: TimelineEntry {
    let date: Date
    let state: WidgetState
}

struct WorkTrackerProvider: TimelineProvider {
    func placeholder(in context: Context) -> WorkTrackerEntry {
        WorkTrackerEntry(date: Date(), state: .preview)
    }

    func getSnapshot(in context: Context, completion: @escaping (WorkTrackerEntry) -> Void) {
        let now = Date()
        completion(WorkTrackerEntry(date: now, state: context.isPreview ? .preview : WidgetState.load().asOf(now)))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<WorkTrackerEntry>) -> Void) {
        let stored = WidgetState.load()
        let now = Date()
        let calendar = Calendar.current
        let state = stored.asOf(now, calendar: calendar)
        let nextMidnight = calendar.date(byAdding: .day, value: 1, to: calendar.startOfDay(for: now))
            ?? now.addingTimeInterval(24 * 3600)
        var entries = [WorkTrackerEntry(date: now, state: state)]
        // When clocked in, add an entry at the moment the target is reached so the colour flips
        // without the app having to push an update.
        if let start = state.timerStart, state.targetSeconds > 0 {
            let reachedAt = start.addingTimeInterval(state.targetSeconds)
            if reachedAt > now && reachedAt < nextMidnight { entries.append(WorkTrackerEntry(date: reachedAt, state: state)) }
        }
        // Start the new day's total at midnight even if the app is not opened.
        entries.append(WorkTrackerEntry(date: nextMidnight, state: stored.asOf(nextMidnight, calendar: calendar)))
        // The app reloads the timeline on every change; this is only a safety net for a new day.
        completion(Timeline(entries: entries, policy: .after(now.addingTimeInterval(30 * 60))))
    }
}

// MARK: - Formatting

private func hoursMinutes(_ seconds: TimeInterval) -> String {
    let total = Int(max(0, seconds))
    return String(format: "%dh %02dm", total / 3600, (total % 3600) / 60)
}

private func hourMinute(_ date: Date) -> String {
    let f = DateFormatter()
    f.dateFormat = "HH:mm"
    return f.string(from: date)
}

private let green = Color(red: 0.06, green: 0.73, blue: 0.51)
private let red = Color(red: 0.94, green: 0.27, blue: 0.27)
private let amber = Color(red: 0.96, green: 0.62, blue: 0.04)

// MARK: - Views

/// Big number: today's clocked-in total, ticking live while clocked in.
private struct TotalText: View {
    let entry: WorkTrackerEntry
    var body: some View {
        if let start = entry.state.timerStart {
            Text(timerInterval: start...start.addingTimeInterval(48 * 3600), countsDown: false)
                .monospacedDigit()
        } else {
            Text(hoursMinutes(entry.state.attendanceSeconds(at: entry.date))).monospacedDigit()
        }
    }
}

private func pauseSymbol(_ reason: String) -> String {
    reason == "LUNCH" ? "fork.knife" : "cup.and.saucer.fill"
}

private struct StatusLine: View {
    let state: WidgetState
    var body: some View {
        HStack(spacing: 5) {
            if let pause = state.pause {
                // "LUNCH · 12:02", like the Today screen.
                Image(systemName: pauseSymbol(pause.reason)).font(.system(size: 9))
                Text("\(state.text(pause.reason)) · \(hourMinute(pause.since))")
            } else {
                Circle().fill(state.clockedIn ? green : Color.secondary.opacity(0.5)).frame(width: 7, height: 7)
                if state.clockedIn, let since = state.clockedInSince {
                    Text("\(state.text("clockedIn")) · \(hourMinute(since))")
                } else {
                    Text(state.text("clockedOut"))
                }
            }
        }
        .font(.system(size: 10, weight: .bold))
        .foregroundStyle(state.clockedIn ? green : state.pause != nil ? amber : .secondary)
        .lineLimit(1)
    }
}

/// How long the pause has lasted so far, ticking live.
private struct PauseLine: View {
    let state: WidgetState
    var size: CGFloat = 11
    var body: some View {
        if let pause = state.pause {
            HStack(spacing: 4) {
                Image(systemName: "timer").font(.system(size: size - 1, weight: .bold))
                Text(timerInterval: pause.since...pause.since.addingTimeInterval(24 * 3600), countsDown: false)
                    .monospacedDigit()
                    .font(.system(size: size, weight: .bold))
            }
            .foregroundStyle(amber)
            .lineLimit(1)
        }
    }
}

/// Clocks out for lunch / a short break, tagged so the app shows and sums it like its own menu.
@available(iOS 17.0, *)
private struct PauseButton: View {
    let reason: String
    let state: WidgetState
    var compact = true

    var body: some View {
        Button(intent: PauseIntent(reason: reason)) {
            Image(systemName: pauseSymbol(reason))
                .font(.system(size: compact ? 11 : 12, weight: .bold))
                .foregroundStyle(amber)
                .frame(minWidth: compact ? 28 : 0, maxWidth: compact ? nil : CGFloat.infinity, maxHeight: .infinity)
                .padding(.vertical, compact ? 6 : 8)
                .background(amber.opacity(0.18), in: RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(state.text(reason))
    }
}

/// Clock in / out, and while clocked in the two pause buttons beside it.
private struct ClockActions: View {
    let state: WidgetState
    var body: some View {
        HStack(spacing: 4) {
            ClockButton(state: state, compact: true)
            if #available(iOS 17.0, *), state.clockedIn {
                PauseButton(reason: "LUNCH", state: state)
                PauseButton(reason: "BREAK", state: state)
            }
        }
        .fixedSize(horizontal: false, vertical: true)
    }
}

/// Progress towards today's target; live while clocked in.
private struct TargetBar: View {
    let entry: WorkTrackerEntry
    var body: some View {
        let s = entry.state
        if s.targetSeconds > 0 {
            let reached = s.attendanceSeconds(at: entry.date) >= s.targetSeconds
            if let start = s.timerStart, !reached {
                ProgressView(timerInterval: start...start.addingTimeInterval(s.targetSeconds), countsDown: false, label: { EmptyView() }, currentValueLabel: { EmptyView() })
                    .tint(.accentColor)
            } else {
                ProgressView(value: min(1, s.attendanceSeconds(at: entry.date) / s.targetSeconds))
                    .tint(reached ? green : .accentColor)
            }
        }
    }
}

/// Clock in / Clock out. Interactive on iOS 17+, a plain status pill before that.
private struct ClockButton: View {
    let state: WidgetState
    var compact = false

    var body: some View {
        let label = HStack(spacing: 4) {
            Image(systemName: state.clockedIn ? "rectangle.portrait.and.arrow.right" : "arrow.right.to.line")
            Text(state.text(state.clockedIn ? "clockOut" : state.pause != nil ? "resume" : "clockIn"))
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .font(.system(size: compact ? 11 : 12, weight: .bold))
        .foregroundStyle(.white)
        .frame(maxWidth: .infinity)
        .padding(.vertical, compact ? 6 : 8)
        .background(state.clockedIn ? red : green, in: RoundedRectangle(cornerRadius: 10))

        if #available(iOS 17.0, *) {
            if state.clockedIn {
                Button(intent: ClockOutIntent()) { label }.buttonStyle(.plain)
            } else {
                Button(intent: ClockInIntent()) { label }.buttonStyle(.plain)
            }
        } else {
            label
        }
    }
}

/// One tap starts the task; the running one is highlighted.
private struct TaskChip: View {
    let task: QuickTask
    let running: Bool

    var body: some View {
        let label = HStack(spacing: 6) {
            Circle().fill(task.productive ? Color.accentColor : amber).frame(width: 6, height: 6)
            VStack(alignment: .leading, spacing: 0) {
                Text(task.title).font(.system(size: 12, weight: running ? .bold : .semibold)).lineLimit(1)
                Text(task.projectLabel).font(.system(size: 9)).foregroundStyle(.secondary).lineLimit(1)
            }
            Spacer(minLength: 0)
            if running {
                Image(systemName: "play.fill").font(.system(size: 9)).foregroundStyle(green)
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 5)
        .frame(maxWidth: .infinity)
        .background(running ? green.opacity(0.15) : Color.secondary.opacity(0.10), in: RoundedRectangle(cornerRadius: 9))

        if #available(iOS 17.0, *), !running {
            Button(intent: SwitchTaskIntent(task: task)) { label }.buttonStyle(.plain)
        } else {
            label
        }
    }
}

/// What is running right now, in one line: the task, or the project when there is no task.
private struct RunningLine: View {
    let state: WidgetState
    var size: CGFloat = 10

    var body: some View {
        if let text = state.runningTask ?? state.runningProject {
            HStack(spacing: 4) {
                Image(systemName: "play.fill").font(.system(size: size - 3)).foregroundStyle(green)
                Text(text).font(.system(size: size, weight: .semibold)).lineLimit(1)
            }
        }
    }
}

private struct SmallView: View {
    let entry: WorkTrackerEntry
    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            StatusLine(state: entry.state)
            Spacer(minLength: 0)
            TotalText(entry: entry)
                .font(.system(size: 24, weight: .heavy, design: .rounded))
                .minimumScaleFactor(0.6)
                .lineLimit(1)
            if entry.state.targetSeconds > 0 {
                Text(String(format: entry.state.text("ofToday"), hoursMinutes(entry.state.targetSeconds)))
                    .font(.system(size: 10)).foregroundStyle(.secondary)
                TargetBar(entry: entry)
            }
            // The smallest size still has to answer "what am I on?" – on a day without a target
            // (a weekend) this is the only thing between the clock and the button.
            RunningLine(state: entry.state)
            PauseLine(state: entry.state)
            Spacer(minLength: 0)
            ClockActions(state: entry.state)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
    }
}

private struct TaskList: View {
    let state: WidgetState
    let limit: Int
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(state.text(state.clockedIn ? "switchTo" : "tasks")).font(.system(size: 9, weight: .bold)).foregroundStyle(.secondary)
            if !state.clockedIn {
                // Tasks are chosen after clocking in.
                Text(state.text("clockInFirst")).font(.system(size: 11)).foregroundStyle(.secondary)
            } else if state.quickTasks.isEmpty {
                Text(state.text("addTasks")).font(.system(size: 11)).foregroundStyle(.secondary)
            } else {
                ForEach(state.quickTasks.prefix(limit)) { task in
                    TaskChip(task: task, running: task.taskId == state.runningTaskId)
                }
            }
        }
    }
}

private struct MediumView: View {
    let entry: WorkTrackerEntry
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 3) {
                StatusLine(state: entry.state)
                TotalText(entry: entry)
                    .font(.system(size: 24, weight: .heavy, design: .rounded))
                    .minimumScaleFactor(0.6)
                    .lineLimit(1)
                if entry.state.targetSeconds > 0 {
                    Text(String(format: entry.state.text("ofToday"), hoursMinutes(entry.state.targetSeconds)))
                        .font(.system(size: 10)).foregroundStyle(.secondary)
                }
                TargetBar(entry: entry)
                PauseLine(state: entry.state)
                Spacer(minLength: 0)
                ClockActions(state: entry.state)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
            TaskList(state: entry.state, limit: 3)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        }
    }
}

private struct LargeView: View {
    let entry: WorkTrackerEntry
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 3) {
                    StatusLine(state: entry.state)
                    TotalText(entry: entry)
                        .font(.system(size: 30, weight: .heavy, design: .rounded))
                        .minimumScaleFactor(0.6)
                        .lineLimit(1)
                    if entry.state.targetSeconds > 0 {
                        Text(String(format: entry.state.text("ofToday"), hoursMinutes(entry.state.targetSeconds)))
                            .font(.system(size: 11)).foregroundStyle(.secondary)
                    }
                    TargetBar(entry: entry)
                    PauseLine(state: entry.state, size: 13)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                VStack(spacing: 6) {
                    ClockButton(state: entry.state)
                    if #available(iOS 17.0, *), entry.state.clockedIn {
                        HStack(spacing: 6) {
                            PauseButton(reason: "LUNCH", state: entry.state, compact: false)
                            PauseButton(reason: "BREAK", state: entry.state, compact: false)
                        }
                        .fixedSize(horizontal: false, vertical: true)
                    }
                }
                .frame(width: 120)
            }
            if let project = entry.state.runningProject {
                HStack(spacing: 4) {
                    Text(entry.state.text("now")).font(.system(size: 9, weight: .bold)).foregroundStyle(.secondary)
                    Text(project).font(.system(size: 12, weight: .bold)).lineLimit(1)
                    if let task = entry.state.runningTask {
                        Text("· \(task)").font(.system(size: 12)).foregroundStyle(.secondary).lineLimit(1)
                    }
                    Spacer(minLength: 0)
                    if let since = entry.state.runningSince {
                        Text(timerInterval: since...since.addingTimeInterval(48 * 3600), countsDown: false)
                            .monospacedDigit().font(.system(size: 11, weight: .semibold)).foregroundStyle(.secondary)
                    }
                }
            }
            TaskList(state: entry.state, limit: 6)
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}

/// Clock in / out on the lock screen: no colour to spend there, so it is a symbol in a capsule.
private struct AccessoryClockButton: View {
    let state: WidgetState
    var body: some View {
        let label = Image(systemName: state.clockedIn ? "pause.circle.fill" : "play.circle.fill")
            .font(.system(size: 20))
        if state.clockedIn {
            Button(intent: ClockOutIntent()) { label }.buttonStyle(.plain)
        } else {
            Button(intent: ClockInIntent()) { label }.buttonStyle(.plain)
        }
    }
}

private struct RectangularView: View {
    let entry: WorkTrackerEntry
    var body: some View {
        HStack(spacing: 6) {
            VStack(alignment: .leading, spacing: 2) {
                StatusLine(state: entry.state)
                TotalText(entry: entry).font(.system(size: 20, weight: .heavy, design: .rounded))
                if entry.state.pause != nil {
                    PauseLine(state: entry.state)
                } else if let project = entry.state.runningProject {
                    Text(project).font(.system(size: 11)).lineLimit(1)
                }
            }
            Spacer(minLength: 0)
            // Clocking in and out has to work in every size, this one included.
            AccessoryClockButton(state: entry.state)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct CircularView: View {
    let entry: WorkTrackerEntry
    var body: some View {
        let s = entry.state
        let progress = s.targetSeconds > 0 ? min(1, s.attendanceSeconds(at: entry.date) / s.targetSeconds) : 0
        let gauge = Gauge(value: progress) {
            Image(systemName: s.clockedIn ? "pause.fill" : "play.fill")
        } currentValueLabel: {
            Text(shortHours(s.attendanceSeconds(at: entry.date))).font(.system(size: 12, weight: .bold))
        }
        .gaugeStyle(.accessoryCircular)

        // The whole gauge is the clock in / out button – there is no room for a separate one, and
        // every size has to be able to do it.
        if s.clockedIn {
            Button(intent: ClockOutIntent()) { gauge }.buttonStyle(.plain)
        } else {
            Button(intent: ClockInIntent()) { gauge }.buttonStyle(.plain)
        }
    }

    private func shortHours(_ seconds: TimeInterval) -> String {
        let h = Int(seconds) / 3600
        let m = (Int(seconds) % 3600) / 60
        return h > 0 ? "\(h):\(String(format: "%02d", m))" : "\(m)m"
    }
}

struct WorkTrackerWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: WorkTrackerEntry

    var body: some View {
        Group {
            switch family {
            case .systemMedium: MediumView(entry: entry)
            case .systemLarge: LargeView(entry: entry)
            case .accessoryRectangular: RectangularView(entry: entry)
            case .accessoryCircular: CircularView(entry: entry)
            default: SmallView(entry: entry)
            }
        }
        .widgetBackground()
    }
}

private extension View {
    @ViewBuilder
    func widgetBackground() -> some View {
        if #available(iOS 17.0, *) {
            containerBackground(for: .widget) { Color(.systemBackground) }
        } else {
            padding().background(Color(.systemBackground))
        }
    }
}

// MARK: - Widget

struct WorkTrackerWidget: Widget {
    let kind = "WorkTrackerWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: WorkTrackerProvider()) { entry in
            WorkTrackerWidgetView(entry: entry)
        }
        .configurationDisplayName("WorkTracker")
        .description("Clock in, clock out and switch tasks; clocked-in time today.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge, .accessoryRectangular, .accessoryCircular])
    }
}

@main
struct WorkTrackerWidgetBundle: WidgetBundle {
    var body: some Widget {
        WorkTrackerWidget()
    }
}
