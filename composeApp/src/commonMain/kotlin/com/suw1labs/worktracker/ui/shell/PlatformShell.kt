package com.suw1labs.worktracker.ui.shell

/**
 * The shell a platform ships with: Liquid Glass on iOS 26, Material 3 Expressive on Android and
 * desktop. Changing what a platform looks like is this one function; [LocalAppShell] overrides it
 * per screen or per test.
 */
expect fun platformAppShell(): AppShell
