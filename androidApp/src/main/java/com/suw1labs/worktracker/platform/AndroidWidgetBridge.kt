package com.suw1labs.worktracker.platform

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.suw1labs.worktracker.widget.WorkTrackerWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Re-renders every placed home-screen widget when the shared data changes. The widget reads its
 * state from the database itself, so the snapshot is not persisted here.
 */
class AndroidWidgetBridge(private val context: Context) : WidgetBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun publish(snapshot: WidgetSnapshot) {
        scope.launch { runCatching { WorkTrackerWidget().updateAll(context) } }
    }
}
