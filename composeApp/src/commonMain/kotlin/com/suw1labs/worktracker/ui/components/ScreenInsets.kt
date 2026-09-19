package com.suw1labs.worktracker.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Space a screen has to keep free at the top and bottom because bars float above it.
 *
 * On iOS the glass bars are drawn over the content – that is the point of Liquid Glass, the
 * content has to be visible through them – so a screen cannot simply be laid out between them.
 * Instead every scrolling screen adds these as `contentPadding`: content scrolls underneath the
 * glass, and the first and last item still come to rest in the clear. Zero everywhere else, where
 * `Scaffold` reserves the space for its bars as usual.
 */
val LocalScreenInsets = staticCompositionLocalOf { PaddingValues(0.dp) }
