package com.suw1labs.worktracker.ui.shell

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * One entry of the app's navigation, in the terms every shell needs: [icon] for the ones drawing
 * with Compose, [systemImage] (an SF Symbol) for the ones drawing with UIKit.
 */
data class ShellTab(
    val label: String,
    val icon: ImageVector,
    val systemImage: String,
    val selected: Boolean,
    val testTag: String,
    val badgeCount: Int = 0,
    val onClick: () -> Unit,
)

/** Everything the chrome shows. Screens never see this; they only get the space that is left. */
data class ShellState(
    val title: String,
    val status: String?,
    val alertCount: Int,
    val onAlertClick: () -> Unit,
    val tabs: List<ShellTab>,
    val snackbarHostState: SnackbarHostState,
)

/**
 * The chrome around the screens: title bar, navigation, snackbars.
 *
 * Platform design languages change – Material 3 became expressive, iOS 26 became Liquid Glass –
 * and they will change again. Everything that follows such a fashion lives in an implementation of
 * this interface, so a new look is a new [AppShell] and not an edit through the whole app:
 * navigation state, screens and view models know nothing about it.
 *
 * @see MaterialShell the Material 3 Expressive chrome (Android, desktop)
 * @see FloatingShell bars floating over the content, drawn with Compose on any platform
 * @see platformAppShell which one a platform picks by default
 */
interface AppShell {
    /**
     * Draws the chrome and calls [content] with the modifier the screen should use – which is how
     * a shell says whether the screen is laid out between the bars or fills the window and passes
     * underneath them (then it provides
     * [com.suw1labs.worktracker.ui.components.LocalScreenInsets] instead).
     *
     * @param wide true for tablet and desktop windows, where navigation belongs at the side.
     */
    @Composable
    fun Chrome(state: ShellState, wide: Boolean, content: @Composable (Modifier) -> Unit)
}

/**
 * The shell in use. Override it to try another design, to pin one platform's look in a screenshot
 * test, or to ship a setting that lets people choose:
 * `CompositionLocalProvider(LocalAppShell provides FloatingShell()) { ... }`.
 */
val LocalAppShell = staticCompositionLocalOf { platformAppShell() }
