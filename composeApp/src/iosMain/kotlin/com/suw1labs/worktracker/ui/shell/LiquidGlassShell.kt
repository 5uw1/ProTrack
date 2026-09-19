package com.suw1labs.worktracker.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.suw1labs.worktracker.ui.components.LocalModalPresence
import com.suw1labs.worktracker.ui.components.LocalScreenInsets

/** Height of the title bar below the status bar, and of the tab bar including its margins. */
private val TitleBarHeight = 44.dp
private val TabBarHeight = 74.dp

/**
 * The chrome iOS 26 draws: both bars are Liquid Glass floating over the content, and the content
 * fills the window and scrolls underneath them. Screens keep the bars' height free through
 * [LocalScreenInsets], so nothing ends up stuck behind the glass.
 *
 * Wide windows (iPad) still get [MaterialShell]; a glass sidebar is a separate piece of work.
 */
class LiquidGlassShell : AppShell {

    @Composable
    override fun Chrome(state: ShellState, wide: Boolean, content: @Composable (Modifier) -> Unit) {
        if (wide) {
            MaterialShell().Chrome(state, wide, content)
            return
        }

        // A dialog's scrim covers the Compose canvas but not native overlays, so the bars step
        // aside while one is open instead of sitting bright and out of reach over it.
        val hidden = LocalModalPresence.current.isModalOpen

        // The title row rides with the content: gone on the way down, back on the way up. Only a
        // threshold of a few pixels, or a shaky finger would flicker it.
        var titleShown by remember { mutableStateOf(true) }
        val scroll = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y < -3f) titleShown = false
                    else if (available.y > 3f) titleShown = true
                    return Offset.Zero
                }
            }
        }
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TitleBarHeight
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + TabBarHeight

        // Surface, because without Scaffold nothing else paints the window background or sets the
        // content colour that text and icons inherit.
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize().nestedScroll(scroll)) {
                CompositionLocalProvider(
                    LocalScreenInsets provides PaddingValues(top = topInset, bottom = bottomInset)
                ) {
                    content(Modifier.fillMaxSize())
                }
                GlassTopBar(
                    title = state.title,
                    status = state.status,
                    alertCount = state.alertCount,
                    onAlertClick = state.onAlertClick,
                    titleShown = titleShown,
                    hidden = hidden,
                    modifier = Modifier.align(Alignment.TopCenter).testTag("top_deadline_alerts_button"),
                )
                GlassTabBar(
                    tabs = state.tabs,
                    hidden = hidden,
                    modifier = Modifier.align(Alignment.BottomCenter).testTag("mobile_bottom_nav_bar"),
                )
                SnackbarHost(
                    state.snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottomInset),
                )
            }
        }
    }
}
