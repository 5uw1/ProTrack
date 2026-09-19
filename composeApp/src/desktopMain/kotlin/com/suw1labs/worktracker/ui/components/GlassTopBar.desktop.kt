package com.suw1labs.worktracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Not used here: the platform keeps the Material `TopAppBar` inside `Scaffold`. */
@Composable
actual fun GlassTopBar(
    title: String,
    status: String?,
    alertCount: Int,
    onAlertClick: () -> Unit,
    modifier: Modifier,
) = Unit
