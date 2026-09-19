package com.suw1labs.worktracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Tab bar for platforms that draw it floating above the bottom edge. iOS renders it as a real
 * Liquid Glass bar (`UIGlassEffect`, iOS 26), every other platform falls back to [FloatingTabBar],
 * which paints the same layout with Compose.
 */
@Composable
expect fun GlassTabBar(items: List<FloatingTabItem>, modifier: Modifier)
