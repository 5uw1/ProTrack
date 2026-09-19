package com.suw1labs.worktracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Not used here (the platform keeps its own navigation bar); draws the Compose bar if it ever is. */
@Composable
actual fun GlassTabBar(items: List<FloatingTabItem>, modifier: Modifier) = FloatingTabBar(items, modifier)
