package com.suw1labs.worktracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suw1labs.worktracker.ui.shell.ShellTab
import com.suw1labs.worktracker.ui.theme.RoseUrgent


/**
 * Tab bar as a rounded bar floating above the bottom edge, the way iOS has drawn tab bars since
 * iOS 26. The selected tab sits in a tinted capsule instead of the Material indicator pill.
 * Android and desktop keep the platform's own navigation bar / rail.
 */
@Composable
fun FloatingTabBar(tabs: List<ShellTab>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = scheme.surfaceVariant.copy(alpha = 0.94f),
            contentColor = scheme.onSurface,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, scheme.onSurface.copy(alpha = 0.08f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { tab -> FloatingTab(tab) }
            }
        }
    }
}

@Composable
private fun RowScope.FloatingTab(item: ShellTab) {
    val scheme = MaterialTheme.colorScheme
    val background by animateColorAsState(
        if (item.selected) scheme.primary.copy(alpha = 0.16f) else Color.Transparent
    )
    val tint by animateColorAsState(
        if (item.selected) scheme.primary else scheme.onSurfaceVariant
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .clickable(onClick = item.onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag(item.testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (item.badgeCount > 0) {
            BadgedBox(
                badge = { Badge(containerColor = RoseUrgent) { Text(item.badgeCount.toString()) } },
            ) {
                Icon(item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.size(24.dp))
            }
        } else {
            Icon(item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.size(24.dp))
        }
        Text(
            text = item.label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
