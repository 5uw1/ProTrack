package com.suw1labs.worktracker.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIColor
import platform.UIKit.UIGlassEffect
import platform.UIKit.UIGlassEffectStyle
import platform.UIKit.UIView
import platform.UIKit.UIVisualEffectView

/**
 * Glass over the status bar and nothing else: no title, no buttons, nothing to tap.
 *
 * It is what is left of the title bar – the part that earns its place. Content scrolling past the
 * top edge blurs out under the clock instead of running into it sharp (the iOS 26 scroll edge
 * effect), while the title itself scrolls with the content as an ordinary heading.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
@Composable
internal fun GlassStatusStrip(hidden: Boolean, modifier: Modifier) {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // Barely tinted: the point of this strip is to let content show through, blurred – tint it
    // any harder and it reads as an opaque bar again.
    val tint = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f).toUIColor()
    val strip = remember { StatusStripView() }

    Box(modifier = modifier.fillMaxWidth().height(statusBar)) {
        UIKitView(
            factory = { strip.build(tint) },
            modifier = Modifier.fillMaxWidth().height(statusBar),
            update = { strip.update(hidden) },
            // Nothing to interact with: taps belong to the content scrolling underneath.
            properties = UIKitInteropProperties(interactionMode = null, placedAsOverlay = true),
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private class StatusStripView {
    private var root: UIVisualEffectView? = null

    fun build(tint: UIColor): UIVisualEffectView {
        val effect = UIGlassEffect.effectWithStyle(UIGlassEffectStyle.UIGlassEffectStyleRegular)
        effect.setTintColor(tint)
        val view = UIVisualEffectView(effect = effect)
        root = view
        return view
    }

    fun update(hidden: Boolean) {
        root?.let { view -> UIView.animateWithDuration(0.2) { view.alpha = if (hidden) 0.0 else 1.0 } }
    }
}

private fun Color.toUIColor(): UIColor =
    UIColor(red = red.toDouble(), green = green.toDouble(), blue = blue.toDouble(), alpha = alpha.toDouble())
