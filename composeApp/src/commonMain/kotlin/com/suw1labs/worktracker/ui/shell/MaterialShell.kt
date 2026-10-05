package com.suw1labs.worktracker.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suw1labs.worktracker.ui.components.WideLayout
import com.suw1labs.worktracker.ui.components.AppMark
import androidx.compose.ui.unit.sp
import com.suw1labs.worktracker.ui.theme.RoseUrgent

/**
 * Material 3 Expressive chrome: a short navigation bar on phones, a wide navigation rail on
 * tablets and desktop, content laid out between the bars. The default on Android and desktop.
 */
/** Width of the collapsed [WideNavigationRail] (Material 3 Expressive: 96 dp). */
private val RailWidth = 96.dp

class MaterialShell : AppShell {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Chrome(state: ShellState, wide: Boolean, content: @Composable (Modifier) -> Unit) {
        if (wide) {
            Row(modifier = Modifier.fillMaxSize()) {
                WideNavigationRail(
                    modifier = Modifier.testTag("desktop_navigation_rail"),
                    // App mark: the app icon itself (the rail is too narrow for the full name),
                    // centred over the tabs.
                    header = {
                        // The header is not stretched to the rail, so it gets the collapsed rail's width
                        // itself (fillMaxWidth would measure to nothing here).
                        Box(modifier = Modifier.width(RailWidth).padding(top = 12.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                            AppMark(size = 36.dp, modifier = Modifier.testTag("app_mark"))
                        }
                    },
                ) {

                    state.tabs.forEach { tab ->
                        WideNavigationRailItem(
                            railExpanded = false,
                            selected = tab.selected,
                            onClick = tab.onClick,
                            icon = { TabIcon(tab) },
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag(tab.testTag),
                        )
                    }
                }

                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    // Content is capped and centred (screens go two-column once there is room for
                    // it); the title lines up with the content's left edge, not the window's.
                    val contentWidth = if (state.fullWidth) maxWidth else WideLayout.contentWidth(maxWidth)
                    val titleInset = ((maxWidth - contentWidth) / 2).coerceAtLeast(0.dp)
                    MaterialScaffold(state, showTitleBadges = false, titleInset = titleInset) { modifier ->
                        Box(modifier = Modifier.fillMaxSize().then(modifier), contentAlignment = Alignment.TopCenter) {
                            content(Modifier.width(contentWidth).fillMaxHeight())
                        }
                    }
                }
            }
        } else {
            MaterialScaffold(state, showTitleBadges = true, bottomBar = {
                ShortNavigationBar(modifier = Modifier.testTag("mobile_bottom_nav_bar")) {
                    state.tabs.forEach { tab ->
                        ShortNavigationBarItem(
                            selected = tab.selected,
                            onClick = tab.onClick,
                            icon = { TabIcon(tab) },
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag(tab.testTag),
                        )
                    }
                }
            }, content = content)
        }
    }

}

/**
 * Material title bar with the content below it, and whatever navigation the shell passes in. Every
 * Compose-drawn shell shares this; only the bars around it differ.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MaterialScaffold(
    state: ShellState,
    showTitleBadges: Boolean,
    titleInset: Dp = 0.dp,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (Modifier) -> Unit,
) {
        // The compact title bar slides away while scrolling down and comes back on scroll up.
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            snackbarHost = { SnackbarHost(state.snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = titleInset)) {
                            Text(
                                text = state.title,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (showTitleBadges && state.status != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = MaterialTheme.shapes.small,
                                ) {
                                    Text(
                                        text = state.status,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        if (showTitleBadges && state.alertCount > 0) {
                            IconButton(
                                onClick = state.onAlertClick,
                                modifier = Modifier.testTag("top_deadline_alerts_button"),
                            ) {
                                BadgedBox(badge = { Badge(containerColor = RoseUrgent) { Text(state.alertCount.toString()) } }) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = RoseUrgent,
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = bottomBar,
    ) { innerPadding ->
        content(Modifier.padding(innerPadding))
    }
}

@Composable
internal fun TabIcon(tab: ShellTab) {
    if (tab.badgeCount > 0) {
        BadgedBox(badge = { Badge(containerColor = RoseUrgent) { Text(tab.badgeCount.toString()) } }) {
            Icon(tab.icon, contentDescription = tab.label)
        }
    } else {
        Icon(tab.icon, contentDescription = tab.label)
    }
}
