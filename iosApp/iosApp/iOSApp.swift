import SwiftUI
import WidgetKit
import ComposeApp

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
        // The shared Kotlin code writes the widget state into the App Group; WidgetKit itself can
        // only be poked from Swift, so hand it a reload callback.
        WidgetReload.shared.handler = { WidgetCenter.shared.reloadAllTimelines() }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .onChange(of: scenePhase) { oldPhase, newPhase in
            // Widget buttons queue their actions in the App Group; replay them as soon as we are active.
            if newPhase == .active { WidgetActionsBridgeKt.applyPendingWidgetActions() }
        }
    }
}
