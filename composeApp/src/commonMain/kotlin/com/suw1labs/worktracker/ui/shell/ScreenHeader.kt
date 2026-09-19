package com.suw1labs.worktracker.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suw1labs.worktracker.ui.theme.RoseUrgent

/**
 * Title of the current screen, for shells that have no title bar to put it in.
 *
 * A bar would say "Projects" right above a tab that already says "Projects" – twice the words and
 * a strip of chrome for it. Instead the shell hands the title down and the screen prints it as its
 * first row, so it scrolls away with the content like any heading.
 */
data class ScreenHeader(
    val title: String,
    val status: String?,
    val alertCount: Int,
    val onAlertClick: () -> Unit,
)

/** Null where the chrome shows the title itself (the Material shells and their `TopAppBar`). */
val LocalScreenHeader = staticCompositionLocalOf<ScreenHeader?> { null }

/** Put this first in a screen's scrolling content; it draws nothing when a title bar exists. */
@Composable
fun InlineScreenTitle(modifier: Modifier = Modifier) {
    val header = LocalScreenHeader.current ?: return
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = header.title,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (header.status != null) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = header.status,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        if (header.alertCount > 0) {
            IconButton(onClick = header.onAlertClick, modifier = Modifier.testTag("top_deadline_alerts_button")) {
                BadgedBox(badge = { Badge(containerColor = RoseUrgent) { Text(header.alertCount.toString()) } }) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = RoseUrgent,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
