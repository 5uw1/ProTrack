package com.suw1labs.worktracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Title bar for platforms that draw bars as glass over the content. iOS renders it as a real
 * `UIGlassEffect` bar spanning the status bar, so scrolled content blurs through it (the iOS 26
 * scroll edge effect); other platforms keep the Material `TopAppBar` in `Scaffold` and never call
 * this.
 *
 * @param status short badge next to the title ("Working"), or null.
 * @param alertCount deadline alerts behind the bell button; 0 hides the button.
 */
@Composable
expect fun GlassTopBar(
    title: String,
    status: String?,
    alertCount: Int,
    onAlertClick: () -> Unit,
    modifier: Modifier,
)
