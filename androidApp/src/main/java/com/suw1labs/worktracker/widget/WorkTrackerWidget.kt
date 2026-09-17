package com.suw1labs.worktracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.suw1labs.worktracker.MainActivity
import com.suw1labs.worktracker.WorkTrackerApplication
import com.suw1labs.worktracker.platform.QuickTask
import com.suw1labs.worktracker.platform.WidgetSnapshot
import com.suw1labs.worktracker.platform.WidgetSnapshots
import com.suw1labs.worktracker.ui.i18n.AppStrings
import com.suw1labs.worktracker.ui.i18n.Language
import com.suw1labs.worktracker.ui.i18n.Translations
import com.suw1labs.worktracker.util.DateFormats
import com.suw1labs.worktracker.util.TimeFormat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Home-screen widget: clocked-in total of today, the running activity and a Clock in / Clock out
 * button. The state comes straight from the shared database; the app re-renders the widget on
 * every change (see AndroidWidgetBridge) and Android refreshes it every 30 minutes on its own.
 */
class WorkTrackerWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(COMPACT, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = WorkTrackerApplication.container(context)
        val repository = container.repository
        val initialSnapshot = container.widgetSnapshot()
        val initialLanguage = repository.settings.first()?.language
        provideContent {
            // Glance keeps this composition alive between updates, so the state must be observed
            // here: every database change (from the app or from the widget's own buttons) re-renders.
            val snapshot by WidgetSnapshots.flow(repository).collectAsState(initialSnapshot)
            val language by repository.settings.map { it?.language }.collectAsState(initialLanguage)
            val strings = Translations.forLanguage(Language.fromCode(language))
            GlanceTheme { WidgetContent(snapshot = snapshot, now = System.currentTimeMillis(), t = strings) }
        }
    }

    companion object {
        val COMPACT = DpSize(140.dp, 100.dp)
        val WIDE = DpSize(250.dp, 100.dp)
    }
}

class WorkTrackerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WorkTrackerWidget()
}

/** Clock in from the widget; continues the last activity of the day like the in-app button. */
class ClockInAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WorkTrackerApplication.container(context).repository.clockIn(System.currentTimeMillis())
        WorkTrackerWidget().update(context, glanceId)
    }
}

class ClockOutAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WorkTrackerApplication.container(context).repository.clockOut(System.currentTimeMillis())
        WorkTrackerWidget().update(context, glanceId)
    }
}

/** Starts the tapped task (clocking in first if needed), exactly like "Track" in the app. */
class SwitchTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TaskId] ?: return
        val projectId = parameters[ProjectId] ?: return
        WorkTrackerApplication.container(context).repository.startActivity(projectId, taskId, "", System.currentTimeMillis())
        WorkTrackerWidget().update(context, glanceId)
    }

    companion object {
        val TaskId = ActionParameters.Key<Long>("taskId")
        val ProjectId = ActionParameters.Key<Long>("projectId")
    }
}

private val Green = Color(0xFF10B981)
private val Red = Color(0xFFEF4444)
private val Amber = Color(0xFFF59E0B)

@Composable
private fun WidgetContent(snapshot: WidgetSnapshot, now: Long, t: AppStrings) {
    val wide = LocalSize.current.width >= WorkTrackerWidget.WIDE.width
    val statusColor = if (snapshot.clockedIn) ColorProvider(Green) else GlanceTheme.colors.onSurfaceVariant
    val muted = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
            Box(modifier = GlanceModifier.size(8.dp).cornerRadius(4.dp).background(statusColor)) {}
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = if (snapshot.clockedIn) t.clockedInSince(DateFormats.hourMinute(snapshot.clockedInSince ?: now)) else t.clockedOut,
                style = TextStyle(color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom, modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = TimeFormat.hoursMinutes(snapshot.attendanceSecondsAt(now)),
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            if (wide && snapshot.targetSeconds > 0) {
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(text = "/ ${TimeFormat.hoursMinutes(snapshot.targetSeconds)}", style = muted, maxLines = 1)
            }
        }
        if (wide) {
            // Running activity, or a hint when nothing runs.
            Text(
                text = snapshot.runningProject?.let { p -> snapshot.runningTask?.let { "$p · $it" } ?: p }
                    ?: (if (snapshot.clockedIn) t.whatWorkingOn else t.clockedInToday),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                maxLines = 1
            )
        }
        if (snapshot.quickTasks.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.height(8.dp))
            QuickTaskChips(tasks = snapshot.quickTasks.take(if (wide) 3 else 2), runningTaskId = snapshot.runningTaskId)
        }
        Spacer(modifier = GlanceModifier.defaultWeight())
        if (snapshot.clockedIn) {
            Button(
                text = t.clockOut,
                onClick = actionRunCallback<ClockOutAction>(),
                colors = ButtonDefaults.buttonColors(backgroundColor = ColorProvider(Red), contentColor = ColorProvider(Color.White)),
                modifier = GlanceModifier.fillMaxWidth()
            )
        } else {
            Button(
                text = t.clockIn,
                onClick = actionRunCallback<ClockInAction>(),
                colors = ButtonDefaults.buttonColors(backgroundColor = ColorProvider(Green), contentColor = ColorProvider(Color.White)),
                modifier = GlanceModifier.fillMaxWidth()
            )
        }
    }
}

/** One-tap task switches; the running task is highlighted and not clickable. */
@Composable
private fun QuickTaskChips(tasks: List<QuickTask>, runningTaskId: Long?) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        tasks.forEachIndexed { index, task ->
            if (index > 0) Spacer(modifier = GlanceModifier.width(6.dp))
            val running = task.taskId == runningTaskId
            val accent = if (task.productive) GlanceTheme.colors.primary else ColorProvider(Amber)
            val chip = GlanceModifier
                .defaultWeight()
                .background(if (running) ColorProvider(Green.copy(alpha = 0.18f)) else GlanceTheme.colors.surfaceVariant)
                .cornerRadius(10.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .let { m ->
                    if (running) m else m.clickable(
                        actionRunCallback<SwitchTaskAction>(
                            actionParametersOf(SwitchTaskAction.TaskId to task.taskId, SwitchTaskAction.ProjectId to task.projectId)
                        )
                    )
                }
            Column(modifier = chip) {
                Text(
                    text = (if (running) "▶ " else "") + task.title,
                    style = TextStyle(color = if (running) ColorProvider(Green) else GlanceTheme.colors.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Text(
                    text = task.projectLabel,
                    style = TextStyle(color = if (task.productive) GlanceTheme.colors.onSurfaceVariant else accent, fontSize = 9.sp),
                    maxLines = 1
                )
            }
        }
    }
}
