package com.suw1labs.worktracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * How many dialogs are open. A shell whose bars are drawn over the content – the iOS glass ones
 * are native views above the Compose canvas – needs this: the dialog's scrim covers the canvas but
 * not them, so they would sit bright and untouched over a modal.
 */
class ModalPresence {
    var openCount by mutableStateOf(0)
        private set

    val isModalOpen: Boolean get() = openCount > 0

    fun opened() { openCount++ }

    fun closed() { openCount-- }
}

val LocalModalPresence = staticCompositionLocalOf { ModalPresence() }

/** Call inside a dialog's content so the chrome knows it is covered for as long as it is shown. */
@Composable
fun TrackModal() {
    val presence = LocalModalPresence.current
    DisposableEffect(presence) {
        presence.opened()
        onDispose { presence.closed() }
    }
}
