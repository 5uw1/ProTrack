package com.suw1labs.worktracker.platform

import androidx.compose.runtime.Composable

/**
 * Requests notification permission where the platform needs it (Android 13+, iOS) and
 * invokes [onGranted] once notifications may be shown. Desktop grants immediately.
 */
@Composable
expect fun NotificationPermissionEffect(onGranted: () -> Unit)
