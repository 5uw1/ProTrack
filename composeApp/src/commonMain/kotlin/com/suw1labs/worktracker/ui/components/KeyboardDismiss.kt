package com.suw1labs.worktracker.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

/**
 * Clears text-field focus (and therefore hides the software keyboard) when the user taps
 * anywhere that does not handle the tap itself, e.g. a card background or a label.
 */
@Composable
fun Modifier.dismissKeyboardOnTap(): Modifier {
    val focusManager = LocalFocusManager.current
    return pointerInput(focusManager) {
        detectTapGestures(onTap = { focusManager.clearFocus() })
    }
}

/** True on phones and tablets, where a focused text field brings up an on-screen keyboard. */
expect val platformHasSoftKeyboard: Boolean

/**
 * Hides the on-screen keyboard as soon as the user starts scrolling a list. Desktop has none, and
 * clearing focus there would only take the keyboard away from what the user is working in (the
 * timesheet calendar loses its arrow keys at every turn of the mouse wheel).
 */
@Composable
fun Modifier.dismissKeyboardOnScroll(): Modifier {
    if (!platformHasSoftKeyboard) return this
    val focusManager = LocalFocusManager.current
    val connection = remember(focusManager) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) focusManager.clearFocus()
                return Offset.Zero
            }
        }
    }
    return nestedScroll(connection)
}
