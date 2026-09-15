package com.suw1labs.worktracker.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/** Desktop needs no runtime permission for tray notifications. */
@Composable
actual fun NotificationPermissionEffect(onGranted: () -> Unit) {
    LaunchedEffect(Unit) { onGranted() }
}
