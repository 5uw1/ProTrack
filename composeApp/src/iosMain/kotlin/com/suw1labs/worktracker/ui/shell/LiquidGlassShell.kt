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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.suw1labs.worktracker.ui.components.LocalModalPresence
import com.suw1labs.worktracker.ui.components.LocalScreenInsets

/** Height of the tab bar including the margins it floats in. */
private val TabBarHeight = 74.dp

/**
 * The chrome iOS 26 draws, and no more than that: one Liquid Glass tab bar floating over the
 * content, and glass over the status bar so scrolled content blurs out under the clock.
 *
 * There is deliberately no title bar. The tab already names the screen, so a bar would repeat it
 * and cost a strip of the window; the title goes into the content as a heading instead
 * ([ScreenHeader]) and scrolls away with it.
 *
 * Content fills the window and passes under both, keeping their height free through
 * [LocalScreenInsets]. Wide windows (iPad) still get [MaterialShell].
 */
class LiquidGlassShell : AppShell {

    @Composable
    override fun Chrome(state: ShellState, wide: Boolean, content: @Composable (Modifier) -> Unit) {
        if (wide) {
            MaterialShell().Chrome(state, wide, content)
            return
        }

        // A dialog's scrim covers the Compose canvas but not native overlays, so the glass steps
        // aside while one is open instead of sitting bright over it.
        val hidden = LocalModalPresence.current.isModalOpen
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + TabBarHeight

        // Surface, because without Scaffold nothing else paints the window background or sets the
        // content colour that text and icons inherit.
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize()) {
                CompositionLocalProvider(
                    LocalScreenInsets provides PaddingValues(top = topInset, bottom = bottomInset),
                    LocalScreenHeader provides ScreenHeader(
                        title = state.title,
                        status = state.status,
                        alertCount = state.alertCount,
                        onAlertClick = state.onAlertClick,
                    ),
                ) {
                    content(Modifier.fillMaxSize())
                }
                GlassStatusStrip(hidden = hidden, modifier = Modifier.align(Alignment.TopCenter))
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
