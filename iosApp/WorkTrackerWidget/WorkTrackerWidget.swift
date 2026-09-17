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

/// Small table of the widget's own texts in the three app languages.
enum WidgetTexts {
    private static let table: [String: [String: String]] = [
        "clockedIn": ["en": "CLOCKED IN", "de": "EINGESTEMPELT", "fr": "POINTÉ"],
        "clockedOut": ["en": "CLOCKED OUT", "de": "AUSGESTEMPELT", "fr": "DÉPOINTÉ"],
        "ofToday": ["en": "of %@ today", "de": "von %@ heute", "fr": "sur %@ aujourd'hui"],
        "clockIn": ["en": "Clock in", "de": "Einstempeln", "fr": "Pointer"],
        "clockOut": ["en": "Clock out", "de": "Ausstempeln", "fr": "Dépointer"],
        "switchTo": ["en": "SWITCH TO", "de": "WECHSELN ZU", "fr": "PASSER À"],
        "tasks": ["en": "TASKS", "de": "AUFGABEN", "fr": "TÂCHES"],
        "clockInFirst": ["en": "Clock in first, then pick a task.", "de": "Zuerst einstempeln, dann Aufgabe wählen.", "fr": "Pointe d'abord, puis choisis une tâche."],
        "addTasks": ["en": "Add tasks in the app", "de": "Aufgaben in der App anlegen", "fr": "Ajoute des tâches dans l'app"],
        "now": ["en": "NOW", "de": "JETZT", "fr": "EN COURS"],
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
    static func perform(_ type: String, taskId: Int? = nil, projectId: Int? = nil, title: String? = nil, projectLabel: String? = nil) {
        guard let d = WidgetState.defaults else { return }
        let now = Date()
        var queue = d.array(forKey: "widget.pendingActions") as? [[String: Any]] ?? []
        var record: [String: Any] = ["type": type, "at": now.timeIntervalSince1970]
        if let taskId { record["taskId"] = taskId }
        if let projectId { record["projectId"] = projectId }
        queue.append(record)
        d.set(queue, forKey: "widget.pendingActions")

        // Optimistic state so the widget reflects the tap immediately.
        let state = WidgetState.load()
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
        completion(WorkTrackerEntry(date: Date(), state: context.isPreview ? .preview : WidgetState.load()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<WorkTrackerEntry>) -> Void) {
        let state = WidgetState.load()
        let now = Date()
        var entries = [WorkTrackerEntry(date: now, state: state)]
        // When clocked in, add an entry at the moment the target is reached so the colour flips
        // without the app having to push an update.
        if let start = state.timerStart, state.targetSeconds > 0 {
            let reachedAt = start.addingTimeInterval(state.targetSeconds)
            if reachedAt > now { entries.append(WorkTrackerEntry(date: reachedAt, state: state)) }
        }
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

private struct StatusLine: View {
    let state: WidgetState
    var body: some View {
        HStack(spacing: 5) {
            Circle().fill(state.clockedIn ? green : Color.secondary.opacity(0.5)).frame(width: 7, height: 7)
            if state.clockedIn, let since = state.clockedInSince {
                Text("\(state.text("clockedIn")) · \(hourMinute(since))")
            } else {
                Text(state.text("clockedOut"))
            }
        }
        .font(.system(size: 10, weight: .bold))
        .foregroundStyle(state.clockedIn ? green : .secondary)
        .lineLimit(1)
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
            Text(state.text(state.clockedIn ? "clockOut" : "clockIn"))
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
            }
            TargetBar(entry: entry)
            Spacer(minLength: 0)
            ClockButton(state: entry.state, compact: true)
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
                Spacer(minLength: 0)
                ClockButton(state: entry.state, compact: true)
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
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                ClockButton(state: entry.state).frame(width: 120)
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

private struct RectangularView: View {
    let entry: WorkTrackerEntry
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            StatusLine(state: entry.state)
            TotalText(entry: entry).font(.system(size: 20, weight: .heavy, design: .rounded))
            if let project = entry.state.runningProject {
                Text(project).font(.system(size: 11)).lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct CircularView: View {
    let entry: WorkTrackerEntry
    var body: some View {
        let s = entry.state
        let progress = s.targetSeconds > 0 ? min(1, s.attendanceSeconds(at: entry.date) / s.targetSeconds) : 0
        Gauge(value: progress) {
            Image(systemName: "timer")
        } currentValueLabel: {
            Text(shortHours(s.attendanceSeconds(at: entry.date))).font(.system(size: 12, weight: .bold))
        }
        .gaugeStyle(.accessoryCircular)
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
