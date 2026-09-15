package com.suw1labs.worktracker.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Parses `#RRGGBB` / `#AARRGGBB` strings without relying on `android.graphics.Color`.
 * Returns null when the string is not a valid hex colour.
 */
fun parseHexColor(hex: String): Color? {
    val clean = hex.trim().removePrefix("#")
    val argb = when (clean.length) {
        6 -> clean.toLongOrNull(16)?.let { 0xFF000000L or it }
        8 -> clean.toLongOrNull(16)
        else -> null
    } ?: return null
    return Color(argb)
}

/** Parses a project colour, falling back to the theme's primary colour. */
@Composable
fun projectColor(hex: String): Color = parseHexColor(hex) ?: MaterialTheme.colorScheme.primary
