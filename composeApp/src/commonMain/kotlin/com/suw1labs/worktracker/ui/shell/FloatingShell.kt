package com.suw1labs.worktracker.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.suw1labs.worktracker.ui.components.FloatingTabBar

/**
 * Bars floating above the bottom edge, drawn with Compose on every platform. Same layout as the
 * native iOS chrome but without real glass, so it is the fallback if the interop bar ever has to
 * go – and the way to see that look on Android or desktop.
 */
class FloatingShell : AppShell {

    @Composable
    override fun Chrome(state: ShellState, wide: Boolean, content: @Composable (Modifier) -> Unit) {
        if (wide) {
            MaterialShell().Chrome(state, wide, content)
        } else {
            MaterialScaffold(
                state = state,
                showTitleBadges = true,
                bottomBar = { FloatingTabBar(state.tabs, Modifier.testTag("mobile_bottom_nav_bar")) },
                content = content,
            )
        }
    }
}
